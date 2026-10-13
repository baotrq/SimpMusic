package com.maxrave.simpmusic.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.WrappedDestination
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.home.HomeScreen
import com.maxrave.simpmusic.ui.screen.home.analytics.AnalyticsScreen
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedScreen
import com.maxrave.simpmusic.ui.screen.library.LibraryScreen
import com.maxrave.simpmusic.ui.screen.library.MixForYouScreen
import com.maxrave.simpmusic.ui.screen.other.SearchScreen
import com.maxrave.simpmusic.ui.screen.player.FullscreenPlayer

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        // Fork: a carousel. Between tabs the page comes in from the side of the tab being moved to
        // (by tab order); elsewhere opening a page brings it in from the right and going back
        // takes it out to the right. Everything used to slide in from the left, both ways.
        enterTransition = {
            fadeIn(initialAlpha = SLIDE_FADE) + slideInHorizontally { it * slideDirection(pop = false) }
        },
        exitTransition = {
            fadeOut(targetAlpha = SLIDE_FADE) + slideOutHorizontally { -it * slideDirection(pop = false) }
        },
        popEnterTransition = {
            fadeIn(initialAlpha = SLIDE_FADE) + slideInHorizontally { it * slideDirection(pop = true) }
        },
        popExitTransition = {
            fadeOut(targetAlpha = SLIDE_FADE) + slideOutHorizontally { -it * slideDirection(pop = true) }
        },
    ) {
        // Bottom bar destinations
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
            )
        }
        composable<LibraryDestination> {
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        // Only reachable as a tab while signed in to YouTube
        composable<MixForYouDestination> {
            MixForYouScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        // Only reachable as a tab while local tracking is enabled.
        // ForceDarkContent for the same reason as album/playlist/artist: the page background comes
        // from the artwork via toImmersiveBackground(), which always lands dark, so the light
        // theme's dark-on-light text and icons would be unreadable on it.
        composable<AnalyticsDestination> {
            ForceDarkContent {
                AnalyticsScreen(
                    navController = navController,
                    innerPadding = innerPadding,
                )
            }
        }
        // Reached only from the Analytics screen's entry banner, so it inherits that screen's
        // gate on local tracking. ForceDarkContent for a different reason than Analytics: the reel
        // is drawn on its own near-black ground whatever the user's theme is, because it is an
        // event and because every card is also a share image that has to survive leaving the app.
        composable<WrappedDestination> {
            ForceDarkContent {
                WrappedScreen(
                    navController = navController,
                    hideNavBar = hideNavBar,
                    // Deliberately not the fullscreen player's `showNavBar(true)` + open sheet:
                    // leaving the reel goes back to Analytics, and raising the Now Playing sheet
                    // over it would be a screen the user never asked for.
                    showNavBar = { showNavBar(false) },
                )
            }
        }
        composable<FullscreenDestination> {
            ForceDarkContent {
                FullscreenPlayer(
                    navController,
                    hideNavBar = hideNavBar,
                    showNavBar = {
                        showNavBar.invoke(true)
                        showNowPlayingSheet.invoke()
                    },
                )
            }
        }
        // Home screen graph
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Library screen graph
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // List screen graph
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Login screen graph
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}

// How faded a page is at the far end of its slide: a light fade, so the edge reads softly.
private const val SLIDE_FADE = 0.5f

// Position of a destination in the tab bar, or null for a page that is not a tab.
private fun NavDestination.tabIndex(): Int? =
    when {
        hasRoute(HomeDestination::class) -> 0
        hasRoute(MixForYouDestination::class) -> 1
        hasRoute(AnalyticsDestination::class) -> 2
        hasRoute(LibraryDestination::class) -> 3
        hasRoute(SearchDestination::class) -> 4
        else -> null
    }

// 1 when the incoming page arrives from the right, -1 from the left.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideDirection(pop: Boolean): Int {
    val from = initialState.destination.tabIndex()
    val to = targetState.destination.tabIndex()
    if (from != null && to != null && from != to) return if (to > from) 1 else -1
    return if (pop) -1 else 1
}
