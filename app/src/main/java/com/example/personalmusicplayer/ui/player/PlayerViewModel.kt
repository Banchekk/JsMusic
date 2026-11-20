package com.example.personalmusicplayer.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalmusicplayer.PersonalMusicApp
import com.example.personalmusicplayer.player.PlayerController
import com.example.personalmusicplayer.player.PlayerState
import com.example.personalmusicplayer.player.toMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val library = (app as PersonalMusicApp).libraryRepository
    private val playlists = (app as PersonalMusicApp).playlistRepository
    private val controller = PlayerController(app)

    /** Состояние плеера + актуальная заметка к текущей песне из базы. */
    val state: StateFlow<PlayerState> =
        combine(controller.state, library.songs) { s, songs ->
            s.copy(note = songs.firstOrNull { it.id == s.songId }?.note)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerState())

    val position: Flow<Long> = controller.positionFlow()

    init {
        controller.connect()
        viewModelScope.launch {
            controller.playbackErrors.collect { library.markUnavailable(it) }
        }
    }

    /** Очередь — все доступные песни библиотеки в порядке списка; старт с выбранной. */
    fun play(songId: Long) {
        viewModelScope.launch {
            val queue = library.songs.first().filter { it.isAvailable }
            val index = queue.indexOfFirst { it.id == songId }
            if (index >= 0) controller.playQueue(queue.map { it.toMediaItem() }, index)
        }
    }

    /** Очередь — песни плейлиста в его порядке; старт с выбранной. */
    fun playFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val queue = playlists.songsOnce(playlistId)
            val index = queue.indexOfFirst { it.id == songId }
            if (index >= 0) controller.playQueue(queue.map { it.toMediaItem() }, index)
        }
    }

    fun togglePlay() = controller.togglePlay()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun cycleRepeat() = controller.cycleRepeat()
    fun close() = controller.close()

    override fun onCleared() {
        controller.release()
    }
}
