package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val gatewayId: String = "",
    val modelName: String = "",
    val totalTokens: Int = 0,
    val estimatedCostUsd: Double = 0.0
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val role: String, // "user", "assistant", "tool", "system"
    val content: String,
    val toolCallsJson: String? = null,
    val toolResultJson: String? = null,
    val toolName: String? = null,
    val riskLevel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val riskLevel: String,
    val argumentsSanitized: String,
    val resultStatus: String, // "SUCCESS", "DENIED", "FAILED", "BLOCKED_BY_SAFETY"
    val approvalSource: String, // "ALWAYS_ALLOW", "USER_CONFIRMED", "DENIED"
    val durationMs: Long = 0
)
