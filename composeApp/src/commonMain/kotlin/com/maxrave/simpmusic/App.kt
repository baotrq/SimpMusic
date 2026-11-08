package com.maxrave.simpmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.Orientation
import com.maxrave.simpmusic.expect.currentOrientation
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.rememberDesktopModeScale
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.playbacksync.SettingsSync
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.AppNavigationRail
import com.maxrave.simpmusic.ui.component.DesktopSidebar
import com.maxrave.simpmusic.ui.component.LiquidGlassAppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.InWindowNowPlayingSheet
import com.maxrave.simpmusic.ui.component.MiniPlayerAnchor
import com.maxrave.simpmusic.ui.component.LocalAppDialogOpen
import com.maxrave.simpmusic.ui.component.LocalNowPlayingOpen
import com.maxrave.simpmusic.ui.component.LocalUserStarted
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.WrappedDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.navigation.graph.AppNavigationGraph
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.screen.other.UnofficialBuildScreen
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.desktopWindowDark
import com.maxrave.simpmusic.ui.theme.desktopWindowLight
import com.maxrave.simpmusic.ui.theme.fontFamily
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.utils.isAppLinkAvailable
import com.maxrave.simpmusic.utils.libraryTabOf
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.ShellWindowInsets
import com.maxrave.simpmusic.expect.ui.SyncWindowTitleBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import com.maxrave.simpmusic.ui.screen.player.content.DESKTOP_PLAYER_CLOSED
import com.maxrave.simpmusic.ui.screen.player.content.DESKTOP_PLAYER_FULL
import com.maxrave.simpmusic.ui.screen.player.content.DESKTOP_PLAYER_MOTION_MS
import com.maxrave.simpmusic.ui.screen.player.content.DESKTOP_PLAYER_PANEL
import com.maxrave.simpmusic.ui.screen.player.content.DesktopCoverFlight
import com.maxrave.simpmusic.ui.screen.player.content.DesktopPlayerMotion
import com.maxrave.simpmusic.ui.screen.player.content.DesktopPlayerTab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.github
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.notification
import simpmusic.composeapp.generated.resources.settings
import simpmusic.composeapp.generated.resources.sleep_timer_off
import simpmusic.composeapp.generated.resources.this_app_needs_to_access_your_notification
import simpmusic.composeapp.generated.resources.this_link_is_not_supported
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.update_available
import simpmusic.composeapp.generated.resources.update_message
import simpmusic.composeapp.generated.resources.yes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: SharedViewModel = koinInject(),
    showDesktopNotificationPermissionDialog: Boolean = false,
    onDismissDesktopNotificationPermissionDialog: (doNotShowAgain: Boolean) -> Unit = {},
    onOpenDesktopNotificationSettings: (doNotShowAgain: Boolean) -> Unit = {},
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val navController = rememberNavController()
    // Fork: an Android window wide enough for the Windows layout, which is a tablet held sideways. The
    // capsule needs the width: below this a portrait tablet keeps the rail and the in-window player.
    val isTabletShell =
        getPlatform() == Platform.Android && windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isDesktopShell = getPlatform() == Platform.Desktop || isTabletShell
    val latestIsDesktopShell by rememberUpdatedState(isDesktopShell)

    // Fork: pick up AI and translation settings changed on another device, each time the app opens.
    val appLifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(appLifecycle) {
        appLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { SettingsSync.pull() }
    }

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val updateData by viewModel.updateResponse.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()
    val showNotificationPermissionDialog by viewModel.showNotificationPermissionDialog.collectAsStateWithLifecycle()

    val isLiquidGlassEnabled by viewModel.getEnableLiquidGlass().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    // Analytics only makes sense with local tracking on, so its tab follows that setting.
    val isLocalTrackingEnabled by viewModel.getLocalTrackingEnabled().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val showAnalyticsTab = isLocalTrackingEnabled == TRUE
    // Mix for you comes from the signed-in YouTube account, so its tab follows the session — the
    // same condition that used to hide the chip inside Library.
    val isYouTubeLoggedIn by viewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val showMixForYouTab = isYouTubeLoggedIn == TRUE

    val themeMode by viewModel.getThemeMode().collectAsStateWithLifecycle(DataStoreManager.THEME_MODE_DARK)
    val isOfficialBuild by viewModel.isOfficialBuild.collectAsStateWithLifecycle()
    // MiniPlayer visibility: derived, never stored.
    //
    // This used to be a rememberSaveable Boolean written by a LaunchedEffect. Two things went
    // wrong with that. The effect only runs AFTER the first composition, so the first frame drew
    // whatever the initial value said — and rememberSaveable RESTORES a previously saved value,
    // so flipping that initial value from true to false changed nothing on a process that had
    // already saved true. The bar therefore showed, hid, and showed again on every start.
    //
    // Reading it straight from nowPlayingData removes both failure modes: there is no first-frame
    // guess to be wrong, and no saved copy to disagree with the source.
    val isShowMiniPlayer by remember {
        derivedStateOf {
            val item = nowPlayingData?.mediaItem
            item != null && item != GenericMediaItem.EMPTY
        }
    }

    // Now playing screen
    var isShowNowPlaylistScreen by rememberSaveable {
        mutableStateOf(false)
    }

    // Fork: Desktop's player has three sizes: closed, the small panel beside the content (just
    // isShowNowPlaylistScreen), and this full-size page over the content.
    var isDesktopPlayerFull by rememberSaveable {
        mutableStateOf(false)
    }

    // Fullscreen
    var isInFullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isNavBarVisible by rememberSaveable {
        mutableStateOf(true)
    }

    var shouldShowUpdateDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Every dialog this file raises on its own. Home's launch banner waits until none is up; see
    // LocalAppDialogOpen. (The banner waits for a running update check by itself.)
    val appDialogOpen =
        shouldShowUpdateDialog || showNotificationPermissionDialog ||
            showDesktopNotificationPermissionDialog || sleepTimerState.isDone

    // Set by the first press or wheel scroll anywhere in the app this launch, or by opening Now
    // Playing. A launch banner that has not had its turn yet then waits for a later launch.
    var userStarted by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(isShowNowPlaylistScreen) {
        if (isShowNowPlaylistScreen) userStarted = true
    }
    // Fork: tapping a song anywhere opens Now Playing straight away. Desktop opens it at full size, and
    // by itself only when nothing was playing: starting a song while one plays leaves the user on the
    // screen they are browsing.
    LaunchedEffect(Unit) {
        var playbackStoppedAt = 0L
        launch {
            viewModel.controllerState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .drop(1)
                .collect { playing -> if (!playing) playbackStoppedAt = System.currentTimeMillis() }
        }
        BaseViewModel.nowPlayingOpenRequests.collect { tappedLoadedSong ->
            // Loading a new queue stops the old song first, so a song that stopped a moment ago still
            // counts as playing.
            val nothingPlaying =
                !viewModel.controllerState.value.isPlaying &&
                    System.currentTimeMillis() - playbackStoppedAt > DESKTOP_RECENTLY_PLAYING_MS
            if (!latestIsDesktopShell) {
                isShowNowPlaylistScreen = true
            } else if (tappedLoadedSong || nothingPlaying) {
                isShowNowPlaylistScreen = true
                isDesktopPlayerFull = true
            }
        }
    }

    val hazeState =
        rememberHazeState()

    LaunchedEffect(intent) {
        val intent = intent ?: return@LaunchedEffect
        val data = intent.data
        Logger.d("MainActivity", "onCreate: $data")
        if (data != null) {
            if (data.scheme == "simpmusic" && data.host == "notification") {
                viewModel.setIntent(null)
                navController.navigate(
                    NotificationDestination,
                )
            } else if (data.scheme == "wordbyword" && data.host == "lastfm-auth") {
                // Last.fm sends the user back here after they approve access, carrying the request
                // token: wordbyword://lastfm-auth?token=xxx. The callback is fixed on the API
                // account, which is why the scheme is not "simpmusic".
                val token = data.getQueryParameter("token")
                Logger.d("MainActivity", "Last.fm callback, token present: ${!token.isNullOrEmpty()}")
                viewModel.setIntent(null)
                // Deliberately no navigation: the login screen is almost certainly already open —
                // the browser was opened from it — and navigating would stack a second copy on top
                // of it. The token is handed straight to the shared view model, and the screen
                // closes itself when it sees a session key appear.
                token?.let { viewModel.completeLastfmLogin(it) }
            } else if (data.host == "simpmusic.org" || data.scheme == "simpmusic") {
                // https://simpmusic.org/app/watch?v=VIDEO_ID
                // https://simpmusic.org/app/playlist?list=PLAYLIST_ID
                // https://simpmusic.org/app/channel/CHANNEL_ID
                // simpmusic://watch?v=VIDEO_ID  (host="watch", no path)
                // simpmusic://playlist?list=PLAYLIST_ID
                // simpmusic://channel/CHANNEL_ID
                val segments = data.pathSegments
                // For simpmusic.org: segments = ["app", "watch"] → appPath = segments[1]
                // For simpmusic://: host IS the appPath (e.g. host="watch"), segments = []
                val appPath =
                    if (data.scheme == "simpmusic") {
                        data.host
                    } else {
                        segments.getOrNull(1)
                    }
                Logger.d("MainActivity", "simpmusic.org deep link, appPath: $appPath")
                // A path added to the when below must also go into APP_LINK_PATHS in AppLinks.kt.
                // Some screens exist only with local tracking on or while signed in to YouTube; see
                // isAppLinkAvailable. Read before setIntent(null): clearing the intent changes this
                // effect's key, and the next frame cancels the effect wherever it is suspended, which
                // would lose a navigation waiting on these reads.
                val localTrackingOn = viewModel.getLocalTrackingEnabled().first() == TRUE
                val youTubeLoggedIn = viewModel.getYouTubeLoggedIn().first() == TRUE
                viewModel.setIntent(null)
                when (appPath) {
                    "watch" -> {
                        data.getQueryParameter("v")?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }

                    "playlist" -> {
                        data.getQueryParameter("list")?.let { playlistId ->
                            if (playlistId.startsWith("OLAK5uy_")) {
                                navController.navigate(AlbumDestination(browseId = playlistId))
                            } else if (playlistId.startsWith("VL")) {
                                navController.navigate(PlaylistDestination(playlistId = playlistId))
                            } else {
                                navController.navigate(PlaylistDestination(playlistId = "VL$playlistId"))
                            }
                        }
                    }

                    "channel", "c" -> {
                        // simpmusic://channel/UCxxx → segments = ["UCxxx"]
                        // simpmusic.org/app/channel/UCxxx → segments = ["app", "channel", "UCxxx"]
                        val artistId =
                            if (data.scheme == "simpmusic") {
                                segments.firstOrNull()
                            } else {
                                segments.getOrNull(2)
                            }
                        artistId?.let {
                            if (it.startsWith("UC")) {
                                navController.navigate(ArtistDestination(channelId = it))
                            } else {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            }
                        }
                    }

                    "album" -> {
                        data.getQueryParameter("id")?.let { albumId ->
                            navController.navigate(AlbumDestination(browseId = albumId))
                        }
                    }

                    // simpmusic://library                     → the Library tab
                    // simpmusic://library?type=favorite       → one of its collections
                    // Added for the Playlists widget, whose shortcuts have to reach these
                    // screens from the home screen without the app already running.
                    "library" -> {
                        val type = data.getQueryParameter("type")
                        if (type.isNullOrBlank()) {
                            // simpmusic://library?tab=wrapped → the Library tab on one of its chips,
                            // named as LibraryChipType stores them (a launch banner points at Wrapped).
                            val tab = libraryTabOf(data)
                            if (!isAppLinkAvailable(appPath, tab, localTrackingOn, youTubeLoggedIn)) {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            } else {
                                viewModel.requestLibraryTab(tab)
                                navController.navigate(LibraryDestination)
                            }
                        } else {
                            navController.navigate(LibraryDynamicPlaylistDestination(type = type))
                        }
                    }

                    // simpmusic://analytics — added for the launch banners.
                    "analytics" -> {
                        if (isAppLinkAvailable(appPath, null, localTrackingOn, youTubeLoggedIn)) {
                            navController.navigate(AnalyticsDestination)
                        } else {
                            viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                        }
                    }

                    // simpmusic://taste — the AI taste card, which sits on Your library. A link of its
                    // own rather than library?tab=your_library, so it can say that the card exists only
                    // while local tracking is on.
                    "taste" -> {
                        if (isAppLinkAvailable(appPath, null, localTrackingOn, youTubeLoggedIn)) {
                            viewModel.requestLibraryTab(LibraryChipType.YOUR_LIBRARY)
                            navController.navigate(LibraryDestination)
                        } else {
                            viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                        }
                    }

                    // simpmusic://recognize — the Quick Settings tile (Android only): Search with the
                    // song recognition sheet open.
                    "recognize" -> {
                        if (isAppLinkAvailable(appPath, null, localTrackingOn, youTubeLoggedIn)) {
                            viewModel.requestSongRecognition(true)
                            // Single top: already on Search, that screen opens the sheet. A second
                            // copy would race the one leaving for the request.
                            navController.navigate(SearchDestination) { launchSingleTop = true }
                        } else {
                            viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                        }
                    }

                    else -> {
                        viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                    }
                }
            } else {
                Logger.d("MainActivity", "onCreate: $data")
                when (val path = data.pathSegments.firstOrNull()) {
                    "playlist" -> {
                        data
                            .getQueryParameter("list")
                            ?.let { playlistId ->
                                viewModel.setIntent(null)
                                if (playlistId.startsWith("OLAK5uy_")) {
                                    navController.navigate(
                                        AlbumDestination(
                                            browseId = playlistId,
                                        ),
                                    )
                                } else if (playlistId.startsWith("VL")) {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = playlistId,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = "VL$playlistId",
                                        ),
                                    )
                                }
                            }
                    }

                    "channel", "c" -> {
                        data.lastPathSegment?.let { artistId ->
                            if (artistId.startsWith("UC")) {
                                viewModel.setIntent(null)
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = artistId,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(
                                    getString(
                                        Res.string.this_link_is_not_supported,
                                    ),
                                )
                            }
                        }
                    }

                    else -> {
                        when {
                            path == "watch" -> data.getQueryParameter("v")
                            data.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(updateData) {
        val response = updateData ?: return@LaunchedEffect
        // Only a NEWER release is offered. This used to compare the tags for inequality, which told a
        // build ahead of the latest release (dev, beta) to "update" back down to it, and told every
        // F-Droid-channel user to update on each launch: that channel's tag carries no leading "v".
        if (viewModel.showedUpdateDialog && VersionManager.isBehind(response.tagName)) {
            shouldShowUpdateDialog = true
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        Logger.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
        if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
            isShowNowPlaylistScreen = false
        }
        // The full-size player covers the content, so a new screen would open out of sight under it.
        isDesktopPlayerFull = false
        // Wrapped counts as fullscreen for the same reason the video player does: it is a
        // full-bleed reel, and the rail and the mini player would sit on top of the card the user
        // is meant to be reading — and on top of every card captured as a share image.
        isInFullscreen = navBackStackEntry?.destination?.hierarchy?.any {
            it.hasRoute(FullscreenDestination::class) || it.hasRoute(WrappedDestination::class)
        } == true
    }
    LaunchedEffect(showAnalyticsTab) {
        // Turning tracking off removes the Analytics tab, so leaving the user standing on it would
        // strand them on a screen no tab points at anymore.
        if (!showAnalyticsTab &&
            navBackStackEntry?.destination?.hierarchy?.any {
                it.hasRoute(AnalyticsDestination::class)
            } == true
        ) {
            navController.navigate(HomeDestination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    LaunchedEffect(showMixForYouTab) {
        // Same for signing out of YouTube: the Mix for you tab goes away, so nobody may be left
        // standing on a screen that has no mixes to show and no tab pointing at it.
        if (!showMixForYouTab &&
            navBackStackEntry?.destination?.hierarchy?.any {
                it.hasRoute(MixForYouDestination::class)
            } == true
        ) {
            navController.navigate(HomeDestination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    var isScrolledToTop by rememberSaveable {
        mutableStateOf(false)
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE
    // Fork: Samsung DeX (see rememberDesktopModeScale), where the player is hosted in-window.
    val inDesktopMode = rememberDesktopModeScale() > 1f
    // A wide window (Desktop, an unfolded Fold, a tablet, DeX) navigates with the side rail and has no
    // bottom bar. Bar phones never get here: they stay in portrait (applyPhoneOrientationLock).
    val useNavigationRail = isTablet

    AppTheme(
        themeMode = themeMode,
        // The setting governs Desktop too, the same as Android: off, every glass surface draws flat.
        liquidGlassEnabled = isLiquidGlassEnabled == TRUE,
        desktopShell = isDesktopShell,
    ) {
        if (!isOfficialBuild) {
            UnofficialBuildScreen()
            return@AppTheme
        }
        // Read inside AppTheme so MaterialTheme reflects the resolved scheme.
        val isLightScheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

        // The desktop shell is a window colour with panels floating on it. The two schemes mirror
        // each other: the window takes the extreme (pure black / pure white) and the panel steps
        // one shade back towards the middle, so the panels read as raised either way.
        val desktopWindow = if (isLightScheme) desktopWindowLight else desktopWindowDark
        val desktopPanel =
            if (isLightScheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        SyncWindowTitleBar(color = desktopWindow, isDark = !isLightScheme)
        // Fullscreen video and Wrapped run edge to edge, as they do on a phone.
        ShellWindowInsets(enabled = isTabletShell && !isInFullscreen, color = desktopWindow)

        // Backdrop base must match the page, so glass over an empty area reads as the page itself.
        // On Desktop the page is the panel: its colour is painted outside the recorded layer, and
        // colorScheme.background carries the seed's hue, which the glass's saturation boost brings out.
        val backdrop = rememberBackdrop(if (isDesktopShell) desktopPanel else MaterialTheme.colorScheme.background)
        val desktopPlayerFull = isDesktopShell && isDesktopPlayerFull && isShowMiniPlayer && !isInFullscreen
        // Every animation of Desktop's player runs off this one position (see DesktopPlayerMotion).
        val desktopPlayerTarget =
            when {
                !isDesktopShell || !isShowNowPlaylistScreen || !isShowMiniPlayer || isInFullscreen -> DESKTOP_PLAYER_CLOSED
                isDesktopPlayerFull -> DESKTOP_PLAYER_FULL
                else -> DESKTOP_PLAYER_PANEL
            }
        LaunchedEffect(desktopPlayerTarget) {
            DesktopPlayerMotion.target = desktopPlayerTarget
            // One frame first, for the layer being moved to to compose and report where it lands.
            withFrameNanos { }
            DesktopPlayerMotion.position.animateTo(
                desktopPlayerTarget.toFloat(),
                tween(DESKTOP_PLAYER_MOTION_MS, easing = FastOutSlowInEasing),
            )
        }
        val desktopPlayerComposed by remember {
            derivedStateOf { DesktopPlayerMotion.target != DESKTOP_PLAYER_CLOSED || DesktopPlayerMotion.position.value > 0f }
        }
        Scaffold(
            // Watches for userStarted without consuming anything. Dialogs and sheets are windows or
            // layers of their own, so a press inside one (closing the update dialog, say) does not count.
            modifier =
                Modifier
                    // Fork: nothing under the in-window player is drawn while it covers the window.
                    .graphicsLayer { alpha = if (MiniPlayerAnchor.coversApp) 0f else 1f }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (!userStarted) {
                                val type = awaitPointerEvent(PointerEventPass.Initial).type
                                if (type == PointerEventType.Press || type == PointerEventType.Scroll) {
                                    userStarted = true
                                }
                            }
                        }
                    },
            containerColor =
                if (isDesktopShell) desktopWindow else MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail) {
                    AnimatedVisibility(
                        // Fork: stays up under the player (drawn in-window, on top of it), so the player
                        // shrinks back onto a bar already in place instead of the bar popping in after.
                        isNavBarVisible,
                        enter = if (getPlatform() == Platform.Android) EnterTransition.None else fadeIn() + slideInHorizontally(),
                        exit = if (getPlatform() == Platform.Android) ExitTransition.None else fadeOut(),
                    ) {
                        // On Android both materials go through the folding bar, which draws the mini
                        // player itself. Desktop keeps the always-open flat bar under its own one.
                        val useFoldingBar = getPlatform() == Platform.Android || isLiquidGlassEnabled == TRUE
                        Column {
                            AnimatedVisibility(
                                isShowMiniPlayer && !useFoldingBar && isLiquidGlassEnabled == DataStoreManager.FALSE,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                MiniPlayer(
                                    Modifier
                                        // 56dp card + the 4dp gap below.
                                        .height(60.dp)
                                        .fillMaxWidth()
                                        .padding(
                                            // The bottom bar's own 16dp, so both edges line up.
                                            horizontal = 16.dp,
                                        ).padding(
                                            bottom = 4.dp,
                                        ),
                                    backdrop = backdrop,
                                    navController = navController,
                                    onClick = {
                                        isShowNowPlaylistScreen = true
                                    },
                                    onClose = {
                                        viewModel.stopPlayer()
                                        viewModel.isServiceRunning = false
                                    },
                                )
                            }
                            if (useFoldingBar) {
                                LiquidGlassAppBottomNavigationBar(
                                    navController = navController,
                                    backdrop = backdrop,
                                    viewModel = viewModel,
                                    onOpenNowPlaying = { isShowNowPlaylistScreen = true },
                                    isScrolledToTop = isScrolledToTop,
                                    showAnalyticsTab = showAnalyticsTab,
                                    showMixForYouTab = showMixForYouTab,
                                    liquidGlass = isLiquidGlassEnabled == TRUE,
                                    // Kept under the in-window player too; only the tablet's side
                                    // panel replaces it.
                                    showMiniPlayer = !(isShowNowPlaylistScreen && isTabletLandscape),
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }
                            } else {
                                AppBottomNavigationBar(
                                    navController = navController,
                                    showAnalyticsTab = showAnalyticsTab,
                                    showMixForYouTab = showMixForYouTab,
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }
                            }
                        }
                    }
                }
            },
            content = { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            // Never true alongside the tablet source further down, which sits inside this Box: one
                            // backdrop nested in itself throws "Recording currently in progress" on the first frame.
                            if (isLiquidGlassEnabled == TRUE && !useNavigationRail) {
                                Modifier.layerBackdrop(backdrop)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    val tabletMiniPlayer: @Composable (Modifier) -> Unit = { modifier ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isShowMiniPlayer && isTablet && !isInFullscreen,
                            // Keeps to the centre of what the panel leaves free, and moves with it.
                            modifier =
                                modifier.graphicsLayer {
                                    if (isDesktopShell) translationX = -DesktopPlayerMotion.capsuleShiftPx(this)
                                },
                            enter = fadeIn() + slideInHorizontally(),
                            exit = fadeOut(),
                        ) {
                            MiniPlayer(
                                if (!isDesktopShell) {
                                    Modifier
                                        // Glass keeps its 52dp card; the flat one is 56dp.
                                        .height(if (isLiquidGlassEnabled == TRUE) 56.dp else 60.dp)
                                        .fillMaxWidth(0.8f)
                                        .padding(
                                            horizontal = 12.dp,
                                        ).padding(
                                            bottom = 4.dp,
                                        )
                                } else {
                                    // Floating capsule, Apple Music style: a fixed size so it never
                                    // stretches to the window width, and no haze at all — the capsule
                                    // paints its own liquid glass. Layering haze underneath blurs the
                                    // same pixels twice and reads as a dark smear, not glass.
                                    // padding BEFORE height: the other way round the bottom margin
                                    // eats into the 72dp and the capsule ends up 52dp tall.
                                    // 60dp is the floor for this layout: the content row is centred
                                    // on the capsule's axis and the 16dp progress box hangs off the
                                    // bottom, so the height has to cover the taller of the artwork
                                    // (32dp) and the two text lines (~33dp), plus that 16dp, plus a
                                    // gap. Going lower means shrinking the artwork again.
                                    Modifier
                                        .wrapContentWidth()
                                        .padding(bottom = 20.dp)
                                        .height(60.dp)
                                },
                                backdrop = backdrop,
                                navController = navController,
                                // A small tablet has no room for the full capsule beside the open panel.
                                capsuleTrackWidth =
                                    if (isTabletShell && !windowSize.isWidthAtLeastBreakpoint(TABLET_FULL_CAPSULE_MIN_WIDTH_DP)) {
                                        160.dp
                                    } else {
                                        300.dp
                                    },
                                onClick = {
                                    // On Desktop this always lands on the small panel.
                                    isDesktopPlayerFull = false
                                    isShowNowPlaylistScreen = true
                                },
                                onClose = {
                                    viewModel.stopPlayer()
                                    viewModel.isServiceRunning = false
                                    if (isDesktopShell) {
                                        isDesktopPlayerFull = false
                                        isShowNowPlaylistScreen = false
                                    }
                                },
                                // The page lives in the Now Playing panel, so the panel opens with it.
                                onOpenFullscreenLyrics = {
                                    if (isDesktopShell) {
                                        // A tab of the full-size player there.
                                        viewModel.setLastPlayerViewTab(DesktopPlayerTab.LYRICS.name)
                                        isDesktopPlayerFull = true
                                    } else {
                                        viewModel.requestFullscreenLyrics()
                                    }
                                    isShowNowPlaylistScreen = true
                                },
                                // At full size the queue is a tab beside the capsule; otherwise its own sheet.
                                onOpenQueue =
                                    if (desktopPlayerFull) {
                                        { viewModel.setLastPlayerViewTab(DesktopPlayerTab.QUEUE.name) }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxSize(),
                    ) {
                        // Everything left of Now Playing. On Desktop the capsule player sits on the centre of
                        // this whole area, sidebar included, so it lives here rather than in the content panel,
                        // which is both narrower than the capsule and clipped; and the area is what its glass
                        // records, so the part over the sidebar refracts the sidebar instead of nothing.
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .then(
                                        // The Desktop capsule player's glass refracts this. Off, the capsule
                                        // draws flat and nothing reads the recording, so skip it.
                                        if (isLiquidGlassEnabled == TRUE && isDesktopShell && isTablet && !isInFullscreen) {
                                            Modifier.layerBackdrop(backdrop)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    // Narrower by the panel's room only where the player covers the
                                    // difference, so the screen beside it never visibly reflows.
                                    .layout { measurable, constraints ->
                                        val width =
                                            if (isDesktopShell && DesktopPlayerMotion.contentNarrow()) {
                                                constraints.maxWidth - DesktopPlayerMotion.panelSlotPx(constraints.maxWidth, this)
                                            } else {
                                                constraints.maxWidth
                                            }
                                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                                    },
                            ) {
                                if (isTablet && !isInFullscreen) {
                                    if (isDesktopShell) {
                                        DesktopSidebar(
                                            navController = navController,
                                            containerColor = desktopPanel,
                                            showAnalyticsTab = showAnalyticsTab,
                                            showMixForYouTab = showMixForYouTab,
                                            // No end padding: the content panel's own 8dp is the gap between the two.
                                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                                            playerOpen = desktopPlayerFull,
                                            onClosePlayer = { isDesktopPlayerFull = false },
                                        ) { klass ->
                                            viewModel.reloadDestination(klass)
                                        }
                                    } else {
                                        AppNavigationRail(
                                            navController = navController,
                                            showAnalyticsTab = showAnalyticsTab,
                                            showMixForYouTab = showMixForYouTab,
                                        ) { klass ->
                                            viewModel.reloadDestination(klass)
                                        }
                                    }
                                }
                                // Desktop only: the content sits in its own rounded panel floating on a
                                // pure black window, Spotify style, while the rail stays flat black
                                // outside it. Phones keep one continuous surface — the inset only reads
                                // as deliberate when there is a window frame around it.
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .then(
                                            if (isDesktopShell) {
                                                Modifier
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(desktopPanel)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            // Stays composed under the full-size player, so the screen is
                                            // where the user left it, and costs nothing to draw there.
                                            .graphicsLayer { alpha = if (isDesktopShell && DesktopPlayerMotion.contentCovered()) 0f else 1f }
                                            .then(
                                                // Tablets record the capsule's source here; Desktop records it on
                                                // the Box above, which takes in the sidebar as well.
                                                if (isLiquidGlassEnabled == TRUE && !isDesktopShell && isTablet && !isInFullscreen) {
                                                    Modifier.layerBackdrop(backdrop)
                                                } else {
                                                    Modifier
                                                },
                                            ).hazeSource(hazeState),
                                    ) {
                                        CompositionLocalProvider(
                                            LocalAppDialogOpen provides appDialogOpen,
                                            LocalNowPlayingOpen provides isShowNowPlaylistScreen,
                                            LocalUserStarted provides userStarted,
                                        ) {
                                            AppNavigationGraph(
                                                innerPadding = innerPadding,
                                                navController = navController,
                                                hideNavBar = {
                                                    isNavBarVisible = false
                                                },
                                                showNavBar = {
                                                    isNavBarVisible = true
                                                },
                                                showNowPlayingSheet = {
                                                    isShowNowPlaylistScreen = true
                                                },
                                                onScrolling = {
                                                    isScrolledToTop = it
                                                },
                                            )
                                        }
                                    }
                                    if (!isDesktopShell) {
                                        tabletMiniPlayer(Modifier.padding(innerPadding).align(Alignment.BottomCenter))
                                    }
                                }
                            }
                            // The player's one surface: above the strip's row and the content, under the
                            // capsule, and inside what the capsule's glass records.
                            if (isDesktopShell && desktopPlayerComposed) {
                                // Tablets have a back button: full size to panel, panel to closed. Declared
                                // ahead of the player, so the player's own back handling still goes first.
                                PlatformBackHandler(enabled = isTabletShell && isShowNowPlaylistScreen) {
                                    if (isDesktopPlayerFull) isDesktopPlayerFull = false else isShowNowPlaylistScreen = false
                                }
                                ForceDarkContent {
                                    NowPlayingScreenContent(
                                        navController = navController,
                                        sharedViewModel = viewModel,
                                        isExpanded = true,
                                        dismissIcon = SimpIcons.ArrowForwardIos,
                                        onDesktopExpand = { isDesktopPlayerFull = true },
                                    ) {
                                        // The page's > collapses to the panel; the panel's closes it.
                                        if (isDesktopPlayerFull) isDesktopPlayerFull = false else isShowNowPlaylistScreen = false
                                    }
                                }
                                DesktopCoverFlight(sharedViewModel = viewModel)
                            }
                            }
                            if (isDesktopShell) {
                                tabletMiniPlayer(Modifier.padding(innerPadding).align(Alignment.BottomCenter))
                            }
                        }
                        // Desktop's player is the surface above; this panel is the tablet's.
                        if (isTablet && isTabletLandscape && !isInFullscreen && !isDesktopShell) {
                            AnimatedVisibility(
                                isShowNowPlaylistScreen,
                                enter = expandHorizontally() + fadeIn(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxHeight()
                                        // The width Now Playing has always had: `fillMaxWidth(0.35f)` of what the
                                        // 80dp NavigationRail left in this Row. The rail and the sidebar now sit
                                        // inside the weighted Box, so that share is taken by hand from the Row's
                                        // own width; otherwise it would follow the sidebar. On Desktop it is 8dp
                                        // less, the spacer it no longer has, so the card keeps its exact size.
                                        .layout { measurable, constraints ->
                                            val width =
                                                (
                                                    ((constraints.maxWidth - 80.dp.roundToPx()) * 0.35f).roundToInt() -
                                                        if (isDesktopShell) 8.dp.roundToPx() else 0
                                                ).coerceIn(0, constraints.maxWidth)
                                            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                                            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                                        },
                                ) {
                                    // On Desktop the content panel's own 8dp end padding is already the gap,
                                    // the same 8dp it keeps from the sidebar; a spacer here doubled it.
                                    if (!isDesktopShell) Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .padding(
                                                innerPadding.copy(
                                                    start = 0.dp,
                                                    top = 0.dp,
                                                    bottom = 0.dp,
                                                ),
                                            ).then(
                                                // Matches the inset of the content panel so the two
                                                // read as a pair of floating cards, not one panel
                                                // with a seam down the middle.
                                                if (isDesktopShell) {
                                                    Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
                                                } else {
                                                    Modifier
                                                },
                                            ).clip(
                                                RoundedCornerShape(12.dp),
                                            ).then(
                                                if (isDesktopShell) {
                                                    Modifier.background(desktopPanel)
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    ) {
                                        ForceDarkContent {
                                            NowPlayingScreenContent(
                                                navController = navController,
                                                sharedViewModel = viewModel,
                                                isExpanded = true,
                                                dismissIcon = SimpIcons.ArrowForwardIos,
                                            ) {
                                                isShowNowPlaylistScreen = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (sleepTimerState.isDone) {
                    Logger.w("MainActivity", "Sleep Timer Done: $sleepTimerState")
                    AlertDialog(
                        properties =
                            DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                            ),
                        onDismissRequest = {
                            viewModel.stopSleepTimer()
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.stopSleepTimer()
                            }) {
                                Text(
                                    stringResource(Res.string.yes),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        text = {
                            Text(
                                stringResource(Res.string.sleep_timer_off),
                                style = typo().labelSmall,
                            )
                        },
                        title = {
                            Text(
                                stringResource(Res.string.good_night),
                                style = typo().bodySmall,
                            )
                        },
                    )
                }

                if (shouldShowUpdateDialog) {
                    val response = updateData ?: return@Scaffold
                    AlertDialog(
                        properties =
                            DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                            ),
                        onDismissRequest = {
                            shouldShowUpdateDialog = false
                            viewModel.showedUpdateDialog = false
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    shouldShowUpdateDialog = false
                                    viewModel.showedUpdateDialog = false
                                    // Fork: never install — just open upstream's release notes to see what to merge.
                                    openUrl("https://github.com/maxrave-dev/SimpMusic/releases/tag/${response.tagName}")
                                },
                            ) {
                                Text(
                                    stringResource(Res.string.github),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    shouldShowUpdateDialog = false
                                    viewModel.showedUpdateDialog = false
                                },
                            ) {
                                Text(
                                    stringResource(Res.string.cancel),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        title = {
                            Text(
                                stringResource(Res.string.update_available),
                                style = typo().labelSmall,
                            )
                        },
                        text = {
                            val formatted =
                                response.releaseTime?.let { input ->
                                    try {
                                        val instant = kotlin.time.Instant.parse(input)
                                        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                        dateTime.format(
                                            LocalDateTime.Format {
                                                day()
                                                char(' ')
                                                monthName(MonthNames.ENGLISH_ABBREVIATED)
                                                char(' ')
                                                year()
                                                char(' ')
                                                hour()
                                                char(':')
                                                minute()
                                                char(':')
                                                second()
                                            },
                                        )
                                    } catch (e: Exception) {
                                        stringResource(Res.string.unknown)
                                    }
                                } ?: stringResource(Res.string.unknown)

                            val updateMessage =
                                runBlocking {
                                    getString(
                                        Res.string.update_message,
                                        response.tagName,
                                        formatted,
                                        // values-iw/values-in still carry an old %3$s; Compose Resources indexes
                                        // args without a bounds check, so omitting it crashes the dialog
                                        "",
                                    )
                                }
                            Column(
                                Modifier
                                    .heightIn(
                                        max = 400.dp,
                                    ).verticalScroll(
                                        rememberScrollState(),
                                    ),
                            ) {
                                Text(
                                    text = updateMessage,
                                    style = typo().labelMedium,
                                    modifier =
                                        Modifier.padding(
                                            vertical = 8.dp,
                                        ),
                                )
                                Markdown(
                                    response.body,
                                    typography =
                                        markdownTypography(
                                            h1 = typo().labelLarge,
                                            h2 = typo().labelMedium,
                                            h3 = typo().labelSmall,
                                            text = typo().bodySmall,
                                            bullet = typo().bodySmall,
                                            paragraph = typo().bodySmall,
                                            textLink =
                                                TextLinkStyles(
                                                    SpanStyle(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        fontFamily = fontFamily(),
                                                        textDecoration = TextDecoration.Underline,
                                                    ),
                                                ),
                                        ),
                                )
                            }
                        },
                    )
                }

                if (showNotificationPermissionDialog || showDesktopNotificationPermissionDialog) {
                    var doNotShowAgain by remember { mutableStateOf(false) }
                    val dismissPermissionDialog = {
                        if (showDesktopNotificationPermissionDialog) {
                            onDismissDesktopNotificationPermissionDialog(doNotShowAgain)
                        } else {
                            viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                        }
                    }
                    AlertDialog(
                        onDismissRequest = {
                            dismissPermissionDialog()
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (showDesktopNotificationPermissionDialog) {
                                        onOpenDesktopNotificationSettings(doNotShowAgain)
                                    } else {
                                        viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                                    }
                                },
                            ) {
                                Text(
                                    stringResource(
                                        if (showDesktopNotificationPermissionDialog) {
                                            Res.string.settings
                                        } else {
                                            Res.string.yes
                                        },
                                    ),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        dismissButton =
                            if (showDesktopNotificationPermissionDialog) {
                                {
                                    TextButton(onClick = dismissPermissionDialog) {
                                        Text(
                                            stringResource(Res.string.cancel),
                                            style = typo().bodySmall,
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        title = {
                            Text(
                                stringResource(Res.string.notification),
                                style = typo().labelSmall,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    stringResource(Res.string.this_app_needs_to_access_your_notification),
                                    style = typo().bodySmall,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .clickable { doNotShowAgain = !doNotShowAgain }
                                            .fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = doNotShowAgain,
                                        onCheckedChange = { doNotShowAgain = it },
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        stringResource(Res.string.do_not_show_again),
                                        style = typo().bodySmall,
                                    )
                                }
                            }
                        },
                    )
                }
            },
        )
        // Fork: outside the Scaffold so the player draws above its bottom bar, which stays
        // composed underneath (see the bottomBar block).
        // Fork: in Samsung DeX the player is drawn inside the app's own window, sliding up
        // over it. The phone's bottom sheet is a separate window, and in DeX that window
        // covered the app window's title bar — its X and minimise buttons with it.
        // Phones use it too: creating the sheet's window cost ~100ms before anything moved.
        if ((inDesktopMode || getPlatform() == Platform.Android) && !isTabletLandscape && !isTabletShell) {
            // Also composed while a finger is on the mini player, built ahead of the tap, and while a
            // drag let go short of opening sinks back into the bar (see MiniPlayerAnchor).
            if (isShowNowPlaylistScreen || MiniPlayerAnchor.pressed || MiniPlayerAnchor.returning) {
                InWindowNowPlayingSheet(
                    onDismiss = { isShowNowPlaylistScreen = false },
                    open = isShowNowPlaylistScreen,
                    backdrop = backdrop.takeIf { isLiquidGlassEnabled == TRUE },
                ) { dismiss ->
                    ForceDarkContent {
                        Box(Modifier.fillMaxSize().background(Color.Black)) {
                            // The player takes 5-6 frames to build. Start the slide on the
                            // black page and build it two frames later, mid-slide, so a tap
                            // answers at once.
                            var showContent by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                repeat(2) { withFrameNanos { } }
                                showContent = true
                            }
                            if (showContent) {
                                NowPlayingScreenContent(
                                    navController = navController,
                                    sharedViewModel = viewModel,
                                    isExpanded = true,
                                    dismissIcon = SimpIcons.KeyboardArrowDown,
                                ) {
                                    dismiss()
                                }
                            }
                        }
                    }
                }
            }
        } else if (isShowNowPlaylistScreen && !isTabletLandscape && !isTabletShell) {
            ForceDarkContent {
                NowPlayingScreen(
                    navController = navController,
                ) {
                    isShowNowPlaylistScreen = false
                }
            }
        }
    }
}

private const val DESKTOP_RECENTLY_PLAYING_MS = 3_000L

// Where the 724dp capsule (the 300dp track, no Desktop-only button) fits beside the open player panel.
private const val TABLET_FULL_CAPSULE_MIN_WIDTH_DP = 1100
