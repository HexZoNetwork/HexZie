package com.example.llm

import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

object ToolProtocolParser {

    /**
     * Attempts to parse JSON-in-text tool protocol if the model outputs raw text tool invocations
     * e.g. {"tool": "screen_read", "arguments": { ... }} or {"tool_calls": [...]}
     */
    fun extractToolCallsFromText(text: String): List<ToolCall> {
        val list = mutableListOf<ToolCall>()
        val trimmed = text.trim()

        // 1. Check for Markdown code blocks with JSON
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        for (match in codeBlockRegex.findAll(trimmed)) {
            val content = match.groupValues[1].trim()
            parseJsonPayload(content, list)
        }

        // 2. Check if the entire response is a JSON object
        if (list.isEmpty() && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
            parseJsonPayload(trimmed, list)
        }

        // 3. Check for XML style <tool_call> ... </tool_call>
        if (list.isEmpty()) {
            val xmlRegex = Regex("<tool_call>\\s*([\\s\\S]*?)\\s*</tool_call>")
            for (match in xmlRegex.findAll(trimmed)) {
                val content = match.groupValues[1].trim()
                parseJsonPayload(content, list)
            }
        }

        return list
    }

    private fun parseJsonPayload(jsonStr: String, outList: MutableList<ToolCall>) {
        try {
            if (jsonStr.startsWith("[")) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    extractSingleToolCall(obj)?.let { outList.add(it) }
                }
            } else if (jsonStr.startsWith("{")) {
                val obj = JSONObject(jsonStr)
                if (obj.has("tool_calls")) {
                    val array = obj.getJSONArray("tool_calls")
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        extractSingleToolCall(item)?.let { outList.add(it) }
                    }
                } else {
                    extractSingleToolCall(obj)?.let { outList.add(it) }
                }
            }
        } catch (_: Exception) {}
    }

    private fun extractSingleToolCall(obj: JSONObject): ToolCall? {
        val name = when {
            obj.has("tool") -> obj.optString("tool")
            obj.has("name") -> obj.optString("name")
            obj.has("function") -> obj.optJSONObject("function")?.optString("name") ?: ""
            else -> ""
        }
        if (name.isBlank()) return null

        val arguments = when {
            obj.has("arguments") -> {
                val argObj = obj.opt("arguments")
                if (argObj is JSONObject || argObj is JSONArray) argObj.toString() else argObj?.toString() ?: "{}"
            }
            obj.has("parameters") -> obj.opt("parameters")?.toString() ?: "{}"
            obj.has("function") -> obj.optJSONObject("function")?.opt("arguments")?.toString() ?: "{}"
            else -> "{}"
        }

        val id = obj.optString("id", "call_" + UUID.randomUUID().toString().take(8))
        return ToolCall(id = id, name = name, argumentsJson = arguments)
    }
}
