package com.example.personalmusicplayer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalmusicplayer.ui.navigation.AppRoot
import com.example.personalmusicplayer.ui.settings.SettingsViewModel
import com.example.personalmusicplayer.ui.theme.PersonalMusicTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        (application as PersonalMusicApp).refreshLibraryAvailability()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val theme by settingsViewModel.theme.collectAsStateWithLifecycle()

            // Значки статус-бара и кнопок навигации подстраиваются под тему приложения,
            // а не под системную тему телефона.
            DisposableEffect(theme.isDark) {
                val style = if (theme.isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            PersonalMusicTheme(appTheme = theme) {
                AppRoot(settingsViewModel = settingsViewModel)
            }
        }
    }
}
