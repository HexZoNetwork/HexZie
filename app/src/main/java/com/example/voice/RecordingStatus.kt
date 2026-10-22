package com.example.voice

/**
 * Represents the recording and processing status of the core voice-first listening service.
 * Supports toggling seamlessly between 'listening' and 'processing' states.
 */
enum class RecordingStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR;

    val isListening: Boolean get() = this == LISTENING
    val isProcessing: Boolean get() = this == PROCESSING
    val isActive: Boolean get() = this == LISTENING || this == PROCESSING

    fun toggled(): RecordingStatus = when (this) {
        LISTENING -> PROCESSING
        PROCESSING -> LISTENING
        else -> LISTENING
    }
}
