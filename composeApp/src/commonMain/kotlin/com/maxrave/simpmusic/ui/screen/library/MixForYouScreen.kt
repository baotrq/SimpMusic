package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.theme.LocalDesktopShell
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_mixes_found

/**
 * The YouTube "Mix for you" playlists, promoted out of the Library chip row into a tab of its own.
 *
 * It shares [LibraryViewModel] with the Library screen rather than owning a view model, so the
 * mixes stay fetched once and the grid, its pull-to-refresh and its empty state behave exactly as
 * they did while this was a chip.
 *
 * The tab is hidden while signed out (see `App.kt`), which is what the chip did too — YouTube has
 * no mixes to give an anonymous session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixForYouScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val mixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()
    // Home's rule, verbatim: transparent only while pixel-0 is on screen. onScrolling is too
    // coarse for this — it stays "on top" through the whole first row.
    val isAtTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }

    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    // What is painted behind this page: the desktop shell paints its rounded panel, not the background.
    val pageBackground =
        if (LocalDesktopShell.current) {
            if (MaterialTheme.colorScheme.background.luminance() > 0.5f) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            MaterialTheme.colorScheme.background
        }

    LaunchedEffect(Unit) {
        if (mixForYou.data.isNullOrEmpty()) {
            viewModel.getYouTubeMixedForYou()
        }
    }

    Box(Modifier.hazeSource(hazeState)) {
        GridLibraryPlaylist(
            navController,
            innerPadding.copy(top = topAppBarHeight),
            mixForYou,
            emptyText = Res.string.no_mixes_found,
            state = gridState,
            onScrolling = onScrolling,
        ) {
            viewModel.getYouTubeMixedForYou()
        }
    }
    // Transparent while the grid sits at pixel zero, frosted the moment it scrolls — the
    // same AnimatedContent crossfade Home and Search run on their bars.
    AnimatedContent(
        targetState = isAtTop,
        transitionSpec = {
            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
        },
    ) { atTop ->
        Column(
            Modifier
                .then(
                    if (atTop) {
                        Modifier.background(Color.Transparent)
                    } else {
                        // AlbumScreen's bar recipe, thinned to 0.3 — see SettingScreen.
                        Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(pageBackground, 0.3f))
                    },
                ).onGloballyPositioned { coordinates ->
                    topAppBarHeight = with(density) { coordinates.size.height.toDp() }
                },
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.mix_for_you),
                        style = typo().titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        }
    }
}