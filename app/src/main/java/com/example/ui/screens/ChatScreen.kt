package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.AgentState
import com.example.data.model.RiskLevel
import com.example.ui.components.HighRiskConfirmCard
import com.example.ui.components.RealtimeFluidWaveform
import com.example.ui.components.ToolCallCard
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAccentElectric
import com.example.ui.theme.HexzieBackground
import com.example.ui.theme.HexzieBorderGlowBrush
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieDenyRed
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexzieSurfaceElevated
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.viewmodel.HexzieViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val micLevel by viewModel.micLevel.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val activeConfirmation by viewModel.activeConfirmation.collectAsState()
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val infiniteTransition = rememberInfiniteTransition(label = "thinking_anim")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
    ) {
        // Top Bar: Session controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Agent Console",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexzieOnSurface,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "Autonomous tool execution transcript",
                    fontSize = 11.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(HexzieSurfaceElevated)
                    .border(1.dp, HexzieBorderGlowBrush, RoundedCornerShape(16.dp))
                    .clickable { viewModel.createNewSession() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        tint = HexzieCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Session",
                        fontSize = 12.sp,
                        color = HexzieCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }
            }
        }

        // Empty state or Messages List
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(HexzieSurfaceElevated)
                            .border(1.dp, HexzieBorderGlowBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Assistant",
                            tint = HexzieCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "HexZie Ready",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HexzieOnSurface,
                        fontFamily = SpaceGrotesk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask questions, inspect the screen, or trigger autonomous device operations hands-free.",
                        fontSize = 13.sp,
                        color = HexzieOnSurfaceMuted,
                        fontFamily = SpaceGrotesk,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(messages) { msg ->
                    when (msg.role) {
                        "user" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(HexzieAccentElectric, HexzieAccent)
                                                )
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = msg.content,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = SpaceGrotesk,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        fontSize = 10.sp,
                                        color = HexzieOnSurfaceMuted,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                            }
                        }
                        "assistant" -> {
                            if (msg.content.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(HexzieCyan)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "HEXZIE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = HexzieCyan,
                                                fontFamily = JetBrainsMono
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                                .background(HexzieSurfaceVariant)
                                                .border(1.dp, HexzieOutline, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                                .padding(14.dp)
                                        ) {
                                            Text(
                                                text = msg.content,
                                                color = HexzieOnSurface,
                                                fontSize = 14.sp,
                                                fontFamily = SpaceGrotesk,
                                                lineHeight = 20.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = timeFormat.format(Date(msg.timestamp)),
                                            fontSize = 10.sp,
                                            color = HexzieOnSurfaceMuted,
                                            fontFamily = JetBrainsMono
                                        )
                                    }
                                }
                            }
                        }
                        "tool" -> {
                            val risk = try {
                                RiskLevel.valueOf(msg.riskLevel ?: "LOW")
                            } catch (_: Exception) {
                                RiskLevel.LOW
                            }
                            ToolCallCard(
                                toolName = msg.toolName ?: "tool",
                                riskLevel = risk,
                                argumentsJson = null,
                                resultText = msg.content,
                                isCompleted = true,
                                isSuccess = !msg.content.startsWith("Error:") && !msg.content.startsWith("Permission Denied")
                            )
                        }
                    }
                }

                if (agentState == AgentState.THINKING || agentState == AgentState.ACTING) {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HexzieSurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(HexzieCyan.copy(alpha = dotAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (agentState == AgentState.THINKING) "Reasoning & generating directives..." else "Executing device tool...",
                                fontSize = 12.sp,
                                color = HexzieCyan,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Confirmation Card if pending
        activeConfirmation?.let { req ->
            HighRiskConfirmCard(request = req)
        }

        // Real-Time Audio Waveform when voice is active
        AnimatedVisibility(
            visible = agentState == AgentState.LISTENING || agentState == AgentState.SPEAKING
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                RealtimeFluidWaveform(
                    micLevel = micLevel,
                    isListening = agentState == AgentState.LISTENING,
                    isSpeaking = agentState == AgentState.SPEAKING,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp
                )
                if (liveTranscript.isNotBlank()) {
                    Text(
                        text = liveTranscript,
                        fontSize = 12.sp,
                        color = HexzieCyan,
                        fontFamily = SpaceGrotesk,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .background(HexzieBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Direct HexZie or ask questions...",
                        color = HexzieOnSurfaceMuted,
                        fontSize = 14.sp,
                        fontFamily = SpaceGrotesk
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HexzieCyan,
                    unfocusedBorderColor = HexzieOutline,
                    focusedTextColor = HexzieOnSurface,
                    unfocusedTextColor = HexzieOnSurface,
                    focusedContainerColor = HexzieSurfaceVariant,
                    unfocusedContainerColor = HexzieSurfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic button
            IconButton(
                onClick = { viewModel.startVoiceListening() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(HexzieSurfaceElevated)
                    .border(1.dp, HexzieBorderGlowBrush, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = HexzieCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send button
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendTextMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(HexzieAccentElectric, HexzieCyan)
                        )
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = HexzieBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
