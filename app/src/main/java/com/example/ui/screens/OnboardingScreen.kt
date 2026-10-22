package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.HexzieAccessibilityService
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieAllowGreen
import com.example.ui.theme.HexzieBackground
import com.example.ui.theme.HexzieBorderGlowBrush
import com.example.ui.theme.HexzieCyan
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexzieSurfaceElevated
import com.example.ui.theme.HexzieSurfaceVariant
import com.example.ui.theme.SpaceGrotesk

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isAccessibilityEnabled = HexzieAccessibilityService.serviceConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HexzieBackground)
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome to HexZie",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HexzieOnSurface,
                fontFamily = SpaceGrotesk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your open, auditable, hands-free on-device AI agent.",
                fontSize = 14.sp,
                color = HexzieOnSurfaceMuted,
                fontFamily = SpaceGrotesk
            )

            Spacer(modifier = Modifier.height(24.dp))

            OnboardingStepItem(
                icon = Icons.Default.Accessibility,
                title = "1. Accessibility Service",
                description = "Enables HexZie to read visible screen elements and perform gestures on your behalf.",
                actionLabel = if (isAccessibilityEnabled) "Active" else "Open Accessibility Settings",
                isCompleted = isAccessibilityEnabled,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OnboardingStepItem(
                icon = Icons.Default.Mic,
                title = "2. Audio Streaming & Wake",
                description = "Required for hands-free wake word recognition and streaming speech recognition.",
                actionLabel = "Configured",
                isCompleted = true,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(14.dp))

            OnboardingStepItem(
                icon = Icons.Default.Security,
                title = "3. Zero-Trust Security",
                description = "All tool executions require your configured permission level and are recorded in the local Audit Log.",
                actionLabel = "Enforced",
                isCompleted = true,
                onClick = {}
            )
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = HexzieCyan),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Launch HexZie Console",
                    fontSize = 14.sp,
                    color = HexzieBackground,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepItem(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
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
                        Icon(imageVector = icon, contentDescription = title, tint = HexzieCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HexzieOnSurface, fontFamily = SpaceGrotesk)
                }

                if (isCompleted) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = HexzieAllowGreen, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 12.sp, color = HexzieOnSurfaceMuted, fontFamily = SpaceGrotesk)

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = HexzieBorderGlowBrush
                )
            ) {
                Text(actionLabel, fontSize = 11.sp, color = if (isCompleted) HexzieAllowGreen else HexzieCyan, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
            }
        }
    }
}
