package com.example.voice

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.HexzieApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Core voice-first listening service.
 * Uses a StateFlow to manage the recording status, allowing the AI to toggle
 * dynamically between 'listening' and 'processing' states.
 */
class HexzieVoiceService : Service() {

    inner class VoiceServiceBinder : Binder() {
        fun getService(): HexzieVoiceService = this@HexzieVoiceService
        val recordingStatus: StateFlow<RecordingStatus> get() = this@HexzieVoiceService.recordingStatus
    }

    private val binder = VoiceServiceBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Internal instance-level StateFlow for recording status
    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private var audioRecordJob: Job? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_AGENT -> {
                serviceRunning = false
                updateStatus(RecordingStatus.IDLE)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_RECORDING -> {
                toggleListeningProcessing()
            }
            ACTION_SET_LISTENING -> {
                isPaused = false
                updateStatus(RecordingStatus.LISTENING)
            }
            ACTION_SET_PROCESSING -> {
                isPaused = false
                updateStatus(RecordingStatus.PROCESSING)
            }
            ACTION_PAUSE -> {
                isPaused = true
                updateStatus(RecordingStatus.IDLE)
            }
            ACTION_RESUME -> {
                isPaused = false
                updateStatus(RecordingStatus.LISTENING)
            }
            else -> {
                serviceRunning = true
                isPaused = false
                val requestedStatusName = intent?.getStringExtra(EXTRA_INITIAL_STATUS)
                val initialStatus = if (requestedStatusName != null) {
                    try { RecordingStatus.valueOf(requestedStatusName) } catch (_: Exception) { RecordingStatus.LISTENING }
                } else {
                    RecordingStatus.LISTENING
                }
                updateStatus(initialStatus)
                startForeground(NOTIFICATION_ID, buildNotification(initialStatus))
            }
        }
        return START_STICKY
    }

    /**
     * Toggles between 'listening' and 'processing' states.
     */
    fun toggleListeningProcessing() {
        val next = _recordingStatus.value.toggled()
        updateStatus(next)
    }

    /**
     * Sets the recording status and triggers appropriate audio/notification behaviors.
     */
    fun setRecordingStatus(status: RecordingStatus) {
        updateStatus(status)
    }

    private fun updateStatus(status: RecordingStatus) {
        _recordingStatus.value = status
        _globalRecordingStatus.value = status

        when (status) {
            RecordingStatus.LISTENING -> {
                startAudioMonitoring()
            }
            RecordingStatus.PROCESSING -> {
                stopAudioMonitoring()
            }
            RecordingStatus.IDLE, RecordingStatus.ERROR -> {
                stopAudioMonitoring()
            }
        }

        updateNotification(status)
    }

    private fun startAudioMonitoring() {
        audioRecordJob?.cancel()
        audioRecordJob = serviceScope.launch(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(this@HexzieVoiceService, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val buffer = ShortArray(bufferSize / 2)
                    while (isActive && _recordingStatus.value == RecordingStatus.LISTENING) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                sum += buffer[i] * buffer[i]
                            }
                            val rms = Math.sqrt(sum / read)
                            val normalized = (rms / 32768.0).coerceIn(0.0, 1.0).toFloat()
                            _micLevel.value = normalized
                            _globalMicLevel.value = normalized
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently fallback if mic is unavailable or testing headless
            } finally {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopAudioMonitoring() {
        audioRecordJob?.cancel()
        audioRecordJob = null
        _micLevel.value = 0f
        _globalMicLevel.value = 0f
    }

    private fun buildNotification(status: RecordingStatus): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, HexzieVoiceService::class.java).apply { action = ACTION_STOP_AGENT },
            PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, HexzieVoiceService::class.java).apply { action = ACTION_TOGGLE_RECORDING },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, HexzieVoiceService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (statusTitle, statusText, toggleLabel) = when (status) {
            RecordingStatus.LISTENING -> Triple(
                "HexZie AI — Listening",
                "Listening for voice commands...",
                "Process AI"
            )
            RecordingStatus.PROCESSING -> Triple(
                "HexZie AI — Processing",
                "Reasoning & formulating response...",
                "Listen"
            )
            RecordingStatus.IDLE -> Triple(
                "HexZie AI — Standby",
                if (isPaused) "Service paused" else "Ready to listen",
                "Start Listening"
            )
            RecordingStatus.ERROR -> Triple(
                "HexZie AI — Error",
                "Listening issue encountered",
                "Retry"
            )
        }

        val builder = NotificationCompat.Builder(this, HexzieApplication.CHANNEL_VOICE_SERVICE)
            .setContentTitle(statusTitle)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, toggleLabel, toggleIntent)
            .addAction(0, if (isPaused) "Resume" else "Pause", pauseResumeIntent)
            .addAction(0, "Emergency Stop", stopIntent)

        return builder.build()
    }

    private fun updateNotification(status: RecordingStatus) {
        val notification = buildNotification(status)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioMonitoring()
        serviceScope.cancel()
        serviceRunning = false
        _recordingStatus.value = RecordingStatus.IDLE
        _globalRecordingStatus.value = RecordingStatus.IDLE
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.voice.START"
        const val ACTION_STOP_AGENT = "com.example.voice.STOP"
        const val ACTION_PAUSE = "com.example.voice.PAUSE"
        const val ACTION_RESUME = "com.example.voice.RESUME"
        const val ACTION_TOGGLE_RECORDING = "com.example.voice.TOGGLE_RECORDING"
        const val ACTION_SET_LISTENING = "com.example.voice.SET_LISTENING"
        const val ACTION_SET_PROCESSING = "com.example.voice.SET_PROCESSING"
        const val EXTRA_INITIAL_STATUS = "EXTRA_INITIAL_STATUS"

        @Volatile
        private var instance: HexzieVoiceService? = null

        var serviceRunning = false
            private set
        var isPaused = false
            private set

        // Global StateFlows for recording status and mic level
        private val _globalRecordingStatus = MutableStateFlow(RecordingStatus.IDLE)
        val recordingStatusFlow: StateFlow<RecordingStatus> = _globalRecordingStatus.asStateFlow()

        private val _globalMicLevel = MutableStateFlow(0f)
        val micLevelFlow: StateFlow<Float> = _globalMicLevel.asStateFlow()

        fun startService(context: Context, initialStatus: RecordingStatus = RecordingStatus.LISTENING) {
            val intent = Intent(context, HexzieVoiceService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INITIAL_STATUS, initialStatus.name)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _globalRecordingStatus.value = initialStatus
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HexzieVoiceService::class.java).apply { action = ACTION_STOP_AGENT }
            context.startService(intent)
            _globalRecordingStatus.value = RecordingStatus.IDLE
        }

        fun toggleRecording(context: Context) {
            val next = _globalRecordingStatus.value.toggled()
            _globalRecordingStatus.value = next
            instance?.setRecordingStatus(next) ?: run {
                val intent = Intent(context, HexzieVoiceService::class.java).apply { action = ACTION_TOGGLE_RECORDING }
                context.startService(intent)
            }
        }

        fun setRecordingStatus(status: RecordingStatus) {
            _globalRecordingStatus.value = status
            instance?.setRecordingStatus(status)
        }
    }
}
