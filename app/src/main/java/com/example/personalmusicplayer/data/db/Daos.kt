package com.example.personalmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    /** Та же композиция, добавленная другим путём (например, через MediaStore и папку). */
    @Query(
        "SELECT * FROM songs WHERE title = :title COLLATE NOCASE AND artist IS :artist " +
            "AND ABS(durationMs - :durationMs) < 1000 LIMIT 1"
    )
    suspend fun findDuplicate(title: String, artist: String?, durationMs: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Query(
        "UPDATE songs SET title = :title, artist = :artist, album = :album, " +
            "durationMs = :durationMs, albumArtUri = :albumArtUri, isAvailable = 1 WHERE uri = :uri"
    )
    suspend fun updateMetadata(
        uri: String, title: String, artist: String?, album: String?,
        durationMs: Long, albumArtUri: String?,
    )

    /** Файл переехал: прежняя запись (заметка, плейлисты) привязывается к новому месту. */
    @Query(
        "UPDATE songs SET uri = :uri, title = :title, artist = :artist, album = :album, " +
            "durationMs = :durationMs, isAvailable = 1 WHERE id = :id"
    )
    suspend fun relink(id: Long, uri: String, title: String, artist: String?, album: String?, durationMs: Long)

    @Query("UPDATE songs SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)

    @Query("UPDATE songs SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: Long, available: Boolean)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    /** Обложки песен всех плейлистов по порядку — для количества и автоколлажа. */
    @Query(
        "SELECT ps.playlistId AS playlistId, s.albumArtUri AS albumArtUri FROM playlist_songs ps " +
            "JOIN songs s ON s.id = ps.songId WHERE s.isAvailable = 1 ORDER BY ps.playlistId, ps.position"
    )
    fun observeArts(): Flow<List<PlaylistSongArt>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun songIdsIn(playlistId: Long): List<Long>

    @Query("SELECT playlistId FROM playlist_songs WHERE songId = :songId")
    suspend fun playlistIdsOf(songId: Long): List<Long>

    @Query("SELECT * FROM playlists ORDER BY position ASC, id DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("SELECT COALESCE(MIN(position), 0) FROM playlists")
    suspend fun minPosition(): Int

    @Query("UPDATE playlists SET position = :position WHERE id = :id")
    suspend fun setPlaylistPosition(id: Long, position: Int)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE playlists SET coverUri = :coverUri WHERE id = :id")
    suspend fun setCover(id: Long, coverUri: String?)

    /** Удаляет только плейлист и связи (CASCADE); песни и аудиофайлы остаются. */
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "SELECT s.* FROM songs s JOIN playlist_songs ps ON ps.songId = s.id " +
            "WHERE ps.playlistId = :playlistId AND s.isAvailable = 1 ORDER BY ps.position"
    )
    fun observeSongs(playlistId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(link: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun setPosition(playlistId: Long, songId: Long, position: Int)
}

data class PlaylistSongArt(val playlistId: Long, val albumArtUri: String?)
