package com.example.personalmusicplayer.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.ui.playlists.AddToPlaylistsDialog
import com.example.personalmusicplayer.ui.playlists.PlaylistsViewModel

private enum class Step { MENU, PLAYLISTS, NOTE }

/** Действия над песней: добавить в плейлисты, добавить/изменить/удалить заметку. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActions(
    song: SongEntity,
    playlistsViewModel: PlaylistsViewModel,
    notesViewModel: NotesViewModel,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.MENU) }
    val hasNote = !song.note.isNullOrBlank()

    when (step) {
        Step.MENU -> ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                ActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, R.string.add_to_playlist_title) {
                    step = Step.PLAYLISTS
                }
                ActionRow(
                    icon = Icons.Filled.Edit,
                    labelRes = if (hasNote) R.string.note_edit else R.string.note_add,
                ) { step = Step.NOTE }
                if (hasNote) {
                    ActionRow(Icons.Filled.Delete, R.string.note_delete) {
                        notesViewModel.setNote(song.id, null)
                        onDismiss()
                    }
                }
            }
        }
        Step.PLAYLISTS -> AddToPlaylistsDialog(song.id, playlistsViewModel, onDismiss)
        Step.NOTE -> NoteEditDialog(
            initialNote = song.note.orEmpty(),
            onSave = { notesViewModel.setNote(song.id, it); onDismiss() },
            onDelete = if (hasNote) ({ notesViewModel.setNote(song.id, null); onDismiss() }) else null,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}
