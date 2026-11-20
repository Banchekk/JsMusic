package com.example.personalmusicplayer

import android.app.Application
import android.os.SystemClock
import com.example.personalmusicplayer.data.db.AppDatabase
import com.example.personalmusicplayer.data.repository.LibraryRepository
import com.example.personalmusicplayer.data.repository.PlaylistRepository
import com.example.personalmusicplayer.data.repository.SettingsRepository
import com.example.personalmusicplayer.data.scan.MusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Простой контейнер зависимостей: без DI-фреймворка, чтобы не усложнять архитектуру. */
class PersonalMusicApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(database.songDao(), MusicScanner(this))
    }
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(this, database) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastAvailabilityCheck = 0L

    /** Скрывает из библиотеки песни, файлов которых больше нет. Не чаще раза в 30 секунд. */
    fun refreshLibraryAvailability() {
        val now = SystemClock.elapsedRealtime()
        if (lastAvailabilityCheck != 0L && now - lastAvailabilityCheck < 30_000) return
        lastAvailabilityCheck = now
        appScope.launch {
            try {
                libraryRepository.checkAllAvailability()
            } catch (e: Exception) {
                // Проверка вспомогательная; следующая попытка — при следующем открытии.
            }
        }
    }
}
