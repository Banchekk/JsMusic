package com.example.personalmusicplayer.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalmusicplayer.PersonalMusicApp
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.data.repository.SyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LibraryMessage {
    data class Synced(val result: SyncResult) : LibraryMessage
    data object Failed : LibraryMessage
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val library = (app as PersonalMusicApp).libraryRepository
    private val settings = (app as PersonalMusicApp).settingsRepository

    /** null — список ещё загружается (чтобы не показывать пустую библиотеку на долю секунды). */
    val songs: StateFlow<List<SongEntity>?> =
        library.songs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _messages = Channel<LibraryMessage>(Channel.BUFFERED)
    val messages: Flow<LibraryMessage> = _messages.receiveAsFlow()

    /** Первый запуск: автоматическое сканирование после получения разрешения. */
    fun onPermissionGranted() {
        viewModelScope.launch {
            if (!settings.isInitialScanDone()) {
                runSync {
                    library.sync(scanMediaStore = true, folders = savedFolders())
                        .also { settings.setInitialScanDone() }
                }
            }
        }
    }

    /** Повторное сканирование (и перечитывание ранее добавленных папок). */
    fun rescan(hasPermission: Boolean) {
        viewModelScope.launch {
            runSync { library.sync(scanMediaStore = hasPermission, folders = savedFolders()) }
        }
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch { runSync { library.importFiles(uris) } }
    }

    fun importFolder(uri: Uri) {
        viewModelScope.launch {
            runSync {
                settings.addImportedFolder(uri.toString())
                library.importFolder(uri)
            }
        }
    }

    private suspend fun savedFolders(): List<Uri> =
        settings.importedFolders().map { Uri.parse(it) }

    private suspend fun runSync(block: suspend () -> SyncResult) {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            _messages.send(LibraryMessage.Synced(block()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.send(LibraryMessage.Failed)
        } finally {
            _isScanning.value = false
        }
    }
}
