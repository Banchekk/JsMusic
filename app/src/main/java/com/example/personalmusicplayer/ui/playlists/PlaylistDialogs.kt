package com.example.personalmusicplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.ui.components.countTextStyle
import com.example.personalmusicplayer.ui.components.songsCountText
import kotlinx.coroutines.launch

private const val MAX_NAME_LENGTH = 60

/** Диалог с полем названия: создание и переименование плейлиста. */
@Composable
fun PlaylistNameDialog(
    titleRes: Int,
    confirmRes: Int,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/** Одна песня — в один или несколько плейлистов сразу (галочки), с созданием нового. */
@Composable
fun AddToPlaylistsDialog(
    songId: Long,
    viewModel: PlaylistsViewModel,
    onDismiss: () -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf<Set<Long>?>(null) } // null — идёт загрузка
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(songId) { checked = viewModel.playlistIdsOf(songId) }

    val list = playlists.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist_title)) },
        text = {
            if (list.isEmpty()) {
                Text(stringResource(R.string.add_to_playlist_none))
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(list, key = { it.playlist.id }) { item ->
                        val id = item.playlist.id
                        val isChecked = checked?.contains(id) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = checked != null) {
                                    val current = checked ?: emptySet()
                                    checked = if (isChecked) current - id else current + id
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    item.playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    songsCountText(item.songCount),
                                    style = countTextStyle(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = checked != null,
                onClick = {
                    viewModel.setMemberships(songId, checked ?: emptySet())
                    onDismiss()
                },
            ) { Text(stringResource(R.string.common_done)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { creating = true }) { Text(stringResource(R.string.playlist_create)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        },
    )

    if (creating) {
        PlaylistNameDialog(
            titleRes = R.string.playlist_create,
            confirmRes = R.string.common_create,
            initialName = "",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                scope.launch {
                    val id = viewModel.createPlaylist(name)
                    checked = (checked ?: emptySet()) + id
                }
            },
        )
    }
}
