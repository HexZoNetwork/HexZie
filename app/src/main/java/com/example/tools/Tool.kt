package com.example.tools

import android.content.Context
import com.example.data.model.RiskLevel

data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null
) {
    companion object {
        const val MAX_OUTPUT_CHARS = 3000

        fun success(text: String): ToolResult {
            val truncated = if (text.length > MAX_OUTPUT_CHARS) {
                text.take(MAX_OUTPUT_CHARS) + "\n... [Truncated: output exceeded 3,000 characters]"
            } else {
                text
            }
            return ToolResult(success = true, output = truncated)
        }

        fun failure(error: String): ToolResult {
            return ToolResult(success = false, output = "Error: $error", error = error)
        }
    }
}

interface Tool {
    val name: String
    val description: String
    val parametersJsonSchema: String
    val riskLevel: RiskLevel
    val requiredPermissionKey: String // Maps to group in Permission Center

    suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult
}
