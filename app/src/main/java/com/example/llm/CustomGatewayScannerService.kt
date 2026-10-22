package com.example.llm

import com.example.data.local.DataStoreManager
import com.example.data.local.SecurityVault
import com.example.data.model.GatewayConfig
import com.example.data.model.LlmModel
import com.example.data.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GatewayScanAndRegisterResult(
    val success: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val registeredGateway: GatewayConfig?,
    val models: List<LlmModel>,
    val message: String
)

/**
 * Service that accepts a base URL and API key, validates connectivity and authentication,
 * parses available models and their capabilities, and registers the gateway for the user.
 */
class CustomGatewayScannerService(
    private val scanner: CustomGatewayScanner = CustomGatewayScanner(),
    private val securityVault: SecurityVault? = null,
    private val dataStoreManager: DataStoreManager? = null
) {

    suspend fun scanAndRegister(
        baseUrl: String,
        apiKey: String,
        name: String = "Custom Gateway",
        providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
        customHeaders: Map<String, String> = emptyMap()
    ): GatewayScanAndRegisterResult = withContext(Dispatchers.IO) {
        val validation = scanner.scanAndValidate(baseUrl, apiKey, customHeaders)
        if (!validation.isValid) {
            return@withContext GatewayScanAndRegisterResult(
                success = false,
                statusCode = validation.statusCode,
                latencyMs = validation.latencyMs,
                registeredGateway = null,
                models = emptyList(),
                message = validation.message
            )
        }

        val models = validation.models
        val defaultModel = selectDefaultModel(models, providerType)
        val visionModel = models.firstOrNull { it.supportsVision }?.id ?: defaultModel
        val quickModel = models.firstOrNull { it.id.contains("mini", ignoreCase = true) || it.id.contains("flash", ignoreCase = true) }?.id ?: defaultModel

        val gatewayId = UUID.randomUUID().toString()
        val config = GatewayConfig(
            id = gatewayId,
            name = name.ifBlank { "Custom Gateway" },
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            providerType = providerType,
            defaultModel = defaultModel,
            visionModel = visionModel,
            quickModel = quickModel,
            isEnabled = true,
            customHeadersJson = JSONObject(customHeaders).toString()
        )

        // Persist key to KeyStore if vault is available
        securityVault?.storeApiKey(gatewayId, apiKey.trim())

        // Register in DataStore if available
        if (dataStoreManager != null) {
            try {
                dataStoreManager.setActiveGatewayId(gatewayId)
            } catch (_: Exception) {}
        }

        GatewayScanAndRegisterResult(
            success = true,
            statusCode = validation.statusCode,
            latencyMs = validation.latencyMs,
            registeredGateway = config,
            models = models,
            message = "Successfully registered gateway with ${models.size} models (${validation.latencyMs}ms latency)."
        )
    }

    private fun selectDefaultModel(models: List<LlmModel>, providerType: ProviderType): String {
        if (models.isEmpty()) return "gpt-4o"
        // Look for common flagship models
        return models.firstOrNull { it.id == "gpt-4o" }?.id
            ?: models.firstOrNull { it.id.contains("gpt-4", ignoreCase = true) }?.id
            ?: models.firstOrNull { it.id.contains("claude-3-5", ignoreCase = true) }?.id
            ?: models.firstOrNull { it.id.contains("gemini", ignoreCase = true) }?.id
            ?: models.firstOrNull { it.id.contains("llama-3", ignoreCase = true) }?.id
            ?: models.first().id
    }
}
