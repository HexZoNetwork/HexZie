package com.example.tools

import android.content.Context
import com.example.data.model.RiskLevel
import kotlinx.coroutines.delay

class AskUserTool : Tool {
    override val name = "ask_user"
    override val description = "Asks the user a clarifying question either spoken out loud or shown on-screen."
    override val parametersJsonSchema = """{"type": "object", "properties": {"question": {"type": "string"}}, "required": ["question"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "agent_utilities"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val q = arguments["question"]?.toString() ?: return ToolResult.failure("Missing question")
        return ToolResult.success("Question presented to user: '$q'")
    }
}

class TodoWriteTool : Tool {
    override val name = "todo_write"
    override val description = "Saves an actionable task or reminder to the local scratchpad."
    override val parametersJsonSchema = """{"type": "object", "properties": {"task": {"type": "string"}}, "required": ["task"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "agent_utilities"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val task = arguments["task"]?.toString() ?: return ToolResult.failure("Missing task")
        return ToolResult.success("Saved todo item: \"$task\"")
    }
}

class WaitTool : Tool {
    override val name = "wait"
    override val description = "Pauses execution for a specified duration in seconds (1 to 10 seconds)."
    override val parametersJsonSchema = """{"type": "object", "properties": {"seconds": {"type": "integer", "minimum": 1, "maximum": 10}}, "required": ["seconds"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "agent_utilities"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val seconds = (arguments["seconds"] as? Number)?.toLong() ?: 1L
        val clamped = seconds.coerceIn(1, 10)
        delay(clamped * 1000)
        return ToolResult.success("Waited $clamped seconds.")
    }
}

class FinishTool : Tool {
    override val name = "finish"
    override val description = "Signals that the current multi-step plan has concluded and summarizes results."
    override val parametersJsonSchema = """{"type": "object", "properties": {"summary": {"type": "string"}}, "required": ["summary"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "agent_utilities"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val summary = arguments["summary"]?.toString() ?: "Task completed."
        return ToolResult.success("Done: $summary")
    }
}
