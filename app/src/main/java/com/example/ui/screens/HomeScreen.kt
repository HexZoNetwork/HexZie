package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentState
import com.example.voice.RecordingStatus
import com.example.ui.components.AudioVisualizerWaveform
import com.example.ui.components.AuditTrailSection
import com.example.ui.components.CenterVoiceRippleStage
import com.example.ui.components.HexzieOrb
import com.example.ui.components.HighRiskConfirmCard
import com.example.ui.components.RealtimeFluidWaveform
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAccentElectric
import com.example.ui.theme.HexzieAllowGreen
import com.example.ui.theme.HexzieBackground
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
import com.example.ui.viewmodel.HexzieViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HexzieViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val agentState by viewModel.agentState.collectAsState()
    val recordingStatus by viewModel.recordingStatus.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val micLevel by viewModel.micLevel.collectAsState()
    val activeConfirmation by viewModel.activeConfirmation.collectAsState()
    val activeGateway by viewModel.activeGateway.collectAsState()
    val safeMode by viewModel.safeModeEnabled.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    val scrollState = rememberScrollState()

    // Blinking streaming cursor
    val cursorTransition = rememberInfiniteTransition(label = "cursor_anim")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    val statePillColor by animateColorAsState(
        targetValue = when (agentState) {
            AgentState.IDLE -> HexzieAccent
            AgentState.LISTENING -> HexzieCyan
            AgentState.THINKING -> HexzieAccentElectric
            AgentState.ACTING -> HexzieAccent
            AgentState.SPEAKING -> HexzieAllowGreen
            AgentState.AWAITING_CONFIRMATION -> HexziePendingAmber
            AgentState.EMERGENCY_STOP, AgentState.ERROR -> HexzieDenyRed
        },
        label = "state_pill_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Model & Safety Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(HexzieSurfaceElevated)
                        .border(1.dp, HexzieBorderGlowBrush, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (activeGateway != null) HexzieCyan else HexzieDenyRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeGateway?.let { "${it.name} • ${it.defaultModel}" } ?: "No Gateway Selected",
                        fontSize = 12.sp,
                        color = HexzieOnSurface,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SpaceGrotesk
                    )
                    if (safeMode) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(HexziePendingAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Safe Mode Active",
                                    tint = HexziePendingAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SAFE", fontSize = 9.sp, color = HexziePendingAmber, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // State Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statePillColor.copy(alpha = 0.15f))
                        .border(1.dp, statePillColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statePillColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (agentState) {
                                AgentState.IDLE -> "Tap orb or say \"Hey Hex\""
                                AgentState.LISTENING -> "Listening to audio..."
                                AgentState.THINKING -> "Reasoning & Planning..."
                                AgentState.ACTING -> "Executing device tool..."
                                AgentState.SPEAKING -> "HexZie Speaking..."
                                AgentState.AWAITING_CONFIRMATION -> "Confirmation required"
                                AgentState.EMERGENCY_STOP -> "Emergency Stopped"
                                AgentState.ERROR -> "Agent Error"
                            },
                            fontSize = 12.sp,
                            color = statePillColor,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }

                // Core Voice Service Status & Mode Switch (Listening <-> Processing)
                AnimatedVisibility(
                    visible = recordingStatus.isActive || agentState == AgentState.LISTENING || agentState == AgentState.THINKING,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(HexzieSurfaceElevated)
                            .border(1.dp, HexzieBorderGlowBrush, RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Listening segment
                        val isListeningActive = recordingStatus == RecordingStatus.LISTENING || agentState == AgentState.LISTENING
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isListeningActive) HexzieCyan.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    if (!isListeningActive) {
                                        viewModel.toggleVoiceListeningProcessing()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("toggle_listening_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Listening Mode",
                                    tint = if (isListeningActive) HexzieCyan else HexzieOnSurfaceMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Listening",
                                    fontSize = 11.sp,
                                    color = if (isListeningActive) HexzieCyan else HexzieOnSurfaceMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Processing segment
                        val isProcessingActive = recordingStatus == RecordingStatus.PROCESSING || agentState == AgentState.THINKING
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isProcessingActive) HexzieAccentElectric.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    if (!isProcessingActive) {
                                        viewModel.toggleVoiceListeningProcessing()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("toggle_processing_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "Processing Mode",
                                    tint = if (isProcessingActive) HexzieAccentElectric else HexzieOnSurfaceMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Processing",
                                    fontSize = 11.sp,
                                    color = if (isProcessingActive) HexzieAccentElectric else HexzieOnSurfaceMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        }
                    }
                }
            }

            // Center Stage: Real-Time Waveform & Ripple Visualization
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                CenterVoiceRippleStage(
                    agentState = agentState,
                    micLevel = micLevel,
                    onClick = { viewModel.toggleVoiceListeningProcessing() },
                    onLongClick = { viewModel.emergencyKillSwitch() },
                    stageSize = 310.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Real-Time Fluid Sinusoidal Waveform reacting directly to microphone input
                RealtimeFluidWaveform(
                    micLevel = micLevel,
                    isListening = agentState == AgentState.LISTENING,
                    isSpeaking = agentState == AgentState.SPEAKING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    height = 54.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live transcript stream card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HexzieSurfaceElevated.copy(alpha = 0.75f))
                        .border(1.dp, HexzieOutline, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("live_transcript"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = liveTranscript.ifEmpty { "Ready. Speak a command or question." },
                            fontSize = 15.sp,
                            color = if (liveTranscript.isEmpty()) HexzieOnSurfaceMuted else HexzieOnSurface,
                            textAlign = TextAlign.Center,
                            fontFamily = SpaceGrotesk,
                            lineHeight = 20.sp
                        )
                        if (agentState == AgentState.LISTENING || agentState == AgentState.THINKING) {
                            Text(
                                text = " ▍",
                                color = HexzieCyan.copy(alpha = cursorAlpha),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // High Risk Confirmation Modal Card
            activeConfirmation?.let { req ->
                HighRiskConfirmCard(request = req)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Actions & Bottom Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Directives",
                        fontSize = 12.sp,
                        color = HexzieOnSurfaceMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpaceGrotesk
                    )
                    Text(
                        text = "Touch to dispatch",
                        fontSize = 10.sp,
                        color = HexzieOutline,
                        fontFamily = JetBrainsMono
                    )
                }

                val quickDirectives = listOf<Triple<String, ImageVector, () -> Unit>>(
                    Triple("Read Screen", Icons.Default.Smartphone) { viewModel.sendTextMessage("Read the screen and tell me what is visible") },
                    Triple("Open YouTube", Icons.Default.SmartDisplay) { viewModel.sendTextMessage("Open YouTube app") },
                    Triple("Volume 50%", Icons.Default.VolumeUp) { viewModel.sendTextMessage("Set media volume to 50 percent") },
                    Triple("Flashlight", Icons.Default.FlashlightOn) { viewModel.sendTextMessage("Turn on flashlight") },
                    Triple("Calendar", Icons.Default.CalendarToday) { viewModel.sendTextMessage("Check my upcoming calendar events") },
                    Triple("Alarm 7 AM", Icons.Default.Alarm) { viewModel.sendTextMessage("Set an alarm for 7:00 AM") },
                    Triple("Notifications", Icons.Default.NotificationsActive) { viewModel.sendTextMessage("Check my recent notifications") }
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    quickDirectives.forEach { (label, icon, action) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(HexzieSurfaceVariant)
                                .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
                                .clickable { action() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = HexzieCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = HexzieOnSurface,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audit Trail Section: Displays last 10 AI actions with timestamps & tool summaries
                AuditTrailSection(
                    auditLogs = auditLogs,
                    onViewAllClick = onNavigateToAudit,
                    modifier = Modifier.fillMaxWidth(),
                    maxItems = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Bar: Chat Switcher & Red Kill Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(HexzieSurfaceElevated)
                            .border(1.dp, HexzieBorderGlowBrush, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToChat() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Open Chat Console",
                                tint = HexzieCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agent Console",
                                fontSize = 13.sp,
                                color = HexzieOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }

                    // Emergency Kill Switch Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(HexzieDenyRed, Color(0xFFB91C1C))
                                )
                            )
                            .clickable { viewModel.emergencyKillSwitch() }
                            .padding(vertical = 12.dp)
                            .testTag("kill_switch_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Emergency Stop",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kill Switch",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }
    }
}
