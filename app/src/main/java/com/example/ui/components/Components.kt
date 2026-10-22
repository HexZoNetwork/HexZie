package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HighRiskActionRequest
import com.example.data.model.PermissionLevel
import com.example.data.model.RiskLevel
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAccentElectric
import com.example.ui.theme.HexzieAllowGreen
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
import kotlin.math.sin

@Composable
fun ToolCallCard(
    toolName: String,
    riskLevel: RiskLevel,
    argumentsJson: String?,
    resultText: String?,
    isCompleted: Boolean = true,
    isSuccess: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val barColor = when (riskLevel) {
        RiskLevel.LOW -> HexzieCyan
        RiskLevel.MEDIUM -> HexzieAccent
        RiskLevel.HIGH -> HexzieDenyRed
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left-edge risk indicator bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (expanded) 140.dp else 52.dp)
                    .background(barColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HexzieSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Tool",
                                tint = HexzieCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toolName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HexzieOnSurface,
                            fontFamily = JetBrainsMono
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Risk Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor.copy(alpha = 0.2f))
                                .border(0.5.dp, barColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = riskLevel.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = barColor,
                                fontFamily = JetBrainsMono
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCompleted) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = if (isSuccess) "Success" else "Failed",
                                tint = if (isSuccess) HexzieAllowGreen else HexzieDenyRed,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Pending",
                                tint = HexziePendingAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = HexzieOnSurfaceMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        if (!argumentsJson.isNullOrBlank()) {
                            Text(
                                text = "PARAMETERS",
                                fontSize = 10.sp,
                                color = HexzieOnSurfaceMuted,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = argumentsJson,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                color = HexzieOnSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HexzieSurfaceElevated)
                                    .border(1.dp, HexzieOutline, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                        }

                        if (!resultText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "OUTPUT",
                                fontSize = 10.sp,
                                color = HexzieOnSurfaceMuted,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = resultText,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                color = if (isSuccess) HexzieOnSurface else HexzieDenyRed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HexzieSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSuccess) HexzieOutline else HexzieDenyRed.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighRiskConfirmCard(
    request: HighRiskActionRequest,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HexzieSurfaceElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .border(2.dp, HexzieDenyRed, RoundedCornerShape(16.dp))
            .testTag("high_risk_confirm_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(HexzieDenyRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = HexzieDenyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "High-Risk Action Confirmation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HexzieDenyRed,
                        fontFamily = SpaceGrotesk
                    )
                    Text(
                        text = "Explicit authorization required",
                        fontSize = 11.sp,
                        color = HexzieOnSurfaceMuted,
                        fontFamily = SpaceGrotesk
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = request.description,
                fontSize = 13.sp,
                color = HexzieOnSurface,
                fontFamily = SpaceGrotesk,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Parameters: ${request.argumentsSummary}",
                fontSize = 11.sp,
                fontFamily = JetBrainsMono,
                color = HexzieOnSurfaceMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HexzieSurfaceVariant)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { request.onDeny() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HexzieOnSurfaceMuted),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(HexzieOutline, HexzieOutline))),
                    modifier = Modifier.testTag("deny_button")
                ) {
                    Text("Cancel", fontFamily = SpaceGrotesk, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { request.onConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = HexzieDenyRed),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("confirm_button")
                ) {
                    Text("Confirm & Execute", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SegmentedPermissionControl(
    selected: PermissionLevel,
    onSelect: (PermissionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HexzieSurfaceElevated)
            .border(1.dp, HexzieOutline, RoundedCornerShape(20.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(
            PermissionLevel.OFF to "Off",
            PermissionLevel.ASK to "Ask",
            PermissionLevel.ALWAYS to "Always"
        ).forEach { (level, label) ->
            val isSelected = selected == level
            val targetBg by animateColorAsState(
                targetValue = when {
                    !isSelected -> Color.Transparent
                    level == PermissionLevel.OFF -> HexzieOnSurfaceMuted.copy(alpha = 0.25f)
                    level == PermissionLevel.ASK -> HexziePendingAmber.copy(alpha = 0.25f)
                    else -> HexzieAllowGreen.copy(alpha = 0.25f)
                },
                label = "perm_bg"
            )
            val targetColor by animateColorAsState(
                targetValue = when {
                    !isSelected -> HexzieOnSurfaceMuted
                    level == PermissionLevel.OFF -> HexzieOnSurface
                    level == PermissionLevel.ASK -> HexziePendingAmber
                    else -> HexzieAllowGreen
                },
                label = "perm_color"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(targetBg)
                    .clickable { onSelect(level) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = targetColor,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    }
}

@Composable
fun AudioVisualizerWaveform(
    micLevel: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 12
        for (i in 0 until barCount) {
            val dynamicHeight = if (isSpeaking) {
                (10.dp + (sin(phase + i * 0.6f) * 12.dp.value).dp.coerceAtLeast(4.dp))
            } else {
                (6.dp + (micLevel * 20.dp.value * sin(phase + i * 0.8f).coerceAtLeast(0.1f)).dp)
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(dynamicHeight.coerceIn(4.dp, 26.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSpeaking) HexzieAllowGreen else HexzieCyan
                    )
            )
        }
    }
}
