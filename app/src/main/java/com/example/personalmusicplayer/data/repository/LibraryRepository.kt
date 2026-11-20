package com.example.personalmusicplayer.data.repository

import android.net.Uri
import com.example.personalmusicplayer.data.db.SongDao
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.data.scan.MusicScanner
import kotlinx.coroutines.flow.Flow

data class SyncResult(val added: Int = 0, val unavailable: Int = 0, val skipped: Int = 0)

class LibraryRepository(
    private val songDao: SongDao,
    private val scanner: MusicScanner,
) {
    val songs: Flow<List<SongEntity>> = songDao.observeAll()

    /**
     * Обновление библиотеки: MediaStore (если есть разрешение) + ранее импортированные папки.
     * Заметки и плейлисты не затрагиваются; пропавшие файлы помечаются недоступными, а не удаляются.
     */
    suspend fun sync(scanMediaStore: Boolean, folders: Collection<Uri>): SyncResult {
        val found = mutableSetOf<String>()
        var added = 0
        if (scanMediaStore) {
            for (song in scanner.scanMediaStore()) {
                found += song.uri
                if (upsert(song)) added++
            }
        }
        for (folder in folders) {
            for (song in scanner.scanTree(folder)) {
                found += song.uri
                if (upsert(song)) added++
            }
        }
        return SyncResult(added = added, unavailable = refreshAvailability(found))
    }

    suspend fun importFiles(uris: List<Uri>): SyncResult {
        var added = 0
        var skipped = 0
        for (uri in uris) {
            scanner.persistReadPermission(uri)
            val song = scanner.readDocument(uri)
            if (song == null) skipped++ else if (upsert(song)) added++
        }
        return SyncResult(added = added, skipped = skipped)
    }

    suspend fun importFolder(treeUri: Uri): SyncResult {
        scanner.persistReadPermission(treeUri)
        var added = 0
        for (song in scanner.scanTree(treeUri)) {
            if (upsert(song)) added++
        }
        return SyncResult(added = added)
    }

    /** Проверяет все песни: пропавшие скрываются из библиотеки, вернувшиеся — появляются снова. */
    suspend fun checkAllAvailability(): Int = refreshAvailability(emptySet())

    suspend fun markUnavailable(songId: Long) = songDao.setAvailable(songId, false)

    suspend fun setNote(songId: Long, note: String?) =
        songDao.setNote(songId, note?.trim()?.takeIf { it.isNotEmpty() })

    /** true — песня новая. Существующей обновляются только метаданные; заметка сохраняется. */
    private suspend fun upsert(scanned: MusicScanner.ScannedSong): Boolean {
        val existing = songDao.findByUri(scanned.uri)
        if (existing != null) {
            songDao.updateMetadata(
                uri = scanned.uri,
                title = scanned.title,
                artist = scanned.artist,
                album = scanned.album,
                durationMs = scanned.durationMs,
                albumArtUri = existing.albumArtUri,
            )
            return false
        }
        // Повторное добавление той же композиции другим путём не создаёт дубликат.
        val duplicate = songDao.findDuplicate(scanned.title, scanned.artist, scanned.durationMs)
        if (duplicate != null) {
            if (duplicate.isAvailable) return false
            // Файл переехал: старая запись (с заметкой и плейлистами) привязывается к новому месту.
            songDao.relink(duplicate.id, scanned.uri, scanned.title, scanned.artist, scanned.album, scanned.durationMs)
            return true
        }

        val cover = scanner.extractCover(Uri.parse(scanned.uri))
        val id = songDao.insert(
            SongEntity(
                uri = scanned.uri,
                title = scanned.title,
                artist = scanned.artist,
                album = scanned.album,
                durationMs = scanned.durationMs,
                albumArtUri = cover,
            )
        )
        return id != -1L
    }

    /** Проверяет файлы, не найденные при сканировании; возвращает, сколько стали недоступны. */
    private suspend fun refreshAvailability(found: Set<String>): Int {
        var newlyUnavailable = 0
        for (song in songDao.getAll()) {
            if (song.uri in found) continue
            val accessible = scanner.isAccessible(Uri.parse(song.uri))
            if (accessible != song.isAvailable) songDao.setAvailable(song.id, accessible)
            if (!accessible && song.isAvailable) newlyUnavailable++
        }
        return newlyUnavailable
    }
}
