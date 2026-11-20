package com.example.personalmusicplayer.ui.notes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.example.personalmusicplayer.R

private const val MAX_NOTE_LENGTH = 500

/** Добавление и редактирование заметки; [onDelete] передаётся, только если заметка уже есть. */
@Composable
fun NoteEditDialog(
    initialNote: String,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialNote) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Поле ещё не готово — пользователь просто нажмёт на него.
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initialNote.isBlank()) R.string.note_add else R.string.note_edit))
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_NOTE_LENGTH) text = it },
                label = { Text(stringResource(R.string.note_hint)) },
                minLines = 3,
                maxLines = 8,
                supportingText = { Text("${text.length}/$MAX_NOTE_LENGTH") },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        },
    )
}
