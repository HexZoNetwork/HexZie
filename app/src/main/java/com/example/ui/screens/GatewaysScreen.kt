package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.GatewayConfig
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAllowGreen
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
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GatewaysScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    val gateways by viewModel.gateways.collectAsState()
    val activeGateway by viewModel.activeGateway.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val latency by viewModel.gatewayLatency.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGateway by remember { mutableStateOf<GatewayConfig?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "AI Gateways & Models",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexzieOnSurface,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "Custom Gateway Scanner & API Key Validator",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )
            }

            Button(
                onClick = {
                    editingGateway = null
                    showAddDialog = true
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HexzieCyan),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .testTag("add_gateway_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = HexzieBackground,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Gateway",
                    fontSize = 12.sp,
                    color = HexzieBackground,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner feedback banner
        validationResult?.let { result ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isValid) HexzieAllowGreen.copy(alpha = 0.12f) else HexzieDenyRed.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(
                        1.dp,
                        if (result.isValid) HexzieAllowGreen else HexzieDenyRed,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = "Status",
                        tint = if (result.isValid) HexzieAllowGreen else HexzieDenyRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (result.isValid) "Gateway Verified (${result.latencyMs}ms)" else "Gateway Validation Failed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isValid) HexzieAllowGreen else HexzieDenyRed,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = result.message,
                            fontSize = 12.sp,
                            color = HexzieOnSurface,
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gateways) { gw ->
                val isActive = activeGateway?.id == gw.id

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) HexzieCyan else HexzieOutline,
                            shape = RoundedCornerShape(14.dp)
                        )
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(HexzieSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lan,
                                        contentDescription = "Gateway",
                                        tint = if (isActive) HexzieCyan else HexzieOnSurfaceMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = gw.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HexzieOnSurface,
                                        fontFamily = SpaceGrotesk
                                    )
                                    Text(
                                        text = "Model: ${gw.defaultModel}",
                                        fontSize = 12.sp,
                                        color = HexzieCyan,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(HexzieCyan.copy(alpha = 0.2f))
                                            .border(1.dp, HexzieCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("ACTIVE", fontSize = 10.sp, color = HexzieCyan, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = {
                                        editingGateway = gw
                                        showAddDialog = true
                                    }
                                ) {
                                    Text("Edit", fontSize = 12.sp, color = HexzieAccent, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold)
                                }

                                if (gateways.size > 1) {
                                    IconButton(onClick = { viewModel.deleteGateway(gw.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = HexzieDenyRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = gw.baseUrl,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMono,
                            color = HexzieOnSurfaceMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.scanGatewayModels(gw) },
                                shape = RoundedCornerShape(16.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(HexzieOutline, HexzieOutline))
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = HexzieCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Models", fontSize = 11.sp, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                            }

                            if (isActive && latency != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(HexzieAllowGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = "Latency", tint = HexzieAllowGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${latency}ms", fontSize = 11.sp, color = HexzieAllowGreen, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!isActive) {
                                Button(
                                    onClick = { viewModel.saveGateway(gw.copy(isEnabled = true)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HexzieSurfaceElevated),
                                    shape = RoundedCornerShape(16.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = HexzieBorderGlowBrush
                                    )
                                ) {
                                    Text("Activate", fontSize = 11.sp, color = HexzieCyan, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Scanned models list
            if (availableModels.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discovered Endpoint Models (${availableModels.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HexzieOnSurface,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "Tap to set default",
                            fontSize = 10.sp,
                            color = HexzieOnSurfaceMuted,
                            fontFamily = JetBrainsMono
                        )
                    }
                }

                items(availableModels) { model ->
                    val isCurrentDefault = activeGateway?.defaultModel == model.id

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(HexzieSurfaceElevated)
                            .border(
                                1.dp,
                                if (isCurrentDefault) HexzieCyan else HexzieOutline,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                activeGateway?.let { cur ->
                                    viewModel.saveGateway(cur.copy(defaultModel = model.id))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = model.id,
                                    fontSize = 12.sp,
                                    color = if (isCurrentDefault) HexzieCyan else HexzieOnSurface,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = if (isCurrentDefault) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${model.contextLength / 1024}k context window",
                                    fontSize = 10.sp,
                                    color = HexzieOnSurfaceMuted,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (model.supportsTools) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HexzieAllowGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("TOOLS", fontSize = 9.sp, color = HexzieAllowGreen, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
                                    }
                                }
                                if (model.supportsVision) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HexzieAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("VISION", fontSize = 9.sp, color = HexzieAccent, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
                                    }
                                }
                                if (isCurrentDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = HexzieCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog with live validation and Quick Presets
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingGateway?.name ?: "") }
        var url by remember { mutableStateOf(editingGateway?.baseUrl ?: "https://api.openai.com/v1") }
        var key by remember { mutableStateOf(editingGateway?.apiKey ?: "") }
        var defaultModel by remember { mutableStateOf(editingGateway?.defaultModel ?: "gpt-4o") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = HexzieSurfaceElevated,
            title = {
                Text(
                    text = if (editingGateway == null) "Configure AI Gateway" else "Edit AI Gateway",
                    color = HexzieOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 11.sp,
                        color = HexzieOnSurfaceMuted,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Ollama Local", "http://10.0.2.2:11434", "llama3.2"),
                            Triple("LM Studio", "http://10.0.2.2:1234/v1", "local-model"),
                            Triple("OpenAI", "https://api.openai.com/v1", "gpt-4o"),
                            Triple("OpenRouter", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-70b-instruct")
                        ).forEach { (presetName, presetUrl, presetModel) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HexzieSurfaceVariant)
                                    .border(1.dp, HexzieOutline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        name = presetName
                                        url = presetUrl
                                        defaultModel = presetModel
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = presetName,
                                    fontSize = 10.sp,
                                    color = HexzieCyan,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Gateway Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HexzieOnSurface,
                            unfocusedTextColor = HexzieOnSurface,
                            focusedBorderColor = HexzieCyan,
                            unfocusedBorderColor = HexzieOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Base URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HexzieOnSurface,
                            unfocusedTextColor = HexzieOnSurface,
                            focusedBorderColor = HexzieCyan,
                            unfocusedBorderColor = HexzieOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("API Key (Stored in Keystore)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HexzieOnSurface,
                            unfocusedTextColor = HexzieOnSurface,
                            focusedBorderColor = HexzieCyan,
                            unfocusedBorderColor = HexzieOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = defaultModel,
                        onValueChange = { defaultModel = it },
                        label = { Text("Default Model Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HexzieOnSurface,
                            unfocusedTextColor = HexzieOnSurface,
                            focusedBorderColor = HexzieCyan,
                            unfocusedBorderColor = HexzieOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Test button right in dialog
                    OutlinedButton(
                        onClick = { viewModel.scanAndValidateGateway(url, key) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = HexzieBorderGlowBrush
                        )
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Validate", tint = HexzieCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate API Key & Scan", fontSize = 12.sp, color = HexzieCyan, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                    }

                    validationResult?.let { res ->
                        Text(
                            text = res.message,
                            fontSize = 11.sp,
                            color = if (res.isValid) HexzieAllowGreen else HexzieDenyRed,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontFamily = SpaceGrotesk
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val config = GatewayConfig(
                            id = editingGateway?.id ?: UUID.randomUUID().toString(),
                            name = name.ifBlank { "Custom Gateway" },
                            baseUrl = url.ifBlank { "https://api.openai.com/v1" },
                            apiKey = key,
                            defaultModel = defaultModel.ifBlank { "gpt-4o" }
                        )
                        viewModel.saveGateway(config)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HexzieCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Gateway", color = HexzieBackground, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel", color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                }
            }
        )
    }
}
