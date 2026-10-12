package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.ui.component.AppleMusicLyricPaddingX
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.ai_translated
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.no_lyrics_for_this_song
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced

/**
 * The LYRICS body: compact header, the app's own [LyricsView] — the SAME renderer the other
 * styles' lyrics card embeds (owner's rule: reuse the existing component, never rebuild it) —
 * two floating action buttons, the sync-type and provider caption lines, and the shared bottom cluster.
 * The top-level composable falls back to MAIN when lyrics disappear mid-session.
 */
@Composable
internal fun AppleMusicLyricsView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    outputName: String?,
    onOpenOutput: () -> Unit,
    panelDrag: AppleMusicPanelDrag,
    modifier: Modifier = Modifier,
    // Fork: the shell has decided this song has no lyrics; say so until it slides back.
    showNoLyrics: Boolean = false,
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val localDensity = LocalDensity.current
    val lyricsData = state.screenData.lyricsData

    // LyricsView applies AppleMusicLyricPaddingX ITSELF, but only on its Apple Music branch — its
    // Classic branch renders LyricsLineItem, which has no horizontal padding of its own. So this
    // tab, which contributes no gutter, left Classic lyrics flush against the screen edge while
    // the header right above them sits at 20dp. Same split the fullscreen sheet already does:
    // the caller contributes whatever the renderer does not, so the TOTAL is the same either way.
    val lyricsStyle by dataStoreManager.lyricsStyle.collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val rendererOwnsGutter = lyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()

    // Fork: the big transport that a tap used to bring up over the lyrics is gone — the header's
    // play/pause and its sideways swipe cover it. A swipe down slides the page back to the player,
    // following the finger: the list's own pull past its top goes through the nested-scroll
    // connection, everywhere else (header, gutters) through the drag detector on the page.
    val pullDownToMain = rememberAppleMusicPullDownConnection(panelDrag)

    Column(modifier = modifier.fillMaxSize().appleMusicDirectionalDrag(enabled = true, up = false, drag = panelDrag)) {
        // Phones leave a small inset above the compact header; desktop keeps its panel controls.
        Spacer(
            modifier =
                Modifier.height(
                    with(localDensity) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(localDensity).toDp() } +
                        if (getPlatform() == Platform.Desktop) APPLE_MUSIC_TOP_BAR_HEIGHT else 8.dp,
                ),
        )
        AppleMusicCompactHeader(state = state, actions = actions, typography = typography, swipeToChangeTrack = true)

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .nestedScroll(pullDownToMain),
        ) {
            // Fork: a song change fades the old lyrics out and the new ones in, instead of the page
            // blanking the moment the track changes and the lyrics popping back in when they load.
            // Keyed on the lyrics themselves, so a translation arriving for the same song updates the
            // page in place rather than fading it.
            AnimatedContent(
                targetState = lyricsData,
                transitionSpec = { fadeIn(tween(LYRICS_SWAP_MS)) togetherWith fadeOut(tween(LYRICS_SWAP_MS)) },
                label = "appleMusicLyricsSwap",
                contentKey = { it?.lyrics },
            ) { lyricsData ->
            if (lyricsData != null) {
                // Transparent background: the page's artwork-tinted gradient is the backdrop,
                // not LyricsView's own dark card color.
                // The app's own renderer, with its own line sizes/animations. The provider caption
                // rides as its LAST list item so it scrolls with the lyrics, per the owner's
                // original spec.
                LyricsView(
                    lyricsData = lyricsData,
                    timeLine = state.timelineFlow,
                    onLineClick = { f ->
                        actions.onUIEvent(UIEvent.UpdateProgress(f))
                    },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // A short fade hides clipping without swallowing the larger blur zones.
                            .appleMusicVerticalFadeEdges(topFade = 24.dp, bottomFade = 32.dp)
                            // Zero under the Apple Music renderer, which insets its own lines so
                            // its blur has margin to spill into; the full gutter under Classic,
                            // which insets nothing. Applied AFTER the fade so the fade still spans
                            // the full width.
                            .padding(horizontal = if (rendererOwnsGutter) 0.dp else AppleMusicLyricPaddingX),
                    backgroundColor = Color.Transparent,
                    footerContent = {
                        // Right-aligned, and stacked: plain text rather than the AIBadge pill the
                        // other styles use. Classic puts that badge beside a "Lyrics" heading where
                        // it has a row to itself; here it would sit inline with the provider
                        // caption, which is already the quietest thing on the page — a filled pill
                        // next to it shouts. Two right-anchored lines read as one footnote block.
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                        ) {
                            if (lyricsData.translatedLyrics?.second == LyricsProvider.AI) {
                                Text(
                                    text = stringResource(Res.string.ai_translated),
                                    style = typography.footer,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            // One line per fact, not one line joined by a bullet. "Word by word"
                            // and "Lyrics provided by SimpMusic Lyrics" are two different things,
                            // and glued together they make a single line long enough to run the
                            // width of the screen.
                            //
                            // fillMaxWidth + TextAlign.End on each, not just the Column's
                            // alignment: Column alignment places a whole text block, so a line that
                            // wraps still ends up left-aligned within itself.
                            Text(
                                text = appleMusicLyricsSyncText(lyricsData),
                                style = typography.footer,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val provider = appleMusicLyricsProviderText(lyricsData)
                            if (provider.isNotBlank()) {
                                Text(
                                    text = provider,
                                    style = typography.footer,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    },
                )
                // Fork: the floating offset / vote / share / fullscreen buttons are gone.
            }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showNoLyrics && lyricsData == null,
                enter = fadeIn(tween(LYRICS_SWAP_MS)),
                exit = fadeOut(tween(LYRICS_SWAP_MS)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = stringResource(Res.string.no_lyrics_for_this_song),
                    style = typography.mainTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                )
            }
        }

        // Desktop only: there the cluster is just the Lyrics · Queue row, the way back to the
        // player with a mouse. Android leaves the page to the lyrics, with room for the gesture bar.
        if (getPlatform() == Platform.Desktop) {
            AppleMusicBottomCluster(
                state = state,
                actions = actions,
                typography = typography,
                viewState = viewState,
                onSelectView = onSelectView,
                activePillContainer = activePillContainer,
                activePillContent = activePillContent,
                deviceVolumeController = deviceVolumeController,
                outputName = outputName,
                onOpenOutput = onOpenOutput,
            )
        } else {
            Spacer(
                modifier =
                    Modifier.height(with(localDensity) { WindowInsets.navigationBars.getBottom(localDensity).toDp() } + 12.dp),
            )
        }
    }
}

// How long the old song's lyrics take to fade into the new song's.
private const val LYRICS_SWAP_MS = 350

/** The sync-type half of the caption: "Word by word", "Line Synced" or "Unsynced". Its own line. */
@Composable
private fun appleMusicLyricsSyncText(lyricsData: NowPlayingScreenData.LyricsData): String {
    val syncTypeText =
        when (lyricsData.lyrics.syncType) {
            "LINE_SYNCED" -> stringResource(Res.string.line_synced)
            "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
            else -> stringResource(Res.string.unsynced)
        }
    return syncTypeText
}

/** The provider half of the caption. Its own line — see the footer for why. */
@Composable
private fun appleMusicLyricsProviderText(lyricsData: NowPlayingScreenData.LyricsData): String =
    when (lyricsData.lyricsProvider) {
        LyricsProvider.SIMPMUSIC -> stringResource(Res.string.lyrics_provider_simpmusic)
        LyricsProvider.LRCLIB -> stringResource(Res.string.lyrics_provider_lrc)
        LyricsProvider.YOUTUBE -> stringResource(Res.string.lyrics_provider_youtube)
        LyricsProvider.SPOTIFY -> stringResource(Res.string.spotify_lyrics_provider)
        LyricsProvider.OFFLINE -> stringResource(Res.string.offline_mode)
        LyricsProvider.BETTER_LYRICS -> stringResource(Res.string.lyrics_provider_betterlyrics)
        LyricsProvider.AI -> ""
    }
