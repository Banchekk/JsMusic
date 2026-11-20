package com.example.personalmusicplayer.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.personalmusicplayer.PersonalMusicApp
import com.example.personalmusicplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Владелец ExoPlayer и MediaSession (служба переднего плана). Музыка играет при свёрнутом
 * приложении, заблокированном и выключенном экране. Системное уведомление с обложкой,
 * экран блокировки, наушники и Bluetooth управляют воспроизведением через MediaSession.
 * Аудиофокус (звонки, другие приложения) и отключение наушников обрабатывает ExoPlayer.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: PlaybackStateStore

    /** Пока играет музыка, раз в 5 секунд запоминаем позицию — для восстановления после перезапуска. */
    private val positionSaver = object : Runnable {
        override fun run() {
            exoPlayer?.let { p ->
                if (p.isPlaying) {
                    store.savePosition(p.currentMediaItem?.mediaId?.toLongOrNull(), p.currentPosition)
                }
            }
            handler.postDelayed(this, POSITION_SAVE_INTERVAL_MS)
        }
    }

    private val stateSaver = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                )
            ) {
                saveQueue(player)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                handler.removeCallbacks(positionSaver)
                if (player.isPlaying) handler.postDelayed(positionSaver, POSITION_SAVE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = PlaybackStateStore(this)

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Процессор не засыпает при выключенном экране.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer = exo
        exo.addListener(stateSaver)

        // «Предыдущая» сразу включает предыдущую песню (в приложении, уведомлении, на Bluetooth).
        // На первой песне (без повтора списка) трек начинается заново.
        val player = object : ForwardingPlayer(exo) {
            override fun seekToPrevious() {
                if (hasPreviousMediaItem()) seekToPreviousMediaItem() else seekTo(0L)
            }
        }

        val builder = MediaSession.Builder(this, player)
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            builder.setSessionActivity(
                PendingIntent.getActivity(
                    this, 0, launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        }
        session = builder.build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.notification_channel_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(notificationProvider)

        restoreQueue(exo)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Приложение смахнули из списка: если музыка не играет, службу можно остановить. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        exoPlayer?.let { exo ->
            saveQueue(exo)
            exo.removeListener(stateSaver)
        }
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        exoPlayer = null
        super.onDestroy()
    }

    private fun saveQueue(player: Player) {
        if (player.mediaItemCount == 0) {
            store.clear()
            return
        }
        val ids = (0 until player.mediaItemCount).mapNotNull {
            player.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        store.saveQueue(
            songIds = ids,
            currentId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            positionMs = player.currentPosition,
            repeatMode = player.repeatMode,
        )
    }

    /**
     * Восстановление после перезапуска: та же очередь, песня и позиция, на паузе.
     * Недоступные (удалённые) песни из очереди пропускаются.
     */
    private fun restoreQueue(player: ExoPlayer) {
        val saved = store.load() ?: return
        val songDao = (application as PersonalMusicApp).database.songDao()
        scope.launch {
            val byId = songDao.getAll().filter { it.isAvailable }.associateBy { it.id }
            val songs = saved.songIds.mapNotNull { byId[it] }
            // Если пользователь уже запустил другую музыку, ничего не трогаем.
            if (songs.isEmpty() || player.mediaItemCount > 0) return@launch
            val startIndex = songs.indexOfFirst { it.id == saved.currentId }.coerceAtLeast(0)
            val position = if (songs[startIndex].id == saved.currentId) saved.positionMs else 0L
            player.setMediaItems(songs.map { it.toMediaItem() }, startIndex, position)
            player.repeatMode = saved.repeatMode
            player.prepare()
        }
    }

    private companion object {
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
