package com.example.personalmusicplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.data.repository.PlaylistSummary
import com.example.personalmusicplayer.ui.components.PlaylistCover
import com.example.personalmusicplayer.ui.components.SearchField
import com.example.personalmusicplayer.ui.components.countTextStyle
import com.example.personalmusicplayer.ui.components.songsCountText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Поиск по плейлистам появляется, когда их больше этого числа. */
private const val SEARCH_MIN_PLAYLISTS = 4

@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel, onOpenPlaylist: (Long) -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Локальная копия порядка: на время перетаскивания список не перезаписывается из базы.
    val ordered = remember { mutableStateListOf<PlaylistSummary>() }
    val gridState = rememberLazyGridState()
    val reorder = remember(gridState) {
        GridReorderState(
            gridState = gridState,
            onMove = { from, to -> ordered.add(to, ordered.removeAt(from)) },
            onDragEnd = { viewModel.reorderPlaylists(ordered.map { it.playlist.id }) },
        )
    }
    LaunchedEffect(playlists) {
        val list = playlists
        if (list != null && reorder.draggedKey == null) {
            ordered.clear()
            ordered.addAll(list)
        }
    }

    // Автопрокрутка, когда плейлист тянут к краю экрана.
    LaunchedEffect(reorder.draggedKey) {
        val key = reorder.draggedKey ?: return@LaunchedEffect
        while (isActive) {
            val layout = gridState.layoutInfo
            val info = layout.visibleItemsInfo.firstOrNull { it.key == key }
            if (info != null) {
                val top = info.offset.y + reorder.dragOffset.y
                val bottom = top + info.size.height
                val step = when {
                    top < layout.viewportStartOffset + 32 -> -16f
                    bottom > layout.viewportEndOffset - 32 -> 16f
                    else -> 0f
                }
                if (step != 0f) reorder.dragBy(Offset(0f, gridState.scrollBy(step)))
            }
            delay(16)
        }
    }

    val canSearch = (playlists?.size ?: 0) > SEARCH_MIN_PLAYLISTS
    LaunchedEffect(canSearch) {
        if (!canSearch) {
            searching = false
            query = ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (searching) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.search_hint_playlists),
                onClose = { searching = false; query = "" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.playlists_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (canSearch) {
                    IconButton(onClick = { searching = true }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
                    }
                }
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_create))
                }
            }
        }

        val list = playlists
        val q = query.trim()
        val filtering = searching && q.isNotEmpty()
        val shown = if (filtering) {
            ordered.filter { it.playlist.name.contains(q, ignoreCase = true) }
        } else {
            ordered
        }

        when {
            list == null -> Box(Modifier.fillMaxSize())
            list.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.playlists_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.playlists_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { creating = true }) { Text(stringResource(R.string.playlist_create)) }
            }
            filtering && shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.TopCenter) {
                Text(
                    stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(shown, key = { it.playlist.id }) { summary ->
                    val id = summary.playlist.id
                    val dragged = reorder.draggedKey == id
                    PlaylistCard(
                        summary = summary,
                        onClick = { onOpenPlaylist(id) },
                        modifier = if (dragged) {
                            Modifier.zIndex(1f).graphicsLayer {
                                translationX = reorder.dragOffset.x
                                translationY = reorder.dragOffset.y
                                scaleX = 1.04f
                                scaleY = 1.04f
                            }
                        } else {
                            Modifier.animateItem()
                        },
                        // Долгое нажатие + перетаскивание меняет порядок (при поиске отключено).
                        dragModifier = Modifier.pointerInput(id, filtering) {
                            if (filtering) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { reorder.start(id) },
                                onDragEnd = { reorder.end() },
                                onDragCancel = { reorder.end() },
                            ) { change, drag ->
                                change.consume()
                                reorder.dragBy(drag)
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            titleRes = R.string.playlist_create,
            confirmRes = R.string.common_create,
            initialName = "",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                scope.launch { viewModel.createPlaylist(name) }
            },
        )
    }
}

@Composable
private fun PlaylistCard(
    summary: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(dragModifier)
    ) {
        PlaylistCover(
            coverUri = summary.playlist.coverUri,
            arts = summary.previewArts,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            iconSize = 48.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            summary.playlist.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            songsCountText(summary.songCount),
            style = countTextStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
        Spacer(Modifier.height(2.dp))
    }
}
