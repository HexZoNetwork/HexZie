package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AuditLogEntity
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAllowGreen
import com.example.ui.theme.HexzieBorderGlowBrush
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieDenyRed
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexziePendingAmber
import com.example.ui.theme.HexzieSurfaceElevated
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SpaceGrotesk
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Audit Trail Section: Displays a transparent list of the last 10 AI actions,
 * including timestamps, tool usage summaries, risk classifications, and execution statuses.
 */
@Composable
fun AuditTrailSection(
    auditLogs: List<AuditLogEntity>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 10
) {
    val recentActions = remember(auditLogs, maxItems) {
        auditLogs.take(maxItems)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HexzieOutline, RoundedCornerShape(16.dp))
            .testTag("audit_trail_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(HexzieCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Audit Trail",
                            tint = HexzieCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Audit Trail",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HexzieOnSurface,
                                fontFamily = SpaceGrotesk
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HexzieSurfaceElevated)
                                    .border(1.dp, HexzieOutline, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Last ${recentActions.size}",
                                    fontSize = 10.sp,
                                    color = HexzieCyan,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Transparent ledger of autonomous voice actions",
                            fontSize = 11.sp,
                            color = HexzieOnSurfaceMuted,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HexzieSurfaceElevated)
                        .clickable { onViewAllClick() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "View All",
                            fontSize = 11.sp,
                            color = HexzieCyan,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View All Logs",
                            tint = HexzieCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (recentActions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HexzieSurfaceElevated.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No recent AI actions recorded",
                            fontSize = 13.sp,
                            color = HexzieOnSurfaceMuted,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Speak a command or tap a directive to see tool usage here in real time.",
                            fontSize = 11.sp,
                            color = HexzieOutline,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentActions.forEachIndexed { index, action ->
                        AuditTrailActionItem(
                            action = action,
                            formattedTime = timeFormat.format(Date(action.timestamp)),
                            isLatest = index == 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTrailActionItem(
    action: AuditLogEntity,
    formattedTime: String,
    isLatest: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (action.resultStatus) {
        "SUCCESS" -> HexzieAllowGreen
        "DENIED", "BLOCKED_BY_SAFETY" -> HexzieDenyRed
        else -> HexziePendingAmber
    }

    val riskColor = when (action.riskLevel) {
        "HIGH" -> HexzieDenyRed
        "MEDIUM" -> HexzieAccent
        else -> HexzieCyan
    }

    val toolIcon = getToolIcon(action.toolName)
    val humanSummary = getHumanToolSummary(action.toolName, action.argumentsSanitized)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) HexzieSurfaceElevated else HexzieSurfaceElevated.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isLatest) HexzieBorderGlowBrush else androidx.compose.ui.graphics.SolidColor(HexzieOutline.copy(alpha = 0.6f)),
                RoundedCornerShape(10.dp)
            )
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = toolIcon,
                            contentDescription = action.toolName,
                            tint = riskColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = action.toolName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMono,
                                color = HexzieOnSurface
                            )
                            if (isLatest) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HexzieCyan.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("LATEST", fontSize = 8.sp, color = HexzieCyan, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
                                }
                            }
                        }
                        Text(
                            text = humanSummary,
                            fontSize = 11.sp,
                            color = HexzieOnSurfaceMuted,
                            fontFamily = SpaceGrotesk,
                            maxLines = if (expanded) 3 else 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMono,
                            color = HexzieOnSurfaceMuted
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = action.resultStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = JetBrainsMono
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = HexzieOutline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HexzieSurfaceVariant)
                        .border(1.dp, HexzieOutline, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "METRICS & APPROVAL",
                        fontSize = 9.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = HexzieOnSurfaceMuted
                    )
                    Text(
                        text = "Approval Source: ${action.approvalSource} • Risk Tier: ${action.riskLevel} • Duration: ${action.durationMs}ms",
                        fontSize = 10.sp,
                        color = HexzieOnSurface,
                        fontFamily = SpaceGrotesk
                    )

                    if (action.argumentsSanitized.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PARAMETERS:",
                            fontSize = 9.sp,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = HexzieOnSurfaceMuted
                        )
                        Text(
                            text = action.argumentsSanitized,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMono,
                            color = HexzieCyan
                        )
                    }
                }
            }
        }
    }
}

private fun getToolIcon(toolName: String): ImageVector {
    return when {
        toolName.contains("screen_read") -> Icons.Default.Smartphone
        toolName.contains("tap") || toolName.contains("swipe") || toolName.contains("scroll") -> Icons.Default.TouchApp
        toolName.contains("camera") -> Icons.Default.CameraAlt
        toolName.contains("volume") -> Icons.Default.VolumeUp
        toolName.contains("flashlight") -> Icons.Default.FlashlightOn
        toolName.contains("app") -> Icons.Default.SmartDisplay
        toolName.contains("call") || toolName.contains("sms") -> Icons.Default.Phone
        toolName.contains("web") -> Icons.Default.Public
        toolName.contains("notification") -> Icons.Default.Notifications
        else -> Icons.Default.Code
    }
}

private fun getHumanToolSummary(toolName: String, argsJson: String): String {
    return try {
        val obj = if (argsJson.startsWith("{")) JSONObject(argsJson) else null
        when (toolName) {
            "open_app" -> "Opened app: " + (obj?.optString("appName") ?: obj?.optString("packageName") ?: "requested app")
            "screen_read" -> "Inspected visible UI elements on screen"
            "screen_tap" -> "Tapped screen at (${obj?.optInt("x") ?: 0}, ${obj?.optInt("y") ?: 0})"
            "screen_type_text" -> "Typed text into focused field"
            "set_volume" -> "Adjusted media volume to ${obj?.optInt("level") ?: 50}%"
            "toggle_flashlight" -> "Toggled device flashlight state"
            "calendar_read" -> "Queried upcoming calendar events"
            "contacts_search" -> "Searched address book for contacts"
            "notification_read" -> "Read incoming notification stream"
            "web_fetch", "web_search" -> "Fetched web resource or search query"
            "global_action" -> "Executed system navigation action: " + (obj?.optString("action") ?: "gesture")
            else -> if (argsJson.isNotBlank() && argsJson != "{}") "Executed $toolName with parameters" else "Executed $toolName"
        }
    } catch (_: Exception) {
        "Executed $toolName"
    }
}
