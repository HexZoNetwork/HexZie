package com.example.agent

import android.content.Context
import com.example.data.local.DataStoreManager
import com.example.data.local.HexzieDatabase
import com.example.data.local.MessageEntity
import com.example.data.model.AgentState
import com.example.data.model.HighRiskActionRequest
import com.example.data.model.PermissionLevel
import com.example.data.model.RiskLevel
import com.example.llm.ChatMessage
import com.example.llm.LlmProvider
import com.example.llm.StreamEvent
import com.example.llm.ToolCall
import com.example.tools.ToolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID

sealed class AgentLoopEvent {
    data class Token(val text: String) : AgentLoopEvent()
    data class ToolStarting(val toolName: String, val argsJson: String, val riskLevel: RiskLevel) : AgentLoopEvent()
    data class ToolFinished(val toolName: String, val result: String, val success: Boolean) : AgentLoopEvent()
    data class HighRiskAwaitingConfirmation(val request: HighRiskActionRequest) : AgentLoopEvent()
    data class Completed(val answer: String) : AgentLoopEvent()
    data class Error(val message: String) : AgentLoopEvent()
    object Cancelled : AgentLoopEvent()
}

class HexzieAgentLoop(
    private val context: Context,
    private val database: HexzieDatabase,
    private val dataStoreManager: DataStoreManager,
    private val toolRegistry: ToolRegistry
) {

    private val _agentState = MutableStateFlow(AgentState.IDLE)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private var activeJob: Job? = null

    fun emergencyStop() {
        activeJob?.cancel()
        activeJob = null
        _agentState.value = AgentState.EMERGENCY_STOP
    }

    suspend fun runLoop(
        sessionId: String,
        userPrompt: String,
        provider: LlmProvider,
        modelName: String,
        onEvent: suspend (AgentLoopEvent) -> Unit
    ) {
        _agentState.value = AgentState.THINKING
        val maxSteps = dataStoreManager.maxStepsFlow.first()
        val systemPrompt = dataStoreManager.systemPromptFlow.first()
        val isSafeMode = dataStoreManager.safeModeFlow.first()
        val denylist = dataStoreManager.denylistFlow.first()

        // 1. Save user message to Room
        val userMsgId = UUID.randomUUID().toString()
        val userMsg = MessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "user",
            content = userPrompt
        )
        database.messageDao().insertMessage(userMsg)

        // 2. Load conversation history
        val existingHistory = database.messageDao().getMessagesList(sessionId)
        val conversation = mutableListOf<ChatMessage>()
        conversation.add(ChatMessage(role = "system", content = systemPrompt))

        // Sliding window: keep latest 20 messages
        val trimmedHistory = if (existingHistory.size > 20) existingHistory.takeLast(20) else existingHistory
        for (m in trimmedHistory) {
            conversation.add(ChatMessage(role = m.role, content = m.content, toolCallId = null, toolName = m.toolName))
        }

        var currentStep = 0
        var sawUntrustedInjection = false

        try {
            while (currentStep < maxSteps) {
                currentStep++
                _agentState.value = AgentState.THINKING

                val toolDefs = toolRegistry.getToolDefinitions()
                var accumulatedTokens = StringBuilder()
                var detectedToolCalls: List<ToolCall>? = null
                var streamError: String? = null

                provider.streamChat(conversation, toolDefs, modelName).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            accumulatedTokens.append(event.text)
                            onEvent(AgentLoopEvent.Token(event.text))
                        }
                        is StreamEvent.ToolCallsDetected -> {
                            detectedToolCalls = event.toolCalls
                        }
                        is StreamEvent.Completed -> {
                            // finished streaming chunk
                        }
                        is StreamEvent.Error -> {
                            streamError = event.error
                        }
                    }
                }

                if (streamError != null) {
                    _agentState.value = AgentState.ERROR
                    onEvent(AgentLoopEvent.Error(streamError!!))
                    return
                }

                // If no tool calls were generated, this is the final answer
                if (detectedToolCalls.isNullOrEmpty()) {
                    val finalAnswer = accumulatedTokens.toString()
                    val assistantMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalAnswer
                    )
                    database.messageDao().insertMessage(assistantMsg)
                    _agentState.value = AgentState.SPEAKING
                    onEvent(AgentLoopEvent.Completed(finalAnswer))
                    return
                }

                // Tools were requested
                _agentState.value = AgentState.ACTING
                val assistantToolCallMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "assistant",
                    content = accumulatedTokens.toString(),
                    toolCallsJson = detectedToolCalls.toString()
                )
                database.messageDao().insertMessage(assistantToolCallMsg)
                conversation.add(ChatMessage(role = "assistant", content = accumulatedTokens.toString()))

                // Execute each tool call
                for (toolCall in detectedToolCalls!!) {
                    val tool = toolRegistry.getTool(toolCall.name)
                    if (tool == null) {
                        val errMsg = "Tool '${toolCall.name}' not found."
                        conversation.add(ChatMessage(role = "tool", content = errMsg, toolCallId = toolCall.id, toolName = toolCall.name))
                        continue
                    }

                    // Parse arguments map
                    val argsMap = try {
                        val obj = JSONObject(toolCall.argumentsJson)
                        val map = mutableMapOf<String, Any?>()
                        for (key in obj.keys()) {
                            map[key] = obj.get(key)
                        }
                        map
                    } catch (_: Exception) {
                        emptyMap<String, Any?>()
                    }

                    onEvent(AgentLoopEvent.ToolStarting(tool.name, toolCall.argumentsJson, tool.riskLevel))

                    // Permission gating
                    val permissionLevel = dataStoreManager.getPermissionFlow(tool.requiredPermissionKey).first()
                    var approved = false
                    var approvalSource = "AUTO"

                    if (permissionLevel == PermissionLevel.OFF) {
                        val res = "Permission Denied: '${tool.name}' is turned OFF by user in Permission Center."
                        onEvent(AgentLoopEvent.ToolFinished(tool.name, res, false))
                        conversation.add(ChatMessage(role = "tool", content = res, toolCallId = toolCall.id, toolName = tool.name))
                        continue
                    }

                    val mustAsk = isSafeMode ||
                            permissionLevel == PermissionLevel.ASK ||
                            tool.riskLevel == RiskLevel.HIGH ||
                            sawUntrustedInjection

                    if (mustAsk) {
                        _agentState.value = AgentState.AWAITING_CONFIRMATION
                        val confirmationDeferred = CompletableDeferred<Boolean>()
                        val request = HighRiskActionRequest(
                            callId = toolCall.id,
                            toolName = tool.name,
                            description = "HexZie wants to execute '${tool.name}' (${tool.riskLevel.name} Risk)",
                            argumentsSummary = toolCall.argumentsJson,
                            onConfirm = { confirmationDeferred.complete(true) },
                            onDeny = { confirmationDeferred.complete(false) }
                        )
                        onEvent(AgentLoopEvent.HighRiskAwaitingConfirmation(request))
                        val userApproved = confirmationDeferred.await()
                        _agentState.value = AgentState.ACTING
                        if (!userApproved) {
                            val res = "Action cancelled: User denied permission to run '${tool.name}'."
                            onEvent(AgentLoopEvent.ToolFinished(tool.name, res, false))
                            conversation.add(ChatMessage(role = "tool", content = res, toolCallId = toolCall.id, toolName = tool.name))
                            continue
                        }
                        approved = true
                        approvalSource = "USER_CONFIRMED"
                    } else {
                        approved = true
                        approvalSource = "ALWAYS_ALLOW"
                    }

                    // Execute tool
                    val executionResult = toolRegistry.executeTool(
                        toolName = tool.name,
                        arguments = argsMap,
                        approvalSource = approvalSource,
                        currentPackage = null,
                        denylist = denylist
                    )

                    // Check if output contains prompt injection pattern
                    if (SafetyPolicy.containsPromptInjection(executionResult.output)) {
                        sawUntrustedInjection = true
                    }

                    onEvent(AgentLoopEvent.ToolFinished(tool.name, executionResult.output, executionResult.success))

                    val toolMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "tool",
                        content = executionResult.output,
                        toolName = tool.name,
                        riskLevel = tool.riskLevel.name,
                        toolResultJson = executionResult.output
                    )
                    database.messageDao().insertMessage(toolMsg)

                    conversation.add(
                        ChatMessage(
                            role = "tool",
                            content = executionResult.output,
                            toolCallId = toolCall.id,
                            toolName = tool.name
                        )
                    )
                }
            }

            // Exceeded max steps
            _agentState.value = AgentState.IDLE
            onEvent(AgentLoopEvent.Completed("Reached maximum plan execution steps ($maxSteps)."))
        } catch (e: CancellationException) {
            _agentState.value = AgentState.EMERGENCY_STOP
            onEvent(AgentLoopEvent.Cancelled)
        } catch (e: Exception) {
            _agentState.value = AgentState.ERROR
            onEvent(AgentLoopEvent.Error(e.localizedMessage ?: "Agent error"))
        }
    }
}
