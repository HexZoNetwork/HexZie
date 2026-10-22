package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class SttResult {
    data class Partial(val text: String) : SttResult()
    data class Final(val text: String) : SttResult()
    data class Error(val message: String) : SttResult()
    data class RmsChanged(val rmsdB: Float) : SttResult()
    object EndOfSpeech : SttResult()
}

class AndroidSttEngine(private val context: Context) {

    fun listen(): Flow<SttResult> = callbackFlow {
        // 1. Permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            trySend(SttResult.Error("Microphone permission is required to listen."))
            close()
            return@callbackFlow
        }

        // 2. Availability check
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SttResult.Error("Speech recognition service is unavailable on this device. You can type or use quick actions."))
            close()
            return@callbackFlow
        }

        var recognizer: SpeechRecognizer? = null
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post {
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        trySend(SttResult.RmsChanged(rmsdB))
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        trySend(SttResult.EndOfSpeech)
                    }
                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap orb to try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out. Tap orb to speak."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Tap orb to retry."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection required for online speech recognition."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Retrying..."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer stopped."
                            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognition server disconnected."
                            else -> "Speech recognition ended ($error)."
                        }
                        trySend(SttResult.Error(msg))
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val final = matches?.firstOrNull() ?: ""
                        if (final.isNotBlank()) {
                            trySend(SttResult.Final(final))
                        } else {
                            trySend(SttResult.Error("No clear speech detected."))
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            trySend(SttResult.Partial(partial))
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer?.startListening(intent)
            } catch (e: Exception) {
                trySend(SttResult.Error(e.localizedMessage ?: "Could not start speech recognition."))
            }
        }

        awaitClose {
            mainHandler.post {
                try {
                    recognizer?.stopListening()
                    recognizer?.cancel()
                    recognizer?.destroy()
                } catch (_: Exception) {}
                recognizer = null
            }
        }
    }
}

class AndroidTtsEngine(
    context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private val pendingQueue = mutableListOf<String>()

    private var tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (_: Exception) {
        null
    }

    private var isReady = false
    var isSpeaking = false
        private set

    var isMuted: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }
                })
                isReady = true
                onInitComplete?.invoke(true)

                // Flush pending utterances queued while GoogleTTS APK was initializing
                if (!isMuted && pendingQueue.isNotEmpty()) {
                    val queued = pendingQueue.toList()
                    pendingQueue.clear()
                    queued.forEach { speak(it) }
                }
            } catch (_: Exception) {
                isReady = false
                onInitComplete?.invoke(false)
            }
        } else {
            isReady = false
            onInitComplete?.invoke(false)
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isMuted || text.isBlank()) return
        if (!isReady || tts == null) {
            pendingQueue.add(text)
            return
        }
        try {
            tts?.speak(text, queueMode, null, "hexzie_utterance_" + System.currentTimeMillis())
        } catch (_: Exception) {
            isSpeaking = false
        }
    }

    fun stop() {
        pendingQueue.clear()
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
    }

    fun setSpeechRate(rate: Float) {
        try {
            tts?.setSpeechRate(rate)
        } catch (_: Exception) {}
    }

    fun shutdown() {
        pendingQueue.clear()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isReady = false
    }
}
