package com.example.llm

import com.example.data.model.LlmModel
import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val role: String, // "system", "user", "assistant", "tool"
    val content: String,
    val toolCallId: String? = null,
    val toolName: String? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class ToolDefinitionDto(
    val name: String,
    val description: String,
    val parametersJsonSchema: String
)

data class ChatCompletionResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val finishReason: String = "stop",
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
)

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class ToolCallsDetected(val toolCalls: List<ToolCall>) : StreamEvent()
    data class Completed(val fullText: String, val promptTokens: Int = 0, val completionTokens: Int = 0) : StreamEvent()
    data class Error(val error: String) : StreamEvent()
}

/**
 * Core LLM Provider interface in HexZie.
 * Defines contracts for synchronous chat completions, SSE token streaming,
 * dynamic model discovery, API key validation, and tool support.
 */
interface LlmProvider {

    /**
     * Non-streaming chat completion with tool calling schema support.
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        tools: List<ToolDefinitionDto> = emptyList(),
        modelName: String
    ): Result<ChatCompletionResponse>

    /**
     * Real-time streaming chat completion via Server-Sent Events (SSE).
     */
    fun streamChat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinitionDto> = emptyList(),
        modelName: String
    ): Flow<StreamEvent>

    /**
     * Queries the endpoint and returns all available models.
     */
    suspend fun listModels(): Result<List<LlmModel>>

    /**
     * Validates API keys against the remote gateway.
     */
    suspend fun validateApiKey(): GatewayValidationResult

    /**
     * Measures connection latency to the provider endpoint in milliseconds.
     */
    suspend fun testConnection(): Result<Long>
}
