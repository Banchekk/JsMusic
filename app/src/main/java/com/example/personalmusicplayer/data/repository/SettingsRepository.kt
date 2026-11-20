package com.example.personalmusicplayer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.personalmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val foldersKey = stringSetPreferencesKey("import_folders")
    private val initialScanKey = booleanPreferencesKey("initial_scan_done")
    private val notificationAskedKey = booleanPreferencesKey("notification_permission_asked")

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { name -> AppTheme.entries.firstOrNull { it.name == name } } ?: AppTheme.ORANGE
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[themeKey] = theme.name }
    }

    /** Папки, импортированные вручную: перечитываются при обновлении библиотеки. */
    suspend fun importedFolders(): Set<String> =
        context.dataStore.data.first()[foldersKey] ?: emptySet()

    suspend fun addImportedFolder(uri: String) {
        context.dataStore.edit { it[foldersKey] = (it[foldersKey] ?: emptySet()) + uri }
    }

    suspend fun isInitialScanDone(): Boolean =
        context.dataStore.data.first()[initialScanKey] ?: false

    suspend fun setInitialScanDone() {
        context.dataStore.edit { it[initialScanKey] = true }
    }

    suspend fun isNotificationPermissionAsked(): Boolean =
        context.dataStore.data.first()[notificationAskedKey] ?: false

    suspend fun setNotificationPermissionAsked() {
        context.dataStore.edit { it[notificationAskedKey] = true }
    }
}
