package com.example.personalmusicplayer.player

import android.content.Context

/**
 * Последняя очередь воспроизведения: чтобы после перезапуска приложения плеер вернулся
 * к той же песне и позиции (на паузе).
 */
class PlaybackStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)

    data class Saved(
        val songIds: List<Long>,
        val currentId: Long?,
        val positionMs: Long,
        val repeatMode: Int,
    )

    fun saveQueue(songIds: List<Long>, currentId: Long?, positionMs: Long, repeatMode: Int) {
        prefs.edit()
            .putString(KEY_IDS, songIds.joinToString(","))
            .putLong(KEY_CURRENT, currentId ?: -1L)
            .putLong(KEY_POSITION, positionMs)
            .putInt(KEY_REPEAT, repeatMode)
            .apply()
    }

    fun savePosition(currentId: Long?, positionMs: Long) {
        prefs.edit()
            .putLong(KEY_CURRENT, currentId ?: -1L)
            .putLong(KEY_POSITION, positionMs)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun load(): Saved? {
        val raw = prefs.getString(KEY_IDS, null) ?: return null
        val ids = raw.split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return Saved(
            songIds = ids,
            currentId = prefs.getLong(KEY_CURRENT, -1L).takeIf { it >= 0 },
            positionMs = prefs.getLong(KEY_POSITION, 0L),
            repeatMode = prefs.getInt(KEY_REPEAT, 0),
        )
    }

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_CURRENT = "current"
        const val KEY_POSITION = "position"
        const val KEY_REPEAT = "repeat"
    }
}
