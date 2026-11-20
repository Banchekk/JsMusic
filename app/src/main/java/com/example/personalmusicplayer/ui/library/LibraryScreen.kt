package com.example.personalmusicplayer.ui.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.data.db.SongEntity
import com.example.personalmusicplayer.ui.components.SearchField
import com.example.personalmusicplayer.ui.components.SongCard
import com.example.personalmusicplayer.ui.components.matchesQuery
import com.example.personalmusicplayer.ui.notes.NotesViewModel
import com.example.personalmusicplayer.ui.notes.SongActions
import com.example.personalmusicplayer.ui.playlists.PlaylistsViewModel

private val AudioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, AudioPermission) == PackageManager.PERMISSION_GRANTED

@Composable
fun LibraryScreen(
    currentSongId: Long?,
    onPlaySong: (Long) -> Unit,
    playlistsViewModel: PlaylistsViewModel,
    notesViewModel: NotesViewModel,
    viewModel: LibraryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) viewModel.importFiles(uris) }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.importFolder(uri) }

    // Пользователь мог выдать разрешение в системных настройках и вернуться в приложение.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = hasAudioPermission(context) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.onPermissionGranted()
        } else if (!permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(AudioPermission)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(messageText(context, it)) }
    }

    // Библиотека опустела — поиск больше не нужен.
    val hasSongs = !songs.isNullOrEmpty()
    LaunchedEffect(hasSongs) {
        if (!hasSongs) {
            searching = false
            query = ""
        }
    }

    val pickFiles = { filesLauncher.launch(arrayOf("audio/*")) }
    val pickFolder = { folderLauncher.launch(null) }

    // Долгое нажатие на песню — меню: в плейлисты, заметка.
    var actionsSong by remember { mutableStateOf<SongEntity?>(null) }
    actionsSong?.let { song ->
        SongActions(
            song = song,
            playlistsViewModel = playlistsViewModel,
            notesViewModel = notesViewModel,
            onDismiss = { actionsSong = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (searching) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.search_hint_songs),
                    onClose = { searching = false; query = "" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                LibraryHeader(
                    enabled = !isScanning,
                    showSearch = hasSongs,
                    onSearch = { searching = true },
                    onRescan = { viewModel.rescan(hasPermission) },
                    onPickFiles = pickFiles,
                    onPickFolder = pickFolder,
                )
            }
            if (isScanning) LinearProgressIndicator(Modifier.fillMaxWidth())

            val list = songs
            if (list == null) {
                Box(Modifier.fillMaxSize())
            } else if (list.isEmpty()) {
                LibraryEmpty(
                    hasPermission = hasPermission,
                    isScanning = isScanning,
                    onGrant = { permissionLauncher.launch(AudioPermission) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                        )
                    },
                    onRescan = { viewModel.rescan(hasPermission) },
                    onPickFiles = pickFiles,
                    onPickFolder = pickFolder,
                )
            } else {
                val q = query.trim()
                val shown = if (searching && q.isNotEmpty()) list.filter { it.matchesQuery(q) } else list
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.TopCenter) {
                        Text(
                            stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { song ->
                            SongCard(
                                song = song,
                                isCurrent = song.id == currentSongId,
                                onClick = { onPlaySong(song.id) },
                                onLongClick = { actionsSong = song },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    enabled: Boolean,
    showSearch: Boolean,
    onSearch: () -> Unit,
    onRescan: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        if (showSearch) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
            }
        }
        IconButton(onClick = onRescan, enabled = enabled) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.library_rescan))
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.library_add))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_add_files)) },
                    onClick = { menuOpen = false; onPickFiles() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_add_folder)) },
                    onClick = { menuOpen = false; onPickFolder() },
                )
            }
        }
    }
}

@Composable
private fun LibraryEmpty(
    hasPermission: Boolean,
    isScanning: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onRescan: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            isScanning -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.library_scanning), style = MaterialTheme.typography.bodyLarge)
            }
            !hasPermission -> {
                Title(R.string.library_permission_title)
                Hint(R.string.library_permission_hint)
                Button(onClick = onGrant) { Text(stringResource(R.string.library_grant)) }
                OutlinedButton(onClick = onPickFiles) { Text(stringResource(R.string.library_add_files)) }
                OutlinedButton(onClick = onPickFolder) { Text(stringResource(R.string.library_add_folder)) }
                TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.library_open_settings)) }
            }
            else -> {
                Title(R.string.library_empty_title)
                Hint(R.string.library_empty_hint)
                Button(onClick = onRescan) { Text(stringResource(R.string.library_scan_now)) }
                OutlinedButton(onClick = onPickFiles) { Text(stringResource(R.string.library_add_files)) }
                OutlinedButton(onClick = onPickFolder) { Text(stringResource(R.string.library_add_folder)) }
            }
        }
    }
}

@Composable
private fun Title(res: Int) =
    Text(stringResource(res), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

@Composable
private fun Hint(res: Int) = Text(
    stringResource(res),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
)

private fun messageText(context: Context, message: LibraryMessage): String = when (message) {
    LibraryMessage.Failed -> context.getString(R.string.sync_failed)
    is LibraryMessage.Synced -> {
        val r = message.result
        val res = context.resources
        val parts = buildList {
            if (r.added > 0) add(res.getQuantityString(R.plurals.sync_added, r.added, r.added))
            if (r.unavailable > 0) add(res.getQuantityString(R.plurals.sync_unavailable, r.unavailable, r.unavailable))
            if (r.skipped > 0) add(context.getString(R.string.sync_skipped, r.skipped))
        }
        if (parts.isEmpty()) context.getString(R.string.sync_nothing_new) else parts.joinToString(". ")
    }
}
