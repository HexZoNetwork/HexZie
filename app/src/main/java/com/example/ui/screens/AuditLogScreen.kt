package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HexzieAccent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    viewModel: HexzieViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.auditLogs.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "SUCCESS" -> logs.filter { it.resultStatus == "SUCCESS" }
            "DENIED" -> logs.filter { it.resultStatus == "DENIED" || it.resultStatus.contains("BLOCK") }
            "HIGH" -> logs.filter { it.riskLevel == "HIGH" }
            else -> logs
        }
    }

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
            Column {
                Text(
                    text = "Security Audit Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexzieOnSurface,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "Immutable record of all autonomous tool calls",
                    fontSize = 12.sp,
                    color = HexzieOnSurfaceMuted,
                    fontFamily = SpaceGrotesk
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val textDump = logs.joinToString("\n---\n") {
                                "[${dateFormat.format(Date(it.timestamp))}] ${it.toolName} (${it.riskLevel}) -> ${it.resultStatus} (${it.durationMs}ms)\nArgs: ${it.argumentsSanitized}"
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("HexZie Audit Log", textDump))
                            Toast.makeText(context, "Audit log copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy All",
                            tint = HexzieCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.clearAuditLogs() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = HexzieOnSurfaceMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All (${logs.size})", "SUCCESS" to "Success", "DENIED" to "Blocked/Denied", "HIGH" to "High Risk").forEach { (filterKey, label) ->
                val isSelected = selectedFilter == filterKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) HexzieSurfaceElevated else HexzieSurfaceVariant)
                        .border(1.dp, if (isSelected) HexzieCyan else HexzieOutline, RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = filterKey }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) HexzieCyan else HexzieOnSurfaceMuted,
                        fontFamily = SpaceGrotesk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(HexzieSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "No Logs",
                            tint = HexzieCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No audit log entries found.", color = HexzieOnSurfaceMuted, fontSize = 14.sp, fontFamily = SpaceGrotesk)
                    Text("Tool actions will be recorded here securely.", color = HexzieOutline, fontSize = 12.sp, fontFamily = SpaceGrotesk)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { entry ->
                    val statusColor = when (entry.resultStatus) {
                        "SUCCESS" -> HexzieAllowGreen
                        "DENIED", "BLOCKED_BY_SAFETY" -> HexzieDenyRed
                        else -> HexziePendingAmber
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HexzieSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HexzieOutline, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.toolName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMono,
                                        color = HexzieOnSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusColor.copy(alpha = 0.2f))
                                            .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(entry.resultStatus, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor, fontFamily = JetBrainsMono)
                                    }
                                }

                                Text(
                                    text = dateFormat.format(Date(entry.timestamp)),
                                    fontSize = 11.sp,
                                    color = HexzieOnSurfaceMuted,
                                    fontFamily = JetBrainsMono
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Source: ${entry.approvalSource} • Risk: ${entry.riskLevel} • Execution: ${entry.durationMs}ms",
                                fontSize = 11.sp,
                                color = HexzieOnSurfaceMuted,
                                fontFamily = SpaceGrotesk
                            )

                            if (entry.argumentsSanitized.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.argumentsSanitized,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMono,
                                    color = HexzieOnSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(HexzieSurfaceElevated)
                                        .border(0.5.dp, HexzieOutline, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
