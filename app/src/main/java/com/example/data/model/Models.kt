package com.example.data.model

enum class PermissionLevel {
    OFF,
    ASK,
    ALWAYS
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class AgentState {
    IDLE,
    LISTENING,
    THINKING,
    ACTING,
    SPEAKING,
    AWAITING_CONFIRMATION,
    EMERGENCY_STOP,
    ERROR
}

enum class ProviderType {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    OLLAMA,
    GEMINI
}

data class GatewayConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val defaultModel: String = "gpt-4o",
    val visionModel: String = "gpt-4o",
    val quickModel: String = "gpt-4o-mini",
    val isEnabled: Boolean = true,
    val customHeadersJson: String = "{}"
)

data class LlmModel(
    val id: String,
    val name: String,
    val supportsTools: Boolean = true,
    val supportsVision: Boolean = false,
    val contextLength: Int = 128000,
    val isFavorite: Boolean = false
)

data class HighRiskActionRequest(
    val callId: String,
    val toolName: String,
    val description: String,
    val argumentsSummary: String,
    val onConfirm: () -> Unit,
    val onDeny: () -> Unit
)
