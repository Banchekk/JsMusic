package com.example.personalmusicplayer.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Песня. Заметка хранится здесь, а не в плейлисте, поэтому она привязана к композиции
 * и не теряется при пересканировании (updateMetadata не трогает заметку).
 */
@Entity(tableName = "songs", indices = [Index(value = ["uri"], unique = true)])
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0,
    val albumArtUri: String? = null,
    val note: String? = null,
    /** false, если файл удалён или перемещён: запись и заметка сохраняются. */
    val isAvailable: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** URI обложки из галереи; null — авто-коллаж из первых четырёх песен. */
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Порядок в списке плейлистов (меняется перетаскиванием); меньше — выше. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)

/** Связь «многие ко многим» с порядком: одна песня может быть в нескольких плейлистах. */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(PlaylistEntity::class, ["id"], ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SongEntity::class, ["id"], ["songId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("songId")],
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)
