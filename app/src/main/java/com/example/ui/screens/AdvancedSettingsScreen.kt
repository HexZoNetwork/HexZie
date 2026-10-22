package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DataStoreManager
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieBackground
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.viewmodel.HexzieViewModel

@Composable
fun AdvancedSettingsScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    var maxSteps by remember { mutableFloatStateOf(25f) }
    var systemPrompt by remember { mutableStateOf(DataStoreManager.DEFAULT_SYSTEM_PROMPT) }
    val scrollState = rememberScrollState()

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
                text = "Advanced Agent Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HexzieOnSurface
            )
            Text(
                text = "Autonomous reasoning parameters and agent behavior prompt",
                fontSize = 12.sp,
                color = HexzieOnSurfaceMuted
            )
        }

        // Max Steps Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Loop Steps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface)
                    Text("${maxSteps.toInt()} steps", fontSize = 13.sp, color = HexzieAccent, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Limits how many autonomous tool actions the agent can perform in a single session before stopping.",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted
                )

                Slider(
                    value = maxSteps,
                    onValueChange = { maxSteps = it },
                    valueRange = 5f..50f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = HexzieAccent, activeTrackColor = HexzieAccent)
                )
            }
        }

        // System Prompt Editor
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System Prompt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Defines HexZie's persona, tool-calling guidance, and safety constraints.",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HexzieOnSurface,
                        unfocusedTextColor = HexzieOnSurface,
                        focusedBorderColor = HexzieAccent,
                        unfocusedBorderColor = HexzieOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { systemPrompt = DataStoreManager.DEFAULT_SYSTEM_PROMPT },
                    colors = ButtonDefaults.buttonColors(containerColor = HexzieBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reset to Default", fontSize = 11.sp, color = HexzieOnSurfaceMuted)
                }
            }
        }
    }
}
