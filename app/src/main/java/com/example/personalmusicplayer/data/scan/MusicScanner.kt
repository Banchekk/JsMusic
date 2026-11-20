package com.example.personalmusicplayer.data.scan

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Поиск аудиофайлов (MediaStore, SAF) и чтение их метаданных. Ничего не пишет в базу. */
class MusicScanner(private val context: Context) {

    data class ScannedSong(
        val uri: String,
        val title: String,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
    )

    private val supportedMime = setOf(
        "audio/mpeg", "audio/mp3", "audio/mp4", "audio/x-m4a", "audio/m4a",
        "audio/mp4a-latm", "audio/wav", "audio/x-wav", "audio/vnd.wave", "audio/wave",
    )
    private val supportedExt = setOf("mp3", "m4a", "wav")

    fun isSupported(name: String?, mime: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        return (mime != null && mime.lowercase() in supportedMime) ||
            (ext != null && ext in supportedExt)
    }

    /** Все поддерживаемые аудиофайлы, которые видит MediaStore (нужно разрешение на чтение). */
    suspend fun scanMediaStore(): List<ScannedSong> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ScannedSong>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        // Рингтоны, уведомления и будильники в библиотеку не попадают.
        val selection = "COALESCE(${MediaStore.Audio.Media.IS_RINGTONE}, 0) = 0 AND " +
            "COALESCE(${MediaStore.Audio.Media.IS_NOTIFICATION}, 0) = 0 AND " +
            "COALESCE(${MediaStore.Audio.Media.IS_ALARM}, 0) = 0"

        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            while (c.moveToNext()) {
                val name = c.getString(nameCol)
                if (!isSupported(name, c.getString(mimeCol))) continue
                val uri = android.content.ContentUris.withAppendedId(collection, c.getLong(idCol))
                result += ScannedSong(
                    uri = uri.toString(),
                    title = clean(c.getString(titleCol)) ?: nameWithoutExt(name),
                    artist = clean(c.getString(artistCol)),
                    album = clean(c.getString(albumCol)),
                    durationMs = c.getLong(durCol),
                )
            }
        }
        result
    }

    /** Файл, выбранный через системный файловый менеджер. null — формат не поддерживается или файл нечитаем. */
    suspend fun readDocument(uri: Uri): ScannedSong? = withContext(Dispatchers.IO) {
        val name = queryDisplayName(uri)
        val mime = context.contentResolver.getType(uri)
        if (!isSupported(name, mime)) return@withContext null
        readMetadata(uri, name)
    }

    /** Рекурсивный обход папки, выбранной через SAF. */
    suspend fun scanTree(treeUri: Uri): List<ScannedSong> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val result = mutableListOf<ScannedSong>()
        val queue = ArrayDeque<DocumentFile>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            for (file in dir.listFiles()) {
                when {
                    file.isDirectory -> queue.add(file)
                    file.isFile && isSupported(file.name, file.type) ->
                        readMetadata(file.uri, file.name)?.let { result += it }
                }
            }
        }
        result
    }

    /** Сохраняет обложку из тегов файла в приватную папку приложения. null, если обложки нет. */
    suspend fun extractCover(uri: Uri): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return@withContext null
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val file = File(dir, sha1(uri.toString()) + ".jpg")
            if (!file.exists()) file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** Файл существует и доступен для чтения (не удалён, не перемещён, доступ не отозван). */
    suspend fun isAccessible(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** Чтобы доступ к выбранным вручную файлам сохранился после перезапуска приложения. */
    fun persistReadPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Не все провайдеры поддерживают постоянный доступ — это не ошибка.
        }
    }

    private fun readMetadata(uri: Uri, displayName: String?): ScannedSong? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            ScannedSong(
                uri = uri.toString(),
                title = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                    ?: nameWithoutExt(displayName),
                artist = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)),
                album = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
            )
        } catch (e: Exception) {
            null // повреждённый или недоступный файл
        } finally {
            retriever.release()
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private fun clean(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() && it != "<unknown>" }

    private fun nameWithoutExt(name: String?): String =
        name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "—"

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
