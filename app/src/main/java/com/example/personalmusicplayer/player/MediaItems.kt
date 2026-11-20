package com.example.personalmusicplayer.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.personalmusicplayer.data.db.SongEntity

/** mediaId = id песни в базе; метаданные нужны для уведомления и экрана блокировки. */
fun SongEntity.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
            .setDurationMs(durationMs)
            .build()
    )
    .build()
