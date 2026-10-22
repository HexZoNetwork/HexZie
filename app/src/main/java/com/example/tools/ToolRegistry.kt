package com.example.tools

import android.content.Context
import com.example.agent.SafetyPolicy
import com.example.data.local.AuditLogEntity
import com.example.data.local.HexzieDatabase
import com.example.data.model.PermissionLevel
import com.example.data.model.RiskLevel
import com.example.llm.ToolDefinitionDto
import kotlinx.coroutines.flow.first

class ToolRegistry(
    private val context: Context,
    private val database: HexzieDatabase
) {

    private val tools = mutableMapOf<String, Tool>()

    init {
        register(ScreenReadTool())
        register(ScreenTapTool())
        register(ScreenSwipeTool())
        register(ScreenScrollTool())
        register(ScreenTypeTextTool())
        register(GlobalActionTool())
        register(OpenAppTool())
        register(ListAppsTool())
        register(SetVolumeTool())
        register(ToggleFlashlightTool())
        register(SetAlarmTool())
        register(ClipboardReadTool())
        register(ClipboardWriteTool())
        register(CallMakeTool())
        register(SmsSendTool())
        register(ContactsSearchTool())
        register(CalendarReadTool())
        register(ScreenCaptureTool())
        register(CameraCaptureTool())
        register(WebFetchTool())
        register(LocationGetTool())
        register(FileReadTool())
        register(FileWriteTool())
        register(AskUserTool())
        register(TodoWriteTool())
        register(WaitTool())
        register(FinishTool())
    }

    private fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): Tool? = tools[name]

    fun getAllTools(): List<Tool> = tools.values.toList()

    fun getToolDefinitions(): List<ToolDefinitionDto> {
        return tools.values.map {
            ToolDefinitionDto(
                name = it.name,
                description = it.description,
                parametersJsonSchema = it.parametersJsonSchema
            )
        }
    }

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any?>,
        approvalSource: String = "AUTO",
        currentPackage: String? = null,
        denylist: List<String> = emptyList()
    ): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult.failure("Unknown tool: $toolName")

        val start = System.currentTimeMillis()

        // 1. Hard safety checks
        val (isBlocked, reason) = SafetyPolicy.isHardBlocked(toolName, arguments, currentPackage, denylist)
        if (isBlocked) {
            val res = ToolResult.failure(reason ?: "Safety violation")
            logAudit(tool, arguments, "BLOCKED_BY_SAFETY", approvalSource, System.currentTimeMillis() - start)
            return res
        }

        // 2. Execute
        val result = try {
            tool.execute(context, arguments)
        } catch (e: Exception) {
            ToolResult.failure(e.localizedMessage ?: "Execution failed")
        }

        val duration = System.currentTimeMillis() - start
        val status = if (result.success) "SUCCESS" else "FAILED"
        logAudit(tool, arguments, status, approvalSource, duration)

        return result
    }

    private suspend fun logAudit(
        tool: Tool,
        arguments: Map<String, Any?>,
        status: String,
        approvalSource: String,
        durationMs: Long
    ) {
        try {
            val sanitized = SafetyPolicy.sanitizeArgumentsForAudit(tool.name, arguments)
            val log = AuditLogEntity(
                toolName = tool.name,
                riskLevel = tool.riskLevel.name,
                argumentsSanitized = sanitized,
                resultStatus = status,
                approvalSource = approvalSource,
                durationMs = durationMs
            )
            database.auditLogDao().insertLog(log)
        } catch (_: Exception) {}
    }
}
