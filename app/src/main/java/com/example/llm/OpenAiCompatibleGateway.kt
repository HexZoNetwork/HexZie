package com.example.llm

import com.example.data.model.LlmModel
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

class OpenAiCompatibleGateway(
    private val baseUrl: String,
    private val apiKey: String,
    private val customHeadersJson: String = "{}"
) : LlmProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun sanitizeUrl(path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return if (cleanBase.endsWith("/v1")) {
            "$cleanBase/$path"
        } else {
            "$cleanBase/v1/$path"
        }
    }

    private fun addHeaders(builder: Request.Builder) {
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        try {
            val customHeaders = JSONObject(customHeadersJson)
            for (key in customHeaders.keys()) {
                builder.header(key, customHeaders.optString(key))
            }
        } catch (_: Exception) {}
    }

    override suspend fun listModels(): Result<List<LlmModel>> = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl("models")
            val reqBuilder = Request.Builder().url(url).get()
            addHeaders(reqBuilder)
            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<LlmModel>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.getString("id")
                val isVision = id.contains("vision", ignoreCase = true) || id.contains("4o", ignoreCase = true) || id.contains("claude-3", ignoreCase = true)
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
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = listModels()
        val elapsed = System.currentTimeMillis() - start
        if (result.isSuccess) Result.success(elapsed) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }

    override suspend fun validateApiKey(): GatewayValidationResult {
        val scanner = CustomGatewayScanner(client)
        val headers = mutableMapOf<String, String>()
        try {
            val json = JSONObject(customHeadersJson)
            for (k in json.keys()) headers[k] = json.optString(k)
        } catch (_: Exception) {}
        return scanner.scanAndValidate(baseUrl, apiKey, headers)
    }

    override suspend fun chatCompletion(
        messages: List<ChatMessage>,
        tools: List<ToolDefinitionDto>,
        modelName: String
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val url = sanitizeUrl("chat/completions")
            val payload = JSONObject()
            payload.put("model", modelName)
            payload.put("stream", false)

            val msgsArray = JSONArray()
            for (m in messages) {
                val mObj = JSONObject()
                mObj.put("role", m.role)
                mObj.put("content", m.content)
                if (m.toolCallId != null) mObj.put("tool_call_id", m.toolCallId)
                if (m.toolName != null) mObj.put("name", m.toolName)
                msgsArray.put(mObj)
            }
            payload.put("messages", msgsArray)

            if (tools.isNotEmpty()) {
                val toolsArray = JSONArray()
                for (t in tools) {
                    val tObj = JSONObject()
                    tObj.put("type", "function")
                    val fnObj = JSONObject()
                    fnObj.put("name", t.name)
                    fnObj.put("description", t.description)
                    try {
                        fnObj.put("parameters", JSONObject(t.parametersJsonSchema))
                    } catch (_: Exception) {
                        fnObj.put("parameters", JSONObject())
                    }
                    tObj.put("function", fnObj)
                    toolsArray.put(tObj)
                }
                payload.put("tools", toolsArray)
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
            addHeaders(reqBuilder)

            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: response.message
                return@withContext Result.failure(Exception("HTTP ${response.code}: $err"))
            }

            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("No choices returned in response"))
            }

            val choice = choices.getJSONObject(0)
            val message = choice.optJSONObject("message")
            val content = message?.optString("content") ?: ""
            val finishReason = choice.optString("finish_reason", "stop")

            val toolCallsList = mutableListOf<ToolCall>()
            val tcArray = message?.optJSONArray("tool_calls")
            if (tcArray != null) {
                for (i in 0 until tcArray.length()) {
                    val tc = tcArray.getJSONObject(i)
                    val id = tc.optString("id", "call_$i")
                    val fn = tc.optJSONObject("function")
                    val name = fn?.optString("name", "") ?: ""
                    val args = fn?.optString("arguments", "{}") ?: "{}"
                    if (name.isNotEmpty()) {
                        toolCallsList.add(ToolCall(id = id, name = name, argumentsJson = args))
                    }
                }
            }

            if (toolCallsList.isEmpty() && content.isNotBlank()) {
                val fallback = ToolProtocolParser.extractToolCallsFromText(content)
                toolCallsList.addAll(fallback)
            }

            val usage = json.optJSONObject("usage")
            val promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0
            val completionTokens = usage?.optInt("completion_tokens", 0) ?: 0

            Result.success(
                ChatCompletionResponse(
                    content = content,
                    toolCalls = toolCallsList,
                    finishReason = finishReason,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens
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
                val url = sanitizeUrl("chat/completions")
                val payload = JSONObject()
                payload.put("model", modelName)
                payload.put("stream", true)

                val msgsArray = JSONArray()
                for (m in messages) {
                    val mObj = JSONObject()
                    mObj.put("role", m.role)
                    mObj.put("content", m.content)
                    if (m.toolCallId != null) {
                        mObj.put("tool_call_id", m.toolCallId)
                    }
                    if (m.toolName != null) {
                        mObj.put("name", m.toolName)
                    }
                    msgsArray.put(mObj)
                }
                payload.put("messages", msgsArray)

                if (tools.isNotEmpty()) {
                    val toolsArray = JSONArray()
                    for (t in tools) {
                        val tObj = JSONObject()
                        tObj.put("type", "function")
                        val fnObj = JSONObject()
                        fnObj.put("name", t.name)
                        fnObj.put("description", t.description)
                        try {
                            fnObj.put("parameters", JSONObject(t.parametersJsonSchema))
                        } catch (_: Exception) {
                            fnObj.put("parameters", JSONObject())
                        }
                        tObj.put("function", fnObj)
                        toolsArray.put(tObj)
                    }
                    payload.put("tools", toolsArray)
                }

                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                addHeaders(reqBuilder)

                val response: Response = client.newCall(reqBuilder.build()).execute()
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: response.message
                    trySend(StreamEvent.Error("HTTP ${response.code}: $err"))
                    close()
                    return@withContext
                }

                val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
                var line: String?
                val accumulatedContent = StringBuilder()
                val toolCallsMap = mutableMapOf<Int, Triple<String, String, StringBuilder>>() // index -> (id, name, args)

                while (reader.readLine().also { line = it } != null) {
                    val l = line!!.trim()
                    if (l.isEmpty() || l.startsWith(":")) continue
                    if (l == "data: [DONE]") break
                    if (l.startsWith("data: ")) {
                        val jsonPart = l.removePrefix("data: ")
                        try {
                            val chunk = JSONObject(jsonPart)
                            val choices = chunk.optJSONArray("choices") ?: continue
                            if (choices.length() == 0) continue
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta") ?: continue

                            if (delta.has("content") && !delta.isNull("content")) {
                                val token = delta.optString("content")
                                if (token.isNotEmpty()) {
                                    accumulatedContent.append(token)
                                    trySend(StreamEvent.Token(token))
                                }
                            }

                            if (delta.has("tool_calls")) {
                                val tcArray = delta.getJSONArray("tool_calls")
                                for (i in 0 until tcArray.length()) {
                                    val tc = tcArray.getJSONObject(i)
                                    val index = tc.optInt("index", 0)
                                    val id = tc.optString("id", "")
                                    val fn = tc.optJSONObject("function")
                                    val name = fn?.optString("name", "") ?: ""
                                    val argsDelta = fn?.optString("arguments", "") ?: ""

                                    val existing = toolCallsMap[index]
                                    if (existing == null) {
                                        toolCallsMap[index] = Triple(id, name, StringBuilder(argsDelta))
                                    } else {
                                        val newId = if (id.isNotEmpty()) id else existing.first
                                        val newName = if (name.isNotEmpty()) name else existing.second
                                        existing.third.append(argsDelta)
                                        toolCallsMap[index] = Triple(newId, newName, existing.third)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // If native tool calls were accumulated
                if (toolCallsMap.isNotEmpty()) {
                    val nativeCalls = toolCallsMap.values.map {
                        ToolCall(id = it.first, name = it.second, argumentsJson = it.third.toString())
                    }
                    trySend(StreamEvent.ToolCallsDetected(nativeCalls))
                } else {
                    // Check fallback JSON-in-text tool protocol
                    val fallbackCalls = ToolProtocolParser.extractToolCallsFromText(accumulatedContent.toString())
                    if (fallbackCalls.isNotEmpty()) {
                        trySend(StreamEvent.ToolCallsDetected(fallbackCalls))
                    }
                }

                trySend(StreamEvent.Completed(accumulatedContent.toString(), 0, 0))
                close()
            } catch (e: Exception) {
                trySend(StreamEvent.Error(e.localizedMessage ?: "Network error"))
                close()
            }
        }
        awaitClose {}
    }
}

typealias OpenAiCompatibleProvider = OpenAiCompatibleGateway

