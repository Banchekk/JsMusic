package com.example.personalmusicplayer.ui.components

import com.example.personalmusicplayer.data.db.SongEntity

/** Поиск по названию, исполнителю, альбому и заметке (без учёта регистра). */
fun SongEntity.matchesQuery(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        artist.orEmpty().contains(query, ignoreCase = true) ||
        album.orEmpty().contains(query, ignoreCase = true) ||
        note.orEmpty().contains(query, ignoreCase = true)
