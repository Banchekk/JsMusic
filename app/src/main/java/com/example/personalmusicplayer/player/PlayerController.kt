package com.example.personalmusicplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow

data class PlayerState(
    val hasMedia: Boolean = false,
    val songId: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val note: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
)

/** Мост между интерфейсом и PlaybackService (через MediaController). Главный поток. */
class PlayerController(private val context: Context) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var consecutiveErrors = 0

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val errors = Channel<Long>(Channel.BUFFERED)
    /** id песен, которые не удалось воспроизвести (файл удалён, повреждён). */
    val playbackErrors: Flow<Long> = errors.receiveAsFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) consecutiveErrors = 0
            publish(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError()
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            Runnable {
                val c = try {
                    f.get()
                } catch (e: Exception) {
                    return@Runnable
                }
                controller = c
                c.addListener(listener)
                publish(c)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int) {
        val c = controller ?: return
        consecutiveErrors = 0
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        val active = c.playWhenReady &&
            c.playbackState != Player.STATE_ENDED && c.playbackState != Player.STATE_IDLE
        if (active) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }

    /** Сразу предыдущая песня; сама логика — в PlaybackService (ForwardingPlayer). */
    fun previous() { controller?.seekToPrevious() }

    /** Закрыть плеер: остановить воспроизведение и очистить очередь (мини-плеер скрывается). */
    fun close() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun positionFlow(): Flow<Long> = flow {
        while (true) {
            emit(controller?.currentPosition ?: 0L)
            delay(250)
        }
    }

    private fun publish(p: Player) {
        val item = p.currentMediaItem
        val meta = item?.mediaMetadata
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: meta?.durationMs ?: 0L
        _state.value = _state.value.copy(
            hasMedia = item != null,
            songId = item?.mediaId?.toLongOrNull(),
            title = meta?.title?.toString(),
            artist = meta?.artist?.toString(),
            artworkUri = meta?.artworkUri?.toString(),
            isPlaying = p.playWhenReady &&
                (p.playbackState == Player.STATE_READY || p.playbackState == Player.STATE_BUFFERING),
            durationMs = duration,
            repeatMode = p.repeatMode,
            hasNext = p.hasNextMediaItem(),
        )
    }

    /** Битый или пропавший файл: сообщаем наверх и переходим к следующему, без бесконечного цикла. */
    private fun handleError() {
        val c = controller ?: return
        c.currentMediaItem?.mediaId?.toLongOrNull()?.let { errors.trySend(it) }
        consecutiveErrors++
        if (consecutiveErrors < c.mediaItemCount && c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            c.prepare()
            c.play()
        } else {
            c.stop()
        }
    }
}
