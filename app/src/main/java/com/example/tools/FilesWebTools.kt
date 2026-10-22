package com.example.tools

import android.content.Context
import android.location.LocationManager
import com.example.agent.SafetyPolicy
import com.example.data.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class WebFetchTool : Tool {
    override val name = "web_fetch"
    override val description = "Fetches text content from a web URL using HTTP GET."
    override val parametersJsonSchema = """{"type": "object", "properties": {"url": {"type": "string"}}, "required": ["url"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "web_access"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val url = arguments["url"]?.toString() ?: return@withContext ToolResult.failure("Missing url")
        try {
            val req = Request.Builder().url(url).header("User-Agent", "HexZie-Agent/1.0").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext ToolResult.failure("HTTP ${resp.code}: ${resp.message}")
            val rawHtml = resp.body?.string() ?: ""
            // Simple tag stripper
            val textOnly = rawHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
            val wrapped = SafetyPolicy.wrapUntrustedData("web: $url", textOnly)
            ToolResult.success(wrapped)
        } catch (e: Exception) {
            ToolResult.failure("Web fetch failed: ${e.message}")
        }
    }
}

class LocationGetTool : Tool {
    override val name = "location_get"
    override val description = "Retrieves the current coarse or fine geographic latitude and longitude of the device."
    override val parametersJsonSchema = """{"type": "object", "properties": {}}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "location"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                ToolResult.success("Location: lat=${loc.latitude}, lon=${loc.longitude}, accuracy=${loc.accuracy}m")
            } else {
                ToolResult.success("Location coordinates currently unavailable. Please ensure GPS is enabled.")
            }
        } catch (e: SecurityException) {
            ToolResult.failure("Location permission not granted: ${e.message}")
        }
    }
}

class FileReadTool : Tool {
    override val name = "file_read"
    override val description = "Reads the text contents of a file inside the device storage."
    override val parametersJsonSchema = """{"type": "object", "properties": {"filename": {"type": "string"}}, "required": ["filename"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "files"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val filename = arguments["filename"]?.toString() ?: return@withContext ToolResult.failure("Missing filename")
        try {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext ToolResult.failure("File does not exist: $filename")
            val content = file.readText()
            ToolResult.success(SafetyPolicy.wrapUntrustedData("file: $filename", content))
        } catch (e: Exception) {
            ToolResult.failure("Failed to read file: ${e.message}")
        }
    }
}

class FileWriteTool : Tool {
    override val name = "file_write"
    override val description = "Writes text content to a file inside the app storage."
    override val parametersJsonSchema = """{"type": "object", "properties": {"filename": {"type": "string"}, "content": {"type": "string"}}, "required": ["filename", "content"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "files"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val filename = arguments["filename"]?.toString() ?: return@withContext ToolResult.failure("Missing filename")
        val content = arguments["content"]?.toString() ?: return@withContext ToolResult.failure("Missing content")
        try {
            val file = File(context.filesDir, filename)
            file.writeText(content)
            ToolResult.success("Saved ${content.length} characters to $filename")
        } catch (e: Exception) {
            ToolResult.failure("Failed to write file: ${e.message}")
        }
    }
}
