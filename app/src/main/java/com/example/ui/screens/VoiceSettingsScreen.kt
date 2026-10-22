package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgentState
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieBackground
import com.example.ui.theme.HexzieBorderGlowBrush
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexzieSurfaceElevated
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.viewmodel.HexzieViewModel

@Composable
fun VoiceSettingsScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedWakePhrase by remember { mutableStateOf("Hey Hex") }
    var porcupineKey by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val micSensitivity by viewModel.micSensitivity.collectAsStateWithLifecycle()
    val micLevel by viewModel.micLevel.collectAsStateWithLifecycle()
    val agentState by viewModel.agentState.collectAsStateWithLifecycle()
    val isListening = agentState == AgentState.LISTENING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Voice & Wake Word",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HexzieOnSurface,
                fontFamily = SpaceGrotesk
            )
            Text(
                text = "Hands-free activation and speech synthesis parameters",
                fontSize = 12.sp,
                color = HexzieOnSurfaceMuted,
                fontFamily = SpaceGrotesk
            )
        }

        // Wake Phrases Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HexzieCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = HexzieCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Wake Activation Phrase", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                        Text("Say this phrase hands-free to awaken HexZie", fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                listOf("Hey Hex", "Hexzie", "Hey Hexzie").forEach { phrase ->
                    val isSelected = selectedWakePhrase == phrase
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) HexzieSurfaceElevated else HexzieBackground)
                            .border(1.dp, if (isSelected) HexzieCyan else HexzieOutline, RoundedCornerShape(10.dp))
                            .clickable { selectedWakePhrase = phrase }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(phrase, fontSize = 14.sp, color = HexzieOnSurface, fontFamily = SpaceGrotesk, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HexzieCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = HexzieCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ACTIVE", fontSize = 9.sp, color = HexzieCyan, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Microphone Sensitivity & Noise Filter Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HexzieCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Mic Sensitivity",
                                tint = HexzieCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Microphone Sensitivity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HexzieOnSurface,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                "Wake-word trigger threshold & noise gain",
                                fontSize = 11.sp,
                                color = HexzieOnSurfaceMuted,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }

                    // Percentage Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HexzieSurfaceElevated)
                            .border(1.dp, HexzieCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(micSensitivity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HexzieCyan,
                            fontFamily = JetBrainsMono
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lower sensitivity suppresses ambient chatter in loud or crowded environments. Higher sensitivity allows hands-free activation from across the room or in quiet spaces.",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Slider
                Slider(
                    value = micSensitivity,
                    onValueChange = { viewModel.setMicSensitivity(it) },
                    valueRange = 0.10f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = HexzieCyan,
                        activeTrackColor = HexzieCyan,
                        inactiveTrackColor = HexzieOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mic_sensitivity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "10% (High Noise Suppression)",
                        fontSize = 10.sp,
                        color = HexzieOnSurfaceMuted,
                        fontFamily = JetBrainsMono
                    )
                    Text(
                        "100% (High Gain / Far-Field)",
                        fontSize = 10.sp,
                        color = HexzieOnSurfaceMuted,
                        fontFamily = JetBrainsMono
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Text(
                    "Environment Presets",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = JetBrainsMono
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple("Noisy (35%)", 0.35f, "preset_noisy"),
                        Triple("Balanced (65%)", 0.65f, "preset_balanced"),
                        Triple("Distant (90%)", 0.90f, "preset_distant")
                    )

                    presets.forEach { (label, value, tag) ->
                        val isSelected = kotlin.math.abs(micSensitivity - value) < 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HexzieCyan.copy(alpha = 0.2f) else HexzieSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) HexzieCyan else HexzieOutline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setMicSensitivity(value) }
                                .padding(vertical = 8.dp)
                                .testTag(tag),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) HexzieCyan else HexzieOnSurface,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Audio Level & Test Feedback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HexzieBackground)
                        .border(1.dp, HexzieOutline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = "Live Meter",
                                    tint = if (isListening) HexzieCyan else HexzieOnSurfaceMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isListening) "Mic Active (Live Level)" else "Audio Input Level",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isListening) HexzieCyan else HexzieOnSurface,
                                    fontFamily = SpaceGrotesk
                                )
                            }

                            Button(
                                onClick = {
                                    if (isListening) viewModel.stopVoiceListening()
                                    else viewModel.startVoiceListening()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) HexzieSurfaceElevated else HexzieCyan.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("test_mic_button")
                            ) {
                                Text(
                                    text = if (isListening) "Stop Test" else "Test Mic",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HexzieCyan,
                                    fontFamily = JetBrainsMono
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Audio meter bar
                        val animatedLevel by animateFloatAsState(
                            targetValue = if (isListening) micLevel else 0f,
                            label = "live_audio_meter"
                        )

                        LinearProgressIndicator(
                            progress = { animatedLevel.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (animatedLevel > 0.8f) HexzieAccent else HexzieCyan,
                            trackColor = HexzieSurfaceElevated
                        )
                    }
                }
            }
        }

        // Picovoice Porcupine AccessKey Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HexzieAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Key", tint = HexzieAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Porcupine Offline Wake (Optional)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                        Text("Ultra low-power on-device keyword detector", fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Enter your free Picovoice AccessKey to enable offline, battery-efficient wake word keyword detection.",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = porcupineKey,
                    onValueChange = { porcupineKey = it },
                    label = { Text("Porcupine AccessKey") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HexzieOnSurface,
                        unfocusedTextColor = HexzieOnSurface,
                        focusedBorderColor = HexzieCyan,
                        unfocusedBorderColor = HexzieOutline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Default Digital Assistant Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HexzieOutline, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HexzieCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assistant, contentDescription = "Assistant", tint = HexzieCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Device Assistant Role", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                        Text("Bind to power button or home button swipe", fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Configure HexZie as your default Android assistant app to launch the voice orb directly from any screen.",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HexzieSurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = HexzieBorderGlowBrush
                    )
                ) {
                    Text("Open Default Assistant Settings", fontSize = 12.sp, color = HexzieCyan, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
