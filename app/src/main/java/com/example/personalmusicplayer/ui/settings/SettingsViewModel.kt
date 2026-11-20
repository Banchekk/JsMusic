package com.example.personalmusicplayer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalmusicplayer.PersonalMusicApp
import com.example.personalmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as PersonalMusicApp).settingsRepository

    // Тема читается сразу (несколько миллисекунд), чтобы при запуске не мигала тема по умолчанию.
    val theme: StateFlow<AppTheme> = repository.theme.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        runBlocking { repository.theme.first() },
    )

    suspend fun isNotificationPermissionAsked(): Boolean = repository.isNotificationPermissionAsked()

    fun markNotificationPermissionAsked() {
        viewModelScope.launch { repository.setNotificationPermissionAsked() }
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }
}
