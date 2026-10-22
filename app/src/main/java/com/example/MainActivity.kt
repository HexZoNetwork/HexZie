package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.GatewaysScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionCenterScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.HexzieAccent
import com.example.ui.theme.HexzieBackground
import com.example.ui.theme.HexzieOnSurface
import com.example.ui.theme.HexzieOnSurfaceMuted
import com.example.ui.theme.HexzieOutline
import com.example.ui.theme.HexzieSurface
import com.example.ui.theme.HexzieTheme
import com.example.ui.viewmodel.HexzieViewModel

enum class HexzieScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Mic),
    CHAT("Chat", Icons.Default.Chat),
    GATEWAYS("Gateways", Icons.Default.Power),
    PERMISSIONS("Safety", Icons.Default.Security),
    AUDIT("Audit", Icons.Default.History),
    VOICE("Voice", Icons.Default.Tune)
}

class MainActivity : ComponentActivity() {

    private val viewModel: HexzieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val voiceTrigger = intent?.getBooleanExtra("EXTRA_VOICE_TRIGGER", false) ?: false

        setContent {
            HexzieTheme {
                MainScreen(
                    viewModel = viewModel,
                    initialVoiceTrigger = voiceTrigger
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: HexzieViewModel,
    initialVoiceTrigger: Boolean
) {
    var currentScreen by remember { mutableStateOf(HexzieScreen.HOME) }

    // Runtime Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    androidx.activity.compose.BackHandler(enabled = currentScreen != HexzieScreen.HOME) {
        currentScreen = HexzieScreen.HOME
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        if (initialVoiceTrigger) {
            viewModel.startVoiceListening()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = HexzieSurface,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                HexzieScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontFamily = com.example.ui.theme.SpaceGrotesk,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HexzieBackground,
                            selectedTextColor = com.example.ui.theme.HexzieCyan,
                            indicatorColor = com.example.ui.theme.HexzieCyan,
                            unselectedIconColor = HexzieOnSurfaceMuted,
                            unselectedTextColor = HexzieOnSurfaceMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HexzieBackground)
        ) {
            when (currentScreen) {
                HexzieScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { currentScreen = HexzieScreen.CHAT },
                    onNavigateToAudit = { currentScreen = HexzieScreen.AUDIT }
                )
                HexzieScreen.CHAT -> ChatScreen(viewModel = viewModel)
                HexzieScreen.GATEWAYS -> GatewaysScreen(viewModel = viewModel)
                HexzieScreen.PERMISSIONS -> PermissionCenterScreen(viewModel = viewModel)
                HexzieScreen.AUDIT -> AuditLogScreen(viewModel = viewModel)
                HexzieScreen.VOICE -> VoiceSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
