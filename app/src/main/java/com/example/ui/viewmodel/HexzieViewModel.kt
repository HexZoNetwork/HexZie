package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HexzieApplication
import com.example.agent.AgentLoopEvent
import com.example.agent.HexzieAgentLoop
import com.example.data.local.AuditLogEntity
import com.example.data.local.DataStoreManager
import com.example.data.local.MessageEntity
import com.example.data.local.SessionEntity
import com.example.data.model.AgentState
import com.example.data.model.GatewayConfig
import com.example.data.model.HighRiskActionRequest
import com.example.data.model.LlmModel
import com.example.data.model.PermissionLevel
import com.example.data.model.ProviderType
import com.example.llm.CustomGatewayScanner
import com.example.llm.CustomGatewayScannerService
import com.example.llm.GatewayFactory
import com.example.llm.GatewayValidationResult
import com.example.tools.ToolRegistry
import com.example.voice.AndroidSttEngine
import com.example.voice.AndroidTtsEngine
import com.example.voice.HexzieVoiceService
import com.example.voice.RecordingStatus
import com.example.voice.SttResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HexzieViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HexzieApplication
    private val db = app.database
    private val dataStore = app.dataStoreManager
    private val securityVault = app.securityVault
    private val toolRegistry = ToolRegistry(application, db)
    private val agentLoop = HexzieAgentLoop(application, db, dataStore, toolRegistry)

    private val sttEngine = AndroidSttEngine(application)
    private val ttsEngine = AndroidTtsEngine(application)

    // Current Session
    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // Sessions & Messages
    val sessions: StateFlow<List<SessionEntity>> = db.sessionDao().getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    // Agent State & Visuals
    private val _agentState = MutableStateFlow(AgentState.IDLE)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    val recordingStatus: StateFlow<RecordingStatus> = HexzieVoiceService.recordingStatusFlow

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    // Confirmation Request
    private val _activeConfirmation = MutableStateFlow<HighRiskActionRequest?>(null)
    val activeConfirmation: StateFlow<HighRiskActionRequest?> = _activeConfirmation.asStateFlow()

    // Gateways
    private val _gateways = MutableStateFlow<List<GatewayConfig>>(emptyList())
    val gateways: StateFlow<List<GatewayConfig>> = _gateways.asStateFlow()

    private val _activeGateway = MutableStateFlow<GatewayConfig?>(null)
    val activeGateway: StateFlow<GatewayConfig?> = _activeGateway.asStateFlow()

    private val _availableModels = MutableStateFlow<List<LlmModel>>(emptyList())
    val availableModels: StateFlow<List<LlmModel>> = _availableModels.asStateFlow()

    private val _gatewayLatency = MutableStateFlow<Long?>(null)
    val gatewayLatency: StateFlow<Long?> = _gatewayLatency.asStateFlow()

    private val _validationResult = MutableStateFlow<GatewayValidationResult?>(null)
    val validationResult: StateFlow<GatewayValidationResult?> = _validationResult.asStateFlow()

    private val gatewayScanner = CustomGatewayScanner()
    private val gatewayScannerService by lazy {
        CustomGatewayScannerService(gatewayScanner, securityVault, dataStore)
    }

    // Settings
    val safeModeEnabled = dataStore.safeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val masterAgentEnabled = dataStore.masterAgentEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val micSensitivity = dataStore.micSensitivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.65f)

    val auditLogs: StateFlow<List<AuditLogEntity>> = db.auditLogDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Capability groups
    val capabilityGroups = listOf(
        "screen_read", "screen_control", "screen_capture", "camera", "microphone",
        "apps", "device_settings", "phone_calls", "sms", "contacts", "calendar",
        "files", "clipboard", "location", "web_access", "notifications", "agent_utilities"
    )

    private val _permissionsMap = MutableStateFlow<Map<String, PermissionLevel>>(emptyMap())
    val permissionsMap: StateFlow<Map<String, PermissionLevel>> = _permissionsMap.asStateFlow()

    private var sttJob: Job? = null
    private var agentJob: Job? = null

    init {
        loadGateways()
        loadPermissions()
        ensureInitialSession()
        observeMessages()
    }

    private fun ensureInitialSession() {
        viewModelScope.launch {
            val list = db.sessionDao().getAllSessions().first()
            if (list.isEmpty()) {
                val newSession = SessionEntity(
                    id = _currentSessionId.value,
                    title = "New Assistant Session"
                )
                db.sessionDao().insertSession(newSession)
            } else {
                _currentSessionId.value = list.first().id
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            _currentSessionId.collect { id ->
                db.messageDao().getMessagesForSession(id).collect {
                    _messages.value = it
                }
            }
        }
    }

    private fun loadGateways() {
        viewModelScope.launch {
            val json = dataStore.gatewaysJsonFlow.first()
            val list = mutableListOf<GatewayConfig>()
            if (!json.isNullOrBlank()) {
                try {
                    val array = JSONArray(json)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.getString("id")
                        val key = securityVault.getApiKey(id)
                        list.add(
                            GatewayConfig(
                                id = id,
                                name = obj.getString("name"),
                                baseUrl = obj.getString("baseUrl"),
                                apiKey = key,
                                providerType = ProviderType.valueOf(obj.optString("providerType", "OPENAI_COMPATIBLE")),
                                defaultModel = obj.optString("defaultModel", "gpt-4o"),
                                isEnabled = obj.optBoolean("isEnabled", true)
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            if (list.isEmpty()) {
                // Pre-configure customizable default gateway
                val defaultGw = GatewayConfig(
                    id = "default_custom_gw",
                    name = "Custom AI Gateway",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "",
                    providerType = ProviderType.OPENAI_COMPATIBLE,
                    defaultModel = "gpt-4o"
                )
                list.add(defaultGw)
            }
            _gateways.value = list
            _activeGateway.value = list.firstOrNull { it.isEnabled } ?: list.firstOrNull()
        }
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val map = mutableMapOf<String, PermissionLevel>()
            for (g in capabilityGroups) {
                map[g] = dataStore.getPermissionFlow(g).first()
            }
            _permissionsMap.value = map
        }
    }

    fun startVoiceListening() {
        if (_agentState.value == AgentState.LISTENING) {
            stopVoiceListening()
            return
        }

        // Barge-in: stop any active speech
        ttsEngine.stop()

        _agentState.value = AgentState.LISTENING
        _liveTranscript.value = "Listening..."
        HexzieVoiceService.startService(app, RecordingStatus.LISTENING)
        HexzieVoiceService.setRecordingStatus(RecordingStatus.LISTENING)

        sttJob?.cancel()
        sttJob = viewModelScope.launch {
            sttEngine.listen().collect { result ->
                when (result) {
                    is SttResult.Partial -> {
                        _liveTranscript.value = result.text
                    }
                    is SttResult.Final -> {
                        _liveTranscript.value = result.text
                        if (result.text.isNotBlank()) {
                            HexzieVoiceService.setRecordingStatus(RecordingStatus.PROCESSING)
                            stopVoiceListening()
                            executePrompt(result.text)
                        }
                    }
                    is SttResult.RmsChanged -> {
                        // Scale input using configured sensitivity gain (0.1..1.0 -> 0.35x..1.45x)
                        val sensitivity = micSensitivity.value
                        val gain = 0.35f + (sensitivity * 1.10f)
                        val baseNormalized = ((result.rmsdB + 2f) / 14f).coerceIn(0.0f, 1.0f)
                        val scaled = (baseNormalized * gain).coerceIn(0.08f, 1.0f)
                        _micLevel.value = scaled
                    }
                    is SttResult.EndOfSpeech -> {
                        _micLevel.value = 0f
                        _agentState.value = AgentState.THINKING
                        HexzieVoiceService.setRecordingStatus(RecordingStatus.PROCESSING)
                    }
                    is SttResult.Error -> {
                        _micLevel.value = 0f
                        _agentState.value = AgentState.IDLE
                        HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
                        _liveTranscript.value = result.message
                    }
                }
            }
        }
    }

    fun stopVoiceListening() {
        sttJob?.cancel()
        sttJob = null
        _micLevel.value = 0f
        if (_agentState.value == AgentState.LISTENING) {
            _agentState.value = AgentState.IDLE
        }
        if (HexzieVoiceService.recordingStatusFlow.value == RecordingStatus.LISTENING) {
            HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
        }
    }

    /**
     * Toggles the voice-first listening service between 'listening' and 'processing' states.
     */
    fun toggleVoiceListeningProcessing() {
        val current = HexzieVoiceService.recordingStatusFlow.value
        val next = current.toggled()
        when (next) {
            RecordingStatus.LISTENING -> {
                HexzieVoiceService.setRecordingStatus(RecordingStatus.LISTENING)
                startVoiceListening()
            }
            RecordingStatus.PROCESSING -> {
                HexzieVoiceService.setRecordingStatus(RecordingStatus.PROCESSING)
                _agentState.value = AgentState.THINKING
                sttJob?.cancel()
                sttJob = null
                _micLevel.value = 0f
                val candidateText = _liveTranscript.value.removePrefix("Listening...").trim()
                if (candidateText.isNotBlank() && !candidateText.startsWith("Error")) {
                    executePrompt(candidateText)
                } else {
                    _liveTranscript.value = "Processing voice command..."
                }
            }
            else -> {
                stopVoiceListening()
            }
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        executePrompt(text)
    }

    private fun executePrompt(prompt: String) {
        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            val gw = _activeGateway.value ?: return@launch
            val provider = GatewayFactory.createProvider(gw)

            HexzieVoiceService.setRecordingStatus(RecordingStatus.PROCESSING)
            _agentState.value = AgentState.THINKING
            _liveTranscript.value = prompt

            agentLoop.runLoop(
                sessionId = _currentSessionId.value,
                userPrompt = prompt,
                provider = provider,
                modelName = gw.defaultModel
            ) { event ->
                when (event) {
                    is AgentLoopEvent.Token -> {
                        _agentState.value = AgentState.THINKING
                    }
                    is AgentLoopEvent.ToolStarting -> {
                        _agentState.value = AgentState.ACTING
                        _liveTranscript.value = "Acting: ${event.toolName}..."
                    }
                    is AgentLoopEvent.ToolFinished -> {
                        _liveTranscript.value = "Completed ${event.toolName}"
                    }
                    is AgentLoopEvent.HighRiskAwaitingConfirmation -> {
                        _agentState.value = AgentState.AWAITING_CONFIRMATION
                        _activeConfirmation.value = event.request
                    }
                    is AgentLoopEvent.Completed -> {
                        _agentState.value = AgentState.SPEAKING
                        _activeConfirmation.value = null
                        _liveTranscript.value = event.answer
                        ttsEngine.speak(event.answer)
                        kotlinx.coroutines.delay(2000)
                        _agentState.value = AgentState.IDLE
                        HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
                    }
                    is AgentLoopEvent.Error -> {
                        _agentState.value = AgentState.ERROR
                        _liveTranscript.value = "Error: ${event.message}"
                        HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
                    }
                    is AgentLoopEvent.Cancelled -> {
                        _agentState.value = AgentState.EMERGENCY_STOP
                        _liveTranscript.value = "Stopped."
                        HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
                    }
                }
            }
        }
    }

    fun emergencyKillSwitch() {
        agentJob?.cancel()
        agentJob = null
        sttJob?.cancel()
        sttJob = null
        ttsEngine.stop()
        agentLoop.emergencyStop()
        HexzieVoiceService.stopService(app)
        HexzieVoiceService.setRecordingStatus(RecordingStatus.IDLE)
        _agentState.value = AgentState.EMERGENCY_STOP
        _liveTranscript.value = "EMERGENCY STOPPED. All actions cancelled."
    }

    fun scanAndValidateGateway(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _validationResult.value = null
            val result = gatewayScanner.scanAndValidate(baseUrl, apiKey)
            _validationResult.value = result
            if (result.isValid) {
                _availableModels.value = result.models
                _gatewayLatency.value = result.latencyMs
            }
        }
    }

    fun scanAndRegisterGateway(baseUrl: String, apiKey: String, name: String = "Custom Gateway") {
        viewModelScope.launch {
            _validationResult.value = null
            val result = gatewayScannerService.scanAndRegister(baseUrl, apiKey, name)
            _validationResult.value = GatewayValidationResult(
                isValid = result.success,
                statusCode = result.statusCode,
                latencyMs = result.latencyMs,
                models = result.models,
                message = result.message
            )
            if (result.success && result.registeredGateway != null) {
                saveGateway(result.registeredGateway)
                _availableModels.value = result.models
                _gatewayLatency.value = result.latencyMs
            }
        }
    }

    fun scanGatewayModels(gateway: GatewayConfig) {
        viewModelScope.launch {
            val key = if (gateway.apiKey.isNotBlank()) gateway.apiKey else securityVault.getApiKey(gateway.id)
            scanAndValidateGateway(gateway.baseUrl, key)
        }
    }

    fun saveGateway(config: GatewayConfig) {
        viewModelScope.launch {
            securityVault.storeApiKey(config.id, config.apiKey)
            val updated = _gateways.value.toMutableList()
            val idx = updated.indexOfFirst { it.id == config.id }
            if (idx >= 0) {
                updated[idx] = config
            } else {
                updated.add(config)
            }
            _gateways.value = updated
            if (_activeGateway.value?.id == config.id || _activeGateway.value == null) {
                _activeGateway.value = config
            }
            persistGateways(updated)
        }
    }

    fun deleteGateway(id: String) {
        viewModelScope.launch {
            val updated = _gateways.value.filter { it.id != id }
            _gateways.value = updated
            if (_activeGateway.value?.id == id) {
                _activeGateway.value = updated.firstOrNull()
            }
            persistGateways(updated)
        }
    }

    private suspend fun persistGateways(list: List<GatewayConfig>) {
        val array = JSONArray()
        for (g in list) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("baseUrl", g.baseUrl)
            obj.put("providerType", g.providerType.name)
            obj.put("defaultModel", g.defaultModel)
            obj.put("isEnabled", g.isEnabled)
            array.put(obj)
        }
        dataStore.saveGatewaysJson(array.toString())
    }

    fun setPermission(group: String, level: PermissionLevel) {
        viewModelScope.launch {
            dataStore.setPermissionLevel(group, level)
            val map = _permissionsMap.value.toMutableMap()
            map[group] = level
            _permissionsMap.value = map
        }
    }

    fun setSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setSafeMode(enabled)
        }
    }

    fun setMasterAgentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setMasterAgentEnabled(enabled)
        }
    }

    fun setMicSensitivity(value: Float) {
        viewModelScope.launch {
            dataStore.setMicSensitivity(value)
        }
    }

    fun revokeAllPermissions() {
        viewModelScope.launch {
            dataStore.revokeAllPermissions(capabilityGroups)
            loadPermissions()
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val session = SessionEntity(
                id = newId,
                title = "Session " + (sessions.value.size + 1)
            )
            db.sessionDao().insertSession(session)
            _currentSessionId.value = newId
        }
    }

    fun switchSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            db.auditLogDao().clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }
}
