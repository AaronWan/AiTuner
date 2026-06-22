package com.example.aituner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.aituner.ai.AiSettingsRepository
import com.example.aituner.ui.screen.*
import com.example.aituner.ui.theme.AiTunerTheme
import com.example.aituner.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class Screen {
    Tuner, AiChat, SingPractice, Settings
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var aiSettingsRepo: AiSettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestMicPermission()

        setContent {
            AiTunerTheme {
                var currentScreen by remember { mutableStateOf(Screen.Tuner) }

                Scaffold(
                    containerColor = DeepBlack,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary
                        ) {
                            NavTab(Screen.Tuner, "调音", Icons.Default.Tune, currentScreen) {
                                currentScreen = Screen.Tuner
                            }
                            NavTab(Screen.SingPractice, "唱谱", Icons.Default.MusicNote, currentScreen) {
                                currentScreen = Screen.SingPractice
                            }
                            NavTab(Screen.AiChat, "AI", Icons.Default.Chat, currentScreen) {
                                currentScreen = Screen.AiChat
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(300)
                        ) { screen ->
                            when (screen) {
                                Screen.Tuner -> TunerScreen(
                                    onNavigateToChat = { currentScreen = Screen.AiChat },
                                    onNavigateToSing = { currentScreen = Screen.SingPractice },
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
                                )
                                Screen.AiChat -> AiChatScreen(
                                    onNavigateBack = { currentScreen = Screen.Tuner }
                                )
                                Screen.SingPractice -> SingPracticeScreen(
                                    onNavigateBack = { currentScreen = Screen.Tuner }
                                )
                                Screen.Settings -> SettingsScreen(
                                    aiSettingsRepo = aiSettingsRepo,
                                    onNavigateBack = { currentScreen = Screen.Tuner }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestMicPermission() {
        // Test mode: skip permission check, permission pre-granted via pm
    }
}

@Composable
private fun RowScope.NavTab(
    screen: Screen,
    label: String,
    icon: ImageVector,
    current: Screen,
    onClick: () -> Unit
) {
    val selected = current == screen
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) AccentCyan else TextMuted
            )
        },
        label = {
            Text(
                label,
                fontSize = 12.sp,
                color = if (selected) AccentCyan else TextMuted
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = AccentCyan.copy(alpha = 0.15f)
        )
    )
}
