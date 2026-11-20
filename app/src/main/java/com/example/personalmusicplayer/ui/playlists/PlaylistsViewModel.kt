package com.example.personalmusicplayer.ui.playlists

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalmusicplayer.PersonalMusicApp
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.data.repository.PlaylistSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Общий для вкладки, страницы плейлиста, плеера и библиотеки (создаётся один раз на Activity). */
class PlaylistsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PersonalMusicApp).playlistRepository
    private val library = (app as PersonalMusicApp).libraryRepository

    /** null — ещё загружается (чтобы не мигала надпись «Плейлистов пока нет»). */
    val playlists: StateFlow<List<PlaylistSummary>?> =
        repo.summaries.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Доступные песни библиотеки — для выбора при добавлении в плейлист. */
    val librarySongs: StateFlow<List<SongEntity>> =
        library.songs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun songsOf(playlistId: Long): Flow<List<SongEntity>> = repo.observeSongs(playlistId)

    suspend fun createPlaylist(name: String): Long = repo.create(name)
    fun rename(id: Long, name: String) { viewModelScope.launch { repo.rename(id, name) } }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
    fun setCover(id: Long, uri: Uri) { viewModelScope.launch { repo.setCoverFromUri(id, uri) } }
    fun resetCover(id: Long) { viewModelScope.launch { repo.resetCover(id) } }
    fun addSongs(id: Long, songIds: List<Long>) { viewModelScope.launch { repo.addSongs(id, songIds) } }
    fun removeSong(id: Long, songId: Long) { viewModelScope.launch { repo.removeSong(id, songId) } }
    fun reorderPlaylists(orderedIds: List<Long>) { viewModelScope.launch { repo.reorderPlaylists(orderedIds) } }
    fun reorder(id: Long, orderedIds: List<Long>) { viewModelScope.launch { repo.reorder(id, orderedIds) } }

    suspend fun playlistIdsOf(songId: Long): Set<Long> = repo.playlistIdsOf(songId)
    fun setMemberships(songId: Long, ids: Set<Long>) { viewModelScope.launch { repo.setMemberships(songId, ids) } }
}
