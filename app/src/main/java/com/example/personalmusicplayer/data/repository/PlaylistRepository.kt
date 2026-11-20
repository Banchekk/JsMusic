package com.example.personalmusicplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.personalmusicplayer.data.db.AppDatabase
import com.example.personalmusicplayer.data.db.PlaylistEntity
import com.example.personalmusicplayer.data.db.PlaylistSongEntity
import com.example.personalmusicplayer.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/** Плейлист для сетки: количество песен и обложки первых четырёх (для автоколлажа). */
data class PlaylistSummary(
    val playlist: PlaylistEntity,
    val songCount: Int,
    val previewArts: List<String?>,
)

class PlaylistRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val dao = db.playlistDao()

    val summaries: Flow<List<PlaylistSummary>> = combine(dao.observeAll(), dao.observeArts()) { playlists, arts ->
        val byPlaylist = arts.groupBy { it.playlistId }
        playlists.map { playlist ->
            val items = byPlaylist[playlist.id].orEmpty()
            PlaylistSummary(playlist, items.size, items.take(4).map { it.albumArtUri })
        }
    }

    fun observeSongs(playlistId: Long): Flow<List<SongEntity>> = dao.observeSongs(playlistId)

    suspend fun songsOnce(playlistId: Long): List<SongEntity> = dao.observeSongs(playlistId).first()

    /** Новый плейлист появляется первым в списке. */
    suspend fun create(name: String): Long =
        dao.insert(PlaylistEntity(name = name.trim(), position = dao.minPosition() - 1))

    /** Сохраняет порядок плейлистов, полученный перетаскиванием. */
    suspend fun reorderPlaylists(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.setPlaylistPosition(id, index) }
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    /** Удаляет плейлист и его связи. Песни и аудиофайлы не затрагиваются. */
    suspend fun delete(id: Long) {
        val oldCover = dao.get(id)?.coverUri
        dao.delete(id)
        oldCover?.let { deleteCoverFile(it) }
    }

    /** Копирует выбранное изображение в хранилище приложения, чтобы обложка не пропала. */
    suspend fun setCoverFromUri(id: Long, uri: Uri) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, COVERS_DIR).apply { mkdirs() }
        val file = File(dir, "${id}_${System.currentTimeMillis()}.jpg")
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (!copied) return@withContext
        val oldCover = dao.get(id)?.coverUri
        dao.setCover(id, Uri.fromFile(file).toString())
        oldCover?.let { deleteCoverFile(it) }
    }

    /** Возврат к автоматической обложке (коллаж из обложек первых четырёх песен). */
    suspend fun resetCover(id: Long) {
        val oldCover = dao.get(id)?.coverUri
        dao.setCover(id, null)
        oldCover?.let { deleteCoverFile(it) }
    }

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) = db.withTransaction {
        val existing = dao.songIdsIn(playlistId).toSet()
        var position = dao.nextPosition(playlistId)
        for (songId in songIds) {
            if (songId !in existing) dao.addSong(PlaylistSongEntity(playlistId, songId, position++))
        }
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = dao.removeSong(playlistId, songId)

    /** Сохраняет порядок песен, полученный перетаскиванием. */
    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) = db.withTransaction {
        orderedSongIds.forEachIndexed { index, songId -> dao.setPosition(playlistId, songId, index) }
    }

    suspend fun playlistIdsOf(songId: Long): Set<Long> = dao.playlistIdsOf(songId).toSet()

    /** Одна песня — сразу в несколько плейлистов: приводит членство к выбранному набору. */
    suspend fun setMemberships(songId: Long, target: Set<Long>) = db.withTransaction {
        val current = dao.playlistIdsOf(songId).toSet()
        for (playlistId in target - current) {
            dao.addSong(PlaylistSongEntity(playlistId, songId, dao.nextPosition(playlistId)))
        }
        for (playlistId in current - target) dao.removeSong(playlistId, songId)
    }

    private fun deleteCoverFile(uriString: String) {
        val file = Uri.parse(uriString).path?.let { File(it) } ?: return
        if (file.parentFile?.name == COVERS_DIR) file.delete()
    }

    private companion object {
        const val COVERS_DIR = "playlist_covers"
    }
}
