package com.example.personalmusicplayer.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.ui.components.Cover
import com.example.personalmusicplayer.ui.notes.NoteEditDialog
import com.example.personalmusicplayer.ui.notes.NotesViewModel
import com.example.personalmusicplayer.ui.playlists.AddToPlaylistsDialog
import com.example.personalmusicplayer.ui.playlists.PlaylistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    playlistsViewModel: PlaylistsViewModel,
    notesViewModel: NotesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle(initialValue = 0L)

    // После закрытия плеера (крестик) очередь пуста — возвращаемся на вкладки.
    LaunchedEffect(state.hasMedia) { if (!state.hasMedia) onBack() }

    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val shown = (dragging ?: position.toFloat()).coerceIn(0f, duration.toFloat())
    val sliderInteraction = remember { MutableInteractionSource() }
    var addToPlaylist by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }

    val currentId = state.songId
    if (addToPlaylist && currentId != null) {
        AddToPlaylistsDialog(
            songId = currentId,
            viewModel = playlistsViewModel,
            onDismiss = { addToPlaylist = false },
        )
    }
    if (editingNote && currentId != null) {
        val existing = state.note.orEmpty()
        NoteEditDialog(
            initialNote = existing,
            onSave = { notesViewModel.setNote(currentId, it); editingNote = false },
            onDelete = if (existing.isNotBlank()) {
                { notesViewModel.setNote(currentId, null); editingNote = false }
            } else {
                null
            },
            onDismiss = { editingNote = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_close))
            }
            IconButton(onClick = viewModel::close) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.player_stop))
            }
        }
        Spacer(Modifier.height(8.dp))
        Cover(
            artUri = state.artworkUri,
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f),
            cornerRadius = 24.dp,
            iconSize = 72.dp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = state.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.artist ?: stringResource(R.string.library_unknown_artist),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        if (!state.note.isNullOrBlank()) {
            // Заметка: нажатие или карандаш открывает редактирование (там же — удаление).
            Row(
                modifier = Modifier.fillMaxWidth().clickable { editingNote = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.note.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 48.dp),
                )
                IconButton(onClick = { editingNote = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.note_edit))
                }
            }
        } else {
            TextButton(onClick = { editingNote = true }, enabled = state.songId != null) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.note_add))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Ползунок вдвое тоньше стандартного: дорожка 16 → 8 dp, ручка 4×44 → 2×22 dp.
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { viewModel.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration.toFloat(),
            interactionSource = sliderInteraction,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteraction,
                    thumbSize = DpSize(2.dp, 22.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(8.dp))
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val repeatOff = state.repeatMode == Player.REPEAT_MODE_OFF
            IconButton(onClick = viewModel::cycleRepeat) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = stringResource(
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
                            Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
                            else -> R.string.player_repeat_off
                        }
                    ),
                    tint = if (repeatOff) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = viewModel::previous, modifier = Modifier.size(56.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.player_previous),
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledIconButton(onClick = viewModel::togglePlay, modifier = Modifier.size(72.dp)) {
                Crossfade(targetState = state.isPlaying, label = "playPause") { playing ->
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (playing) R.string.player_pause else R.string.player_play
                        ),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            IconButton(onClick = viewModel::next, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.player_next),
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { addToPlaylist = true }, enabled = state.songId != null) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.add_to_playlist_button),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
