package com.example.llm

import com.example.data.model.LlmModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GatewayValidationResult(
    val isValid: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val models: List<LlmModel>,
    val message: String,
    val endpointUsed: String = ""
)

class CustomGatewayScanner(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private fun normalizeUrl(baseUrl: String, path: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/v1")) {
            "$clean/$path"
        } else {
            "$clean/v1/$path"
        }
    }

    /**
     * Scans the provided gateway URL, validates API keys against the endpoint,
     * measures response latency, and returns parsed models with their capabilities.
     */
    suspend fun scanAndValidate(
        baseUrl: String,
        apiKey: String,
        customHeaders: Map<String, String> = emptyMap()
    ): GatewayValidationResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext GatewayValidationResult(
                isValid = false,
                statusCode = 0,
                latencyMs = 0,
                models = emptyList(),
                message = "Base URL cannot be empty."
            )
        }

        val start = System.currentTimeMillis()
        val candidateUrls = listOf(
            normalizeUrl(baseUrl, "models"),
            baseUrl.trimEnd('/') + "/models",
            baseUrl.trimEnd('/') + "/api/tags" // Ollama
        )

        var lastStatusCode = 0
        var lastErrorMessage = ""

        for (url in candidateUrls) {
            try {
                val reqBuilder = Request.Builder().url(url).get()
                if (apiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
                }
                customHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }

                val response = client.newCall(reqBuilder.build()).execute()
                val latency = System.currentTimeMillis() - start
                lastStatusCode = response.code

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val models = parseModelsJson(body, url.contains("/api/tags"))
                    return@withContext GatewayValidationResult(
                        isValid = true,
                        statusCode = response.code,
                        latencyMs = latency,
                        models = models,
                        message = "Valid connection! Discovered ${models.size} models (${latency}ms).",
                        endpointUsed = url
                    )
                } else if (response.code == 401) {
                    return@withContext GatewayValidationResult(
                        isValid = false,
                        statusCode = 401,
                        latencyMs = latency,
                        models = emptyList(),
                        message = "Unauthorized (401): Invalid or missing API Key.",
                        endpointUsed = url
                    )
                } else if (response.code == 403) {
                    return@withContext GatewayValidationResult(
                        isValid = false,
                        statusCode = 403,
                        latencyMs = latency,
                        models = emptyList(),
                        message = "Forbidden (403): The API key lacks permission or quota.",
                        endpointUsed = url
                    )
                } else {
                    lastErrorMessage = "HTTP ${response.code}: ${response.message}"
                }
            } catch (e: Exception) {
                lastErrorMessage = e.localizedMessage ?: "Connection error"
            }
        }

        GatewayValidationResult(
            isValid = false,
            statusCode = lastStatusCode,
            latencyMs = System.currentTimeMillis() - start,
            models = emptyList(),
            message = "Validation failed: $lastErrorMessage"
        )
    }

    private fun parseModelsJson(body: String, isOllama: Boolean): List<LlmModel> {
        val list = mutableListOf<LlmModel>()
        try {
            val json = JSONObject(body)
            if (isOllama || json.has("models")) {
                val arr = json.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val name = m.optString("name", "model-$i")
                    list.add(
                        LlmModel(
                            id = name,
                            name = name,
                            supportsTools = true,
                            supportsVision = name.contains("llava", ignoreCase = true) || name.contains("vision", ignoreCase = true),
                            contextLength = 32768
                        )
                    )
                }
            } else {
                val data = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (id.isNotBlank()) {
                        val isVision = id.contains("vision", ignoreCase = true) ||
                                id.contains("4o", ignoreCase = true) ||
                                id.contains("claude-3", ignoreCase = true) ||
                                id.contains("gemini", ignoreCase = true)
                        list.add(
                            LlmModel(
                                id = id,
                                name = id,
                                supportsTools = true,
                                supportsVision = isVision,
                                contextLength = 128000
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
