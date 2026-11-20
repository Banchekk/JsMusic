package com.example.personalmusicplayer.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.ui.home.HomeScreen
import com.example.personalmusicplayer.ui.library.LibraryScreen
import com.example.personalmusicplayer.ui.notes.NotesViewModel
import com.example.personalmusicplayer.ui.player.MiniPlayer
import com.example.personalmusicplayer.ui.player.PlayerScreen
import com.example.personalmusicplayer.ui.player.PlayerViewModel
import com.example.personalmusicplayer.ui.playlists.PlaylistDetailScreen
import com.example.personalmusicplayer.ui.playlists.PlaylistsScreen
import com.example.personalmusicplayer.ui.playlists.PlaylistsViewModel
import com.example.personalmusicplayer.ui.settings.SettingsScreen
import com.example.personalmusicplayer.ui.settings.SettingsViewModel

private const val PLAYER_ROUTE = "player"
private const val PLAYLIST_PREFIX = "playlist/"
private const val PLAYLIST_ROUTE = "playlist/{playlistId}"

enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home("home", R.string.tab_home, Icons.Filled.Home),
    Library("library", R.string.tab_library, Icons.Filled.LibraryMusic),
    Playlists("playlists", R.string.tab_playlists, Icons.Filled.PlaylistPlay),
    Settings("settings", R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun AppRoot(
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel = viewModel(),
    playlistsViewModel: PlaylistsViewModel = viewModel(),
    notesViewModel: NotesViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val onPlayerScreen = currentRoute == PLAYER_ROUTE
    val inPlaylistDetail = currentRoute?.startsWith(PLAYLIST_PREFIX) == true
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    // Android 13+: без этого разрешения системное уведомление с управлением музыкой не показывается.
    // Спрашиваем один раз — когда плеер впервые появился.
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(playerState.hasMedia) {
        if (playerState.hasMedia &&
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            !settingsViewModel.isNotificationPermissionAsked()
        ) {
            settingsViewModel.markNotificationPermissionAsked()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = playerState.hasMedia && !onPlayerScreen,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                MiniPlayer(
                    state = playerState,
                    onOpen = { navController.navigate(PLAYER_ROUTE) { launchSingleTop = true } },
                    onTogglePlay = playerViewModel::togglePlay,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onClose = playerViewModel::close,
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !onPlayerScreen,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route ||
                                (tab == Tab.Playlists && inPlaylistDetail),
                            onClick = {
                                if (tab == Tab.Playlists && inPlaylistDetail) {
                                    // Нажатие на «Плейлисты» на странице плейлиста — к списку плейлистов.
                                    navController.popBackStack(Tab.Playlists.route, inclusive = false)
                                } else {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) { ScreenSurface { HomeScreen() } }
            composable(Tab.Library.route) {
                ScreenSurface {
                    LibraryScreen(
                        currentSongId = playerState.songId,
                        onPlaySong = playerViewModel::play,
                        playlistsViewModel = playlistsViewModel,
                        notesViewModel = notesViewModel,
                    )
                }
            }
            composable(Tab.Playlists.route) {
                ScreenSurface {
                    PlaylistsScreen(
                        viewModel = playlistsViewModel,
                        onOpenPlaylist = { id -> navController.navigate("$PLAYLIST_PREFIX$id") },
                    )
                }
            }
            composable(
                route = PLAYLIST_ROUTE,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("playlistId") ?: return@composable
                ScreenSurface {
                    PlaylistDetailScreen(
                        playlistId = id,
                        viewModel = playlistsViewModel,
                        playerViewModel = playerViewModel,
                        notesViewModel = notesViewModel,
                        currentSongId = playerState.songId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(Tab.Settings.route) { ScreenSurface { SettingsScreen(settingsViewModel) } }
            composable(
                route = PLAYER_ROUTE,
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) },
            ) {
                ScreenSurface {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        playlistsViewModel = playlistsViewModel,
                        notesViewModel = notesViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/**
 * Каждый экран — непрозрачная поверхность, которая перехватывает касания: нажатия по пустому
 * месту не «проваливаются» к экрану, оставшемуся под ним (например, во время смены вкладок).
 */
@Composable
private fun ScreenSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        content = content,
    )
}
