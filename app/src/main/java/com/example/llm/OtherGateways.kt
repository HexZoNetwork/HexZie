package com.example.llm

import com.example.data.model.GatewayConfig
import com.example.data.model.LlmModel
import com.example.data.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OllamaGateway(
    private val baseUrl: String
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun cleanBaseUrl(): String = baseUrl.trimEnd('/')

    override suspend fun listModels(): Result<List<LlmModel>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("${cleanBaseUrl()}/api/tags").get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("Ollama HTTP ${resp.code}"))
            val body = resp.body?.string() ?: ""
            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: JSONArray()
            val list = mutableListOf<LlmModel>()
            for (i in 0 until modelsArray.length()) {
                val m = modelsArray.getJSONObject(i)
                val name = m.getString("name")
                list.add(
                    LlmModel(
                        id = name,
                        name = name,
                        supportsTools = true,
                        supportsVision = name.contains("llava") || name.contains("vision"),
                        contextLength = 32000
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = listModels()
        if (result.isSuccess) Result.success(System.currentTimeMillis() - start) else Result.failure(result.exceptionOrNull() ?: Exception("Failed"))
    }

    override suspend fun validateApiKey(): GatewayValidationResult {
        return CustomGatewayScanner(client).scanAndValidate(baseUrl, "")
    }

    override suspend fun chatCompletion(
        messages: List<ChatMessage>,
        tools: List<ToolDefinitionDto>,
        modelName: String
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/api/chat"
            val payload = JSONObject()
            payload.put("model", modelName)
            payload.put("stream", false)

            val msgsArray = JSONArray()
            for (m in messages) {
                val mObj = JSONObject()
                mObj.put("role", m.role)
                mObj.put("content", m.content)
                msgsArray.put(mObj)
            }
            payload.put("messages", msgsArray)

            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("Ollama HTTP ${resp.code}"))

            val body = resp.body?.string() ?: "{}"
            val json = JSONObject(body)
            val msg = json.optJSONObject("message")
            val content = msg?.optString("content") ?: ""
            val toolCalls = ToolProtocolParser.extractToolCallsFromText(content)

            Result.success(
                ChatCompletionResponse(
                    content = content,
                    toolCalls = toolCalls,
                    promptTokens = json.optInt("prompt_eval_count", 0),
                    completionTokens = json.optInt("eval_count", 0)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamChat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinitionDto>,
        modelName: String
    ): Flow<StreamEvent> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val url = "${cleanBaseUrl()}/api/chat"
                val payload = JSONObject()
                payload.put("model", modelName)
                payload.put("stream", true)

                val msgsArray = JSONArray()
                for (m in messages) {
                    val mObj = JSONObject()
                    mObj.put("role", m.role)
                    mObj.put("content", m.content)
                    msgsArray.put(mObj)
                }
                payload.put("messages", msgsArray)

                val req = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    trySend(StreamEvent.Error("Ollama HTTP ${resp.code}: ${resp.message}"))
                    close()
                    return@withContext
                }

                val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                var line: String?
                val accumulated = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    val l = line!!.trim()
                    if (l.isEmpty()) continue
                    try {
                        val chunk = JSONObject(l)
                        val msg = chunk.optJSONObject("message")
                        val content = msg?.optString("content") ?: ""
                        if (content.isNotEmpty()) {
                            accumulated.append(content)
                            trySend(StreamEvent.Token(content))
                        }
                    } catch (_: Exception) {}
                }

                // Check for fallback tool calls
                val fallbackCalls = ToolProtocolParser.extractToolCallsFromText(accumulated.toString())
                if (fallbackCalls.isNotEmpty()) {
                    trySend(StreamEvent.ToolCallsDetected(fallbackCalls))
                }

                trySend(StreamEvent.Completed(accumulated.toString(), 0, 0))
                close()
            } catch (e: Exception) {
                trySend(StreamEvent.Error(e.localizedMessage ?: "Ollama error"))
                close()
            }
        }
        awaitClose {}
    }
}

object GatewayFactory {
    fun createProvider(config: GatewayConfig): LlmProvider {
        return when (config.providerType) {
            ProviderType.OLLAMA -> OllamaGateway(config.baseUrl)
            else -> OpenAiCompatibleGateway(config.baseUrl, config.apiKey, config.customHeadersJson)
        }
    }
}
