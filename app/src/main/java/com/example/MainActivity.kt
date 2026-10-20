package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.theme.StupidAiTheme

enum class AppScreen {
    CHAT,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()

            StupidAiTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

                    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                        when (screen) {
                            AppScreen.CHAT -> {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    onNavigateToSettings = {
                                        currentScreen = AppScreen.SETTINGS
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.CHAT
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
