package com.example.personalmusicplayer.ui.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.ui.components.PlaylistCover
import com.example.personalmusicplayer.ui.components.SearchField
import com.example.personalmusicplayer.ui.components.SongCard
import com.example.personalmusicplayer.ui.components.matchesQuery
import com.example.personalmusicplayer.ui.components.songsCountText
import com.example.personalmusicplayer.ui.notes.NotesViewModel
import com.example.personalmusicplayer.ui.notes.SongActions
import com.example.personalmusicplayer.ui.player.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistsViewModel,
    playerViewModel: PlayerViewModel,
    notesViewModel: NotesViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
) {
    val summaries by viewModel.playlists.collectAsStateWithLifecycle()
    val summary = summaries?.firstOrNull { it.playlist.id == playlistId }
    val dbSongs by remember(playlistId) { viewModel.songsOf(playlistId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val librarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()

    // Плейлист удалён — возвращаемся к списку.
    LaunchedEffect(summaries, summary) {
        if (summaries != null && summary == null) onBack()
    }

    // Локальная копия порядка: на время перетаскивания список не перезаписывается из базы.
    val items = remember { mutableStateListOf<SongEntity>() }
    val listState = rememberLazyListState()
    val reorder = remember(listState) {
        ReorderState(
            listState = listState,
            onMove = { from, to -> items.add(to, items.removeAt(from)) },
            onDragEnd = { viewModel.reorder(playlistId, items.map { it.id }) },
        )
    }
    LaunchedEffect(dbSongs) {
        val songs = dbSongs
        if (songs != null && reorder.draggedKey == null) {
            items.clear()
            items.addAll(songs)
        }
    }

    // Автопрокрутка, когда песню тянут к краю экрана.
    LaunchedEffect(reorder.draggedKey) {
        val key = reorder.draggedKey ?: return@LaunchedEffect
        while (isActive) {
            val layout = listState.layoutInfo
            val info = layout.visibleItemsInfo.firstOrNull { it.key == key }
            if (info != null) {
                val top = info.offset + reorder.dragOffset
                val bottom = top + info.size
                val step = when {
                    top < layout.viewportStartOffset + 32 -> -16f
                    bottom > layout.viewportEndOffset - 32 -> 16f
                    else -> 0f
                }
                if (step != 0f) reorder.dragBy(listState.scrollBy(step))
            }
            delay(16)
        }
    }

    var editing by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var coverDialog by remember { mutableStateOf(false) }
    var addingSongs by remember { mutableStateOf(false) }
    var actionsSong by remember { mutableStateOf<SongEntity?>(null) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCover(playlistId, uri)
    }

    val songsInDb = dbSongs
    val editMode = editing && !songsInDb.isNullOrEmpty()

    Column(Modifier.fillMaxSize()) {
        // Верхняя строка: назад, режим изменения, меню
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Box(Modifier.weight(1f))
            if (!songsInDb.isNullOrEmpty()) {
                IconButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editMode) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = stringResource(
                            if (editMode) R.string.playlist_edit_done else R.string.playlist_edit
                        ),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.common_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_add_songs)) },
                        onClick = { menuOpen = false; addingSongs = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_rename)) },
                        onClick = { menuOpen = false; renaming = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_cover)) },
                        onClick = { menuOpen = false; coverDialog = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.playlist_delete)) },
                        onClick = { menuOpen = false; deleting = true },
                    )
                }
            }
        }

        // Шапка: обложка, название, количество песен
        if (summary != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PlaylistCover(
                    coverUri = summary.playlist.coverUri,
                    arts = summary.previewArts,
                    modifier = Modifier.size(104.dp),
                    iconSize = 40.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        summary.playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        songsCountText(summary.songCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when {
            songsInDb == null -> Box(Modifier.fillMaxSize())
            songsInDb.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.playlist_empty),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { addingSongs = true }) {
                    Text(stringResource(R.string.playlist_add_songs))
                }
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { song ->
                    val dragged = reorder.draggedKey == song.id
                    val onCardClick: (() -> Unit)? =
                        if (editMode) null else ({ playerViewModel.playFromPlaylist(playlistId, song.id) })
                    Row(
                        modifier = if (dragged) {
                            Modifier.zIndex(1f).graphicsLayer { translationY = reorder.dragOffset }
                        } else {
                            Modifier.animateItem()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (editMode) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .pointerInput(song.id) {
                                        detectDragGestures(
                                            onDragStart = { reorder.start(song.id) },
                                            onDragEnd = { reorder.end() },
                                            onDragCancel = { reorder.end() },
                                        ) { change, drag ->
                                            change.consume()
                                            reorder.dragBy(drag.y)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = stringResource(R.string.playlist_drag),
                                )
                            }
                        }
                        SongCard(
                            song = song,
                            modifier = Modifier.weight(1f),
                            isCurrent = song.id == currentSongId,
                            onClick = onCardClick,
                            onLongClick = if (editMode) null else ({ actionsSong = song }),
                        )
                        if (editMode) {
                            IconButton(
                                onClick = { viewModel.removeSong(playlistId, song.id) },
                                modifier = Modifier.size(40.dp).alpha(if (dragged) 0.5f else 1f),
                            ) {
                                Icon(
                                    Icons.Filled.RemoveCircleOutline,
                                    contentDescription = stringResource(R.string.playlist_remove),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Диалоги ----
    actionsSong?.let { song ->
        SongActions(
            song = song,
            playlistsViewModel = viewModel,
            notesViewModel = notesViewModel,
            onDismiss = { actionsSong = null },
        )
    }
    if (renaming && summary != null) {
        PlaylistNameDialog(
            titleRes = R.string.playlist_rename,
            confirmRes = R.string.common_save,
            initialName = summary.playlist.name,
            onDismiss = { renaming = false },
            onConfirm = { name -> renaming = false; viewModel.rename(playlistId, name) },
        )
    }
    if (deleting && summary != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.playlist_delete)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm, summary.playlist.name)) },
            confirmButton = {
                TextButton(onClick = { deleting = false; viewModel.delete(playlistId) }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
    if (coverDialog) {
        AlertDialog(
            onDismissRequest = { coverDialog = false },
            title = { Text(stringResource(R.string.playlist_cover_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = {
                        coverDialog = false
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.playlist_cover_gallery)) }
                    TextButton(onClick = {
                        coverDialog = false
                        viewModel.resetCover(playlistId)
                    }) { Text(stringResource(R.string.playlist_cover_auto)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { coverDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
    if (addingSongs) {
        val inPlaylist = songsInDb.orEmpty().map { it.id }.toSet()
        AddSongsSheet(
            candidates = librarySongs.filter { it.id !in inPlaylist },
            onAdd = { ids -> addingSongs = false; viewModel.addSongs(playlistId, ids) },
            onDismiss = { addingSongs = false },
        )
    }
}

/** Выбор песен библиотеки для добавления в плейлист (карточки с рамкой у выбранных) и поиск. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsSheet(
    candidates: List<SongEntity>,
    onAdd: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val shown = if (searching && q.isNotEmpty()) candidates.filter { it.matchesQuery(q) } else candidates

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxHeight(0.85f).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.playlist_add_songs),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!searching && candidates.isNotEmpty()) {
                    IconButton(onClick = { searching = true }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
                    }
                }
                Button(
                    enabled = selected.isNotEmpty(),
                    onClick = { onAdd(candidates.filter { it.id in selected }.map { it.id }) },
                ) { Text(stringResource(R.string.playlist_add_songs_count, selected.size)) }
            }
            if (searching) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.search_hint_songs),
                    onClose = { searching = false; query = "" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            when {
                candidates.isEmpty() -> Text(
                    stringResource(R.string.playlist_add_songs_none),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shown.isEmpty() -> Text(
                    stringResource(R.string.search_no_results),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.id }) { song ->
                        SongCard(
                            song = song,
                            isCurrent = song.id in selected,
                            onClick = {
                                selected = if (song.id in selected) selected - song.id else selected + song.id
                            },
                        )
                    }
                }
            }
        }
    }
}
