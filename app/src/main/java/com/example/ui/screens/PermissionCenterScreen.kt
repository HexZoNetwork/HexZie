package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PermissionLevel
import com.example.ui.components.SegmentedPermissionControl
import com.example.ui.theme.HexzieAccent
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

@Composable
fun PermissionCenterScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    val masterEnabled by viewModel.masterAgentEnabled.collectAsState()
    val safeMode by viewModel.safeModeEnabled.collectAsState()
    val permissionsMap by viewModel.permissionsMap.collectAsState()

    var showRevokeDialog by remember { mutableStateOf(false) }

    val groupDescriptions = mapOf(
        "screen_read" to ("Screen Reading" to "Inspect visible UI elements, text, and buttons."),
        "screen_control" to ("Screen Control" to "Tap, swipe, scroll, and type into app inputs."),
        "screen_capture" to ("Screen Capture" to "Capture on-device screenshots for model vision."),
        "camera" to ("Camera" to "Take photos for visual inspection."),
        "microphone" to ("Microphone & Wake" to "Listen for hands-free wake phrase and audio STT."),
        "apps" to ("Apps" to "Open and query installed applications."),
        "device_settings" to ("Device Settings" to "Adjust volume, flashlight, alarms, and timers."),
        "phone_calls" to ("Phone Calls" to "Initiate phone calls to contacts or numbers."),
        "sms" to ("SMS Messages" to "Read and send text messages."),
        "contacts" to ("Contacts" to "Search contact list names and phone numbers."),
        "calendar" to ("Calendar" to "Read upcoming events and schedule items."),
        "files" to ("Files" to "Read and write authorized app storage files."),
        "clipboard" to ("Clipboard" to "Read and paste system clipboard text."),
        "location" to ("Location" to "Retrieve current geographic coordinates."),
        "web_access" to ("Web Access" to "Fetch web pages and perform online queries."),
        "notifications" to ("Notifications" to "Read active notifications and reply to chats."),
        "agent_utilities" to ("Agent Utilities" to "Ask clarifying questions and save todos.")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Permission Center",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexzieOnSurface,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "Fine-grained on-device capability tiers",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )
            }

            Button(
                onClick = { showRevokeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HexzieDenyRed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("revoke_all_button")
            ) {
                Icon(imageVector = Icons.Default.Block, contentDescription = "Revoke All", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Revoke All", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Controls Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HexzieSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HexzieBorderGlowBrush, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Master Agent Toggle
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HexzieCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "Agent", tint = HexzieCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Autonomous Execution", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                            Text("Allows HexZie to trigger device actions", fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                        }
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { viewModel.setMasterAgentEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = HexzieCyan, checkedTrackColor = HexzieCyan.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Safe Mode Toggle
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HexziePendingAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Safe", tint = HexziePendingAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Universal Safe Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                            Text("Forces confirmation for all actions", fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                        }
                    }
                    Switch(
                        checked = safeMode,
                        onCheckedChange = { viewModel.setSafeMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = HexziePendingAmber, checkedTrackColor = HexziePendingAmber.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Capability Tiers",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HexzieOnSurface,
                fontFamily = SpaceGrotesk
            )
            Text(
                text = "Off / Ask / Always",
                fontSize = 10.sp,
                color = HexzieOnSurfaceMuted,
                fontFamily = JetBrainsMono
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Capability Tiers List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.capabilityGroups) { groupKey ->
                val (title, desc) = groupDescriptions[groupKey] ?: (groupKey to "")
                val currentLevel = permissionsMap[groupKey] ?: PermissionLevel.ASK

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                            Text(desc, fontSize = 11.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        SegmentedPermissionControl(
                            selected = currentLevel,
                            onSelect = { newLevel ->
                                viewModel.setPermission(groupKey, newLevel)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            containerColor = HexzieSurfaceElevated,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = HexzieDenyRed, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Revoke All Permissions?", color = HexzieOnSurface, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            },
            text = {
                Text(
                    text = "This will immediately turn OFF all capability permissions, preventing HexZie from reading the screen or dispatching device actions.",
                    color = HexzieOnSurfaceMuted,
                    fontSize = 13.sp,
                    fontFamily = SpaceGrotesk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revokeAllPermissions()
                        showRevokeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HexzieDenyRed)
                ) {
                    Text("Revoke All", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRevokeDialog = false }) {
                    Text("Cancel", color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)
                }
            }
        )
    }
}
