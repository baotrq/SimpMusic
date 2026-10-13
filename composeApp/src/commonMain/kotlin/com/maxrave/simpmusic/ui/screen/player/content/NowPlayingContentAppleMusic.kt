package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.DisposableEffect
import com.maxrave.simpmusic.ui.component.MiniPlayerAnchor
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import kotlin.math.hypot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.rememberDeviceVolumeController
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.LyricText
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SmartDisplay
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicBottomCluster
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicHeaderActions
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricStrip
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricsView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicMeshBackdrop
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicOutputSheet
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicQueueView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicTypography
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicGlyphButton
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.APPLE_MUSIC_TOP_BAR_HEIGHT
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicPanelDrag
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicDirectionalDrag
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicPanelFingerTracker
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicFadeBetween
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicGradientColorAt
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicVerticalFadeEdges
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.displayName
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicMesh
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.pow

/**
 * The Apple Music Now Playing style: a style-internal dock (Lyrics · [Output | Listen Together] ·
 * Queue) swaps the BODY between three full-screen views instead of scrolling a single column, à la
 * [NowPlayingContentSpotify]/[NowPlayingContentM3Expressive]. MAIN stands on a mesh made of the
 * artwork's own colours; LYRICS and QUEUE on the frosted cover art — see [AppleMusicMainView].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentAppleMusic(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // Seeded from the view model, not from MAIN: this player lives in a ModalBottomSheet, so
    // dismissing it disposes the tree and rememberSaveable dies with it. rememberSaveable is still
    // the right holder WITHIN a session (it survives rotation without a round trip), the view model
    // just supplies where the user was last.
    val sharedViewModel: SharedViewModel = koinInject()
    var viewState by rememberSaveable {
        mutableStateOf(
            sharedViewModel.lastPlayerViewTab.value
                ?.let { saved -> AppleMusicView.entries.firstOrNull { it.name == saved } }
                ?: AppleMusicView.MAIN,
        )
    }
    LaunchedEffect(viewState) { sharedViewModel.setLastPlayerViewTab(viewState.name) }

    // Fork: Lyrics and Queue are a PANEL that slides up over the player and back down, following
    // the finger, instead of a crossfade between whole bodies. [viewState] is where the user is;
    // [panelView] is what the panel shows, and outlives viewState through the closing slide so the
    // page does not empty itself on the way down. [panelOffset] is the panel's position as a share
    // of the page height: 0 fully open, 1 fully below the page.
    var panelView by remember { mutableStateOf(viewState.takeIf { it != AppleMusicView.MAIN }) }
    val panelOffset = remember { Animatable(if (viewState == AppleMusicView.MAIN) 1f else 0f) }
    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var panelDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableFloatStateOf(0f) }
    val panelScope = rememberCoroutineScope()

    val openPanel: (AppleMusicView, Float) -> Unit = { view, velocity ->
        panelView = view
        viewState = view
        panelScope.launch { panelOffset.animateTo(0f, PANEL_SPRING, initialVelocity = velocity) }
    }
    val closePanel: (Float) -> Unit = { velocity ->
        viewState = AppleMusicView.MAIN
        panelScope.launch {
            panelOffset.animateTo(1f, PANEL_SPRING, initialVelocity = velocity)
            if (viewState == AppleMusicView.MAIN) panelView = null
        }
    }
    val selectView: (AppleMusicView) -> Unit = { view ->
        if (view == AppleMusicView.MAIN) closePanel(0f) else openPanel(view, 0f)
    }
    // Fork: a back swipe on Lyrics or Queue slides the panel down to the player instead of
    // closing the whole player. Composed inside the sheet, so it takes priority over its dismiss.
    PlatformBackHandler(enabled = viewState != AppleMusicView.MAIN) { closePanel(0f) }
    // A page claims a gesture for the panel; the root, which never moves, then follows the finger
    // (see AppleMusicPanelDrag for why the two are split).
    var panelClaimed by remember { mutableStateOf(false) }
    val panelDrag = remember { AppleMusicPanelDrag(onClaim = { panelClaimed = true }, isClaimed = { panelClaimed }) }
    val onPanelFingerDelta: (Float) -> Unit = { dy ->
        if (pageHeightPx > 0f) {
            if (!panelDragging) {
                panelDragging = true
                dragValue = panelOffset.value
                dragStartValue = dragValue
            }
            dragValue = (dragValue + dy / pageHeightPx).coerceIn(0f, 1f)
            val target = dragValue
            panelScope.launch { panelOffset.snapTo(target) }
        }
    }
    val onPanelFingerUp: (Float) -> Unit = { velocityPx ->
        if (panelDragging) {
            panelDragging = false
            val moved = dragValue - dragStartValue
            val opens =
                when {
                    velocityPx < -PANEL_FLING_VELOCITY -> true
                    velocityPx > PANEL_FLING_VELOCITY -> false
                    moved < -PANEL_COMMIT_FRACTION -> true
                    moved > PANEL_COMMIT_FRACTION -> false
                    else -> dragStartValue < 0.5f
                }
            val velocity = if (pageHeightPx > 0f) velocityPx / pageHeightPx else 0f
            if (opens) openPanel(panelView ?: AppleMusicView.LYRICS, velocity) else closePanel(velocity)
        }
    }

    // Lyrics can disappear mid-session (provider swap gone offline, a track that simply has none)
    // — fall back to MAIN rather than stranding the user on an empty body, matching the dock's own
    // disabled-Lyrics-button gate.
    //
    // The wait is the whole point. Every track change rebuilds NowPlayingScreenData from scratch
    // with lyricsData = null and only THEN fetches (SharedViewModel.kt:342), so "null" is the
    // normal state of every song for as long as the request takes. Reacting to it immediately —
    // which is what this did — threw the user out of the lyrics tab on every single skip, even
    // when the incoming track had lyrics arriving a moment later. Lyrics landing restarts this
    // effect and cancels the wait, so the fallback only ever fires for a track that really has
    // none.
    //
    // Fork: once the wait says the song really has none, the page says so ("No lyrics for this
    // song") and slides back to the player NO_LYRICS_HOLD_MS later, instead of leaving at once.
    // Keyed on the song too, so swiping to another song without lyrics starts the count over.
    // Decided per song, whichever page is up, so opening Lyrics on a song already known to have
    // none shows the message straight away rather than after another wait.
    var lyricsMissing by remember { mutableStateOf(false) }
    LaunchedEffect(state.screenData.lyricsData, state.screenData.nowPlayingTitle, state.screenData.thumbnailURL) {
        lyricsMissing = false
        if (state.screenData.lyricsData != null) return@LaunchedEffect
        delay(LYRICS_ABSENCE_GRACE_MS)
        lyricsMissing = true
    }
    LaunchedEffect(lyricsMissing, viewState) {
        if (viewState != AppleMusicView.LYRICS || !lyricsMissing) return@LaunchedEffect
        delay(NO_LYRICS_HOLD_MS)
        closePanel(0f)
    }

    // This style has no scroll and no collapsed toolbar (unlike Classic/M3E); park the shared
    // toolbar-visibility flag at false so a style switch mid-session can't leave it stuck shown.
    LaunchedEffect(Unit) {
        actions.onToolbarVisibilityChange(false)
    }

    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor == Color.Black) seed else paletteColor
    val activePillContainer = remember(seedColor) { lerp(seedColor, Color.White, 0.75f) }
    val activePillContent = remember(seedColor) { lerp(seedColor, Color.Black, 0.6f) }

    // Only a Spotify canvas takes the whole page now; Apple Music's animated artwork plays in the
    // artwork's own frame (see AppleMusicMainView), so it neither blacks the page out nor hides the
    // controls.
    val fullscreenCanvas = state.screenData.canvasData?.let { !it.isAnimatedArtwork() } == true

    // Fork: YouTube Music's Song / Video switch, for a track that is a music video and is playing
    // with its picture (the watch-video setting). "Song" just stops SHOWING the video — the cover
    // comes back, the sound carries on from the same spot untouched — so the switch is instant both
    // ways. Song is the default; a pick holds for the following tracks until the app is restarted
    // (SharedViewModel.playerShowsVideo).
    val playerShowsVideo by sharedViewModel.playerShowsVideo.collectAsStateWithLifecycle()
    val preferSong = !playerShowsVideo
    val hasVideoVersion = state.screenData.canvasData == null && state.screenData.isVideo && state.shouldShowVideo
    val showVideo = state.shouldShowVideo && !preferSong
    val showVersionSwitch = hasVideoVersion && !fullscreenCanvas
    val showCanvasBackdrop =
        viewState == AppleMusicView.MAIN &&
            (fullscreenCanvas || (state.screenData.canvasData == null && state.screenData.isVideo && showVideo))

    val deviceVolumeController = rememberDeviceVolumeController()
    val typography = rememberAppleMusicTypography()
    val localDensity = LocalDensity.current

    // The canvas page is black and every other state is the artwork gradient — but the swap must
    // NOT be instant. viewState flips the moment a tab is tapped, while the Crossfade below still
    // spends 300ms fading MAIN out: a hard swap repaints the page bright underneath a canvas that
    // is still on screen, which is the flicker when leaving MAIN for Queue/Lyrics and again on the
    // way back. Fading the black layer on the SAME 300ms curve keeps the two in step.
    val canvasBackdropAlpha by animateFloatAsState(
        targetValue = if (showCanvasBackdrop && fullscreenCanvas) 1f else 0f,
        animationSpec = tween(300),
        label = "appleMusicCanvasBackdrop",
    )

    // Where the sound leaves by. The player lists its own outputs; while casting, the receiver is
    // the answer instead. Named by the same rule the output sheet uses, so the caption and the
    // ticked row can never disagree.
    val player = koinInject<MediaPlayerHandler>().player
    val audioOutputs by player.audioOutputs.collectAsStateWithLifecycle()
    val activeOutputName = audioOutputs.firstOrNull { it.isActive }?.displayName()
    val outputName = if (state.castState.isRemote) state.castState.deviceName else activeOutputName
    var showOutputSheet by rememberSaveable { mutableStateOf(false) }
    val openOutput = { showOutputSheet = true }

    // Backdrop source for the Desktop dismiss button below. The glass layers MUST be a sibling of
    // the button, never its parent: nesting the button inside the source is the render-feedback
    // loop that crashes the RuntimeShader.
    val panelBackdrop = rememberBackdrop(Color.Black)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { pageHeightPx = it.height.toFloat() }
                .appleMusicPanelFingerTracker(
                    isClaimed = { panelClaimed },
                    onDelta = { onPanelFingerDelta(it) },
                    onEnd = { onPanelFingerUp(it) },
                    onRelease = { panelClaimed = false },
                ),
    ) {
        Box(modifier = Modifier.matchParentSize().layerBackdrop(panelBackdrop)) {
            AppleMusicArtworkBackdrop(state = state, actions = actions, seedColor = seedColor)
            // Flat black only for a CANVAS (it fills the screen). A video letterboxes, so a black page
            // turns the bars above and below it into dead black slabs — keep the artwork-tinted
            // gradient there.
            if (canvasBackdropAlpha > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = canvasBackdropAlpha)))
            }
        }
        // The player itself. Left composed while the panel is moving or partly open, so the slide
        // shows it underneath; dropped once the panel fully covers it, so a playing video or
        // animated artwork does not keep decoding behind the lyrics.
        val mainCovered by remember {
            derivedStateOf { panelView != null && !panelDragging && !panelOffset.isRunning && panelOffset.value == 0f }
        }
        if (!mainCovered) {
            AppleMusicMainView(
                state = state,
                actions = actions,
                typography = typography,
                viewState = AppleMusicView.MAIN,
                onSelectView = selectView,
                seedColor = seedColor,
                activePillContainer = activePillContainer,
                activePillContent = activePillContent,
                deviceVolumeController = deviceVolumeController,
                outputName = outputName,
                onOpenOutput = openOutput,
                panelDrag = panelDrag,
                onPanelDragStart = { if (panelView == null) panelView = AppleMusicView.LYRICS },
                showVideo = showVideo,
                // The switch sits inside the top bar now, so it takes no room of its own.
                topChromeExtra = 0.dp,
            )
        }

        // While the player is showing, the Lyrics page is still kept built below the page (and not
        // drawn): building it is the heaviest thing on this screen, and doing it on the first frame
        // of a swipe up made the panel hitch before it started to follow the finger.
        //
        // Not on the very first frame, though: building it there made tapping the mini player wait
        // a beat before the player even started to slide in. It is built once the player is up.
        var prewarmLyrics by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(LYRICS_PREWARM_DELAY_MS)
            prewarmLyrics = true
        }
        val shownPanel: AppleMusicView? = panelView ?: AppleMusicView.LYRICS.takeIf { prewarmLyrics }
        shownPanel?.let { shown ->
            // The player dims as the panel covers it.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - panelOffset.value) * PANEL_SCRIM_ALPHA }
                        .background(Color.Black),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = panelOffset.value * size.height
                            // Fully below the page: skip drawing it at all.
                            alpha = if (panelOffset.value >= 1f) 0f else 1f
                        },
            ) {
                // The panel brings its own frosted page: it slides over the player's mesh, which
                // would otherwise show through the lyrics.
                Box(modifier = Modifier.matchParentSize().background(Color.Black)) {
                    AppleMusicArtworkBackdrop(state = state, actions = actions, seedColor = seedColor)
                }
                // A plain switch, not a crossfade: the page only changes while it is out of sight
                // below the player (or on Desktop's Lyrics · Queue row).
                shown.let { view ->
                    when (view) {
                        AppleMusicView.LYRICS ->
                            AppleMusicLyricsView(
                                state = state,
                                actions = actions,
                                typography = typography,
                                viewState = viewState,
                                onSelectView = selectView,
                                activePillContainer = activePillContainer,
                                activePillContent = activePillContent,
                                deviceVolumeController = deviceVolumeController,
                                outputName = outputName,
                                onOpenOutput = openOutput,
                                panelDrag = panelDrag,
                                showNoLyrics = lyricsMissing,
                            )

                        AppleMusicView.QUEUE ->
                            AppleMusicQueueView(
                                state = state,
                                actions = actions,
                                typography = typography,
                                viewState = viewState,
                                onSelectView = selectView,
                                activePillContainer = activePillContainer,
                                activePillContent = activePillContent,
                                deviceVolumeController = deviceVolumeController,
                                outputName = outputName,
                                onOpenOutput = openOutput,
                                panelDrag = panelDrag,
                            )

                        AppleMusicView.MAIN -> Unit
                    }
                }
            }
        }

        // How the page is dismissed, and it differs by platform because the page itself does.
        //
        // On Android this is a bottom sheet, so a grabber is the native way out and tapping it
        // dismisses. On Desktop the same content is a side panel that never slides anywhere — a
        // grabber there is a handle for a gesture that does not exist, which is why this style
        // shipped with no visible way to close the panel at all while Classic and M3 Expressive
        // both draw state.dismissIcon in their top bar.
        // MAIN only: Queue and Lyrics each have their own header, and a floating button over
        // those reads as belonging to the list rather than to the panel.
        if (getPlatform() == Platform.Desktop) {
            if (viewState == AppleMusicView.MAIN) {
                LiquidGlassIconButton(
                    backdrop = panelBackdrop,
                    imageVector = state.dismissIcon,
                    shape = RoundedCornerShape(24.dp),
                    // Same as AnalyticsScreen's back button: a 48dp circle catches only a short arc
                    // of the default directional sweep and reads as rimless. 1.dp is the smallest
                    // step that stays visible without looking like a plain border.
                    highlight = Highlight(width = 1.dp),
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(DISMISS_BUTTON_INSET)
                            .size(DISMISS_BUTTON_SIZE),
                    onClick = { actions.onDismiss() },
                )
            }
        } else if (panelView != AppleMusicView.LYRICS) {
            // Fork: a top bar in place of the grabber — ⌄ on the left, ⋯ on the right, the Song /
            // Video switch between them. It sits ABOVE the views, not inside one, so it stays put
            // while Lyrics/Queue slide over the player. ⌄ steps back one level: from Lyrics or Queue
            // to the player, from the player to wherever the user came from.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = with(localDensity) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(localDensity).toDp() })
                        .height(GRABBER_HEIGHT)
                        .padding(horizontal = 8.dp)
                        // On Lyrics/Queue a drag down here moves the panel, never the sheet.
                        .appleMusicDirectionalDrag(enabled = viewState != AppleMusicView.MAIN, up = false, drag = panelDrag, intercept = true),
            ) {
                AppleMusicGlyphButton(
                    icon = SimpIcons.KeyboardArrowDown,
                    size = 40.dp,
                    onClick = { if (viewState == AppleMusicView.MAIN) actions.onDismiss() else closePanel(0f) },
                )
                Spacer(modifier = Modifier.weight(1f))
                AppleMusicGlyphButton(icon = SimpIcons.MoreVert, size = 40.dp, onClick = { actions.onShowMoreSheet() })
            }
        }

        // The Song / Video switch, top centre in the bar (Desktop: level with the glass dismiss
        // button), on the player page only.
        AnimatedVisibility(
            visible = showVersionSwitch && viewState == AppleMusicView.MAIN && panelView == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top =
                            with(localDensity) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(localDensity).toDp() } +
                                if (getPlatform() == Platform.Desktop) DISMISS_BUTTON_INSET else (GRABBER_HEIGHT - VERSION_SWITCH_HEIGHT) / 2,
                    ),
        ) {
            AppleMusicVersionSwitch(
                videoSelected = !preferSong,
                onSelect = { video -> sharedViewModel.setPlayerShowsVideo(video) },
            )
        }

        if (showOutputSheet) {
            AppleMusicOutputSheet(
                outputs = audioOutputs,
                castState = state.castState,
                deviceVolumeController = deviceVolumeController,
                onSelectOutput = { output -> player.selectAudioOutput(output.id) },
                onRefresh = { player.refreshAudioOutputs() },
                onDismiss = { showOutputSheet = false },
            )
        }
    }
}

/**
 * MAIN: the artwork edge to edge at the top — uncropped, at its own proportions — dissolving into
 * a mesh of its own colours, with the title row, the current lyric and the shared cluster hung
 * from the bottom of the page.
 *
 * The artwork is square for a still cover: a full-width square, with everything else below it.
 * Apple Music's animated artwork (3:4 or 1:1) plays in a frame of its own shape, top-aligned, and
 * keeps the controls exactly where the still cover put them — they must not jump when the clip
 * arrives a few seconds into the track — so a tall clip's lower part fades out behind the title.
 *
 * A Spotify canvas (9:16, made to fill a phone) still takes the whole page: the controls float on
 * top of it with a scrim and auto-hide via [NowPlayingContentState.showControlLayout]. A tap re-shows
 * hidden controls; a tap on an already-shown VIDEO (not canvas) enters the fullscreen video route.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicMainView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    seedColor: Color,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    outputName: String?,
    onOpenOutput: () -> Unit,
    panelDrag: AppleMusicPanelDrag,
    onPanelDragStart: () -> Unit,
    // Fork: whether a music video shows its picture (the Song / Video switch), and the room the
    // switch takes under the top chrome while it is up.
    showVideo: Boolean,
    topChromeExtra: Dp,
) {
    val localDensity = LocalDensity.current
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One

    val canvasData = state.screenData.canvasData
    val animatedArtwork = canvasData?.takeIf { it.isAnimatedArtwork() }
    val fullscreenCanvas = canvasData != null && animatedArtwork == null
    val isVideoBackdrop = canvasData == null && state.screenData.isVideo && showVideo

    // Same fade/half-blended-frame fix M3E uses: fast fade-in, relaxed fade-out. Only a fullscreen
    // canvas ever hides the controls — every other artwork sits above them, not behind them.
    val controlsAlpha by animateFloatAsState(
        targetValue = if (!fullscreenCanvas || state.showControlLayout) 1f else 0f,
        animationSpec = tween(durationMillis = if (!fullscreenCanvas || state.showControlLayout) 180 else 500, easing = LinearEasing),
        label = "appleMusicControlsAlpha",
    )

    // Over-video controls, exactly like Classic/M3E: hidden by default, a tap on the video shows
    // them, and they auto-hide again after 3s. Subtitles default on, toggled from that overlay.
    var showVideoOverlay by rememberSaveable { mutableStateOf(false) }
    var showSubtitle by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(showVideoOverlay) {
        if (showVideoOverlay) {
            delay(3000)
            showVideoOverlay = false
        }
    }

    // Height of the controls block, for fitting a video above it and sizing the canvas idle overlay.
    // remember, not rememberSaveable: a measured height must not survive a rotation. Seeded near its
    // real value so the first frame does not lay a video out against a wrong one.
    var bottomContentHeightDp by remember { mutableIntStateOf(430) }

    // What the page's mesh is read from: the cover, or the animated artwork's still once it is up —
    // the page should continue whatever is actually on screen above it.
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var clipStill by remember(animatedArtwork?.url) { mutableStateOf<ImageBitmap?>(null) }
    var clipFrameHeight by remember(animatedArtwork?.url) { mutableStateOf(0.dp) }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                // Fork: swipe up pulls the lyrics panel up after the finger, in place of the dock's
                // Lyrics button. A swipe down is left to the sheet, which still closes the player.
                // Always on: a song with no lyrics opens to "No lyrics for this song".
                .appleMusicDirectionalDrag(
                    enabled = true,
                    up = true,
                    drag = panelDrag,
                    onStart = onPanelDragStart,
                    intercept = true,
                ),
    ) {
        // A full-width square: the cover at its own proportions, nothing cut off either side. Capped
        // for a landscape window, where a full-width square would be taller than the page.
        val artworkHeight = minOf(maxWidth, maxHeight * ARTWORK_MAX_HEIGHT_FRACTION)
        val clipMaxHeight = maxHeight * ANIMATED_ARTWORK_MAX_HEIGHT_FRACTION
        val artworkFade = artworkHeight * ARTWORK_FADE_FRACTION
        val showsClip = animatedArtwork != null && clipStill != null
        val statusBarHeight = with(localDensity) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(localDensity).toDp() }

        // Fork: the still cover is centred in the room between the top chrome and the controls,
        // instead of pinned to the top of the page with the gap left under it. Where there is no
        // spare room (a landscape window) it stays at the top as before. The animated artwork keeps
        // its own top-aligned frame.
        val topChrome =
            statusBarHeight +
                (if (getPlatform() == Platform.Desktop) DISMISS_BUTTON_INSET + DISMISS_BUTTON_SIZE else GRABBER_HEIGHT) +
                topChromeExtra
        val artworkSpare = maxHeight - bottomContentHeightDp.dp - topChrome - artworkHeight
        val artworkTop = if (artworkSpare > 0.dp) topChrome + artworkSpare / 2 else 0.dp
        // Fork: where the cover sits, so the one flying in from the mini player lands on it.
        val playerArtRect =
            with(localDensity) { Rect(0f, artworkTop.toPx(), maxWidth.toPx(), (artworkTop + artworkHeight).toPx()) }
        DisposableEffect(playerArtRect) {
            MiniPlayerAnchor.playerArt = playerArtRect
            onDispose { MiniPlayerAnchor.playerArt = null }
        }

        // The page itself. A video keeps the frosted cover behind it and a fullscreen canvas the black
        // page, as before; everything else stands on the mesh, hung from where the artwork ends.
        val meshPage = !fullscreenCanvas && !isVideoBackdrop
        val mesh =
            if (meshPage) {
                rememberAppleMusicMesh(
                    artwork = if (showsClip) clipStill else coverBitmap,
                    seed = (animatedArtwork?.url ?: state.screenData.thumbnailURL).hashCode(),
                )
            } else {
                null
            }
        val meshSeam = if (showsClip) clipFrameHeight else artworkTop + artworkHeight
        // Fork: while the player grows out of the mini player the cover sits plain on a solid page;
        // once it is fully open the Apple Music look (the mesh, the cover's dissolving edges, the
        // status-bar blur) blends in over half a second.
        val playerSettled by remember { derivedStateOf { (MiniPlayerAnchor.progress?.invoke() ?: 0f) == 0f } }
        val effects = remember { Animatable(if (playerSettled) 1f else 0f) }
        LaunchedEffect(playerSettled) {
            if (playerSettled) {
                effects.animateTo(1f, tween(EFFECTS_IN_MS, delayMillis = EFFECTS_DELAY_MS, easing = FastOutSlowInEasing))
            } else {
                effects.snapTo(0f)
            }
        }
        if (meshPage) {
            Box(Modifier.fillMaxSize().background(appleMusicGradientColorAt(seedColor, 1f)))
            // The mesh spreads out from the cover to the screen's edges: a soft-edged circle centred
            // on the cover, growing from the cover's size to the whole page.
            AppleMusicMeshBackdrop(
                mesh = mesh,
                seam = meshSeam,
                modifier =
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = if (effects.value < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                        }.drawWithContent {
                            drawContent()
                            val e = effects.value
                            if (e < 1f) {
                                val center = Offset(size.width / 2, (artworkTop + artworkHeight / 2).toPx())
                                val start = artworkHeight.toPx() / 2
                                val radius = (start + (hypot(size.width, size.height) - start) * e).coerceAtLeast(1f)
                                drawRect(
                                    Brush.radialGradient(
                                        0f to Color.Black,
                                        0.6f to Color.Black,
                                        1f to Color.Transparent,
                                        center = center,
                                        radius = radius,
                                    ),
                                    alpha = if (e <= 0f) 0f else 1f,
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        },
            )
        }

        // The band under the status bar and the grabber, where a bright or busy sleeve drowns their
        // white glyphs: blurred (TopBandBlur, inside each page so it moves with it) and darkened
        // (the wash below). Android only — Desktop has neither, and its dismiss button is glass.
        val topBand =
            if (meshPage && getPlatform() == Platform.Android) statusBarHeight + TOP_BAND_STRIP else 0.dp

        HorizontalPager(
            state = state.artworkPagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
            key = { idx ->
                val vid = state.artworkQueue.getOrNull(idx)?.videoId.orEmpty()
                "appleMusicArtwork_${vid}_$idx"
            },
        ) { page ->
            AppleMusicArtworkPage(
                state = state,
                actions = actions,
                localDensity = localDensity,
                page = page,
                topChromeExtra = topChromeExtra,
                artworkTop = artworkTop,
                artworkHeight = artworkHeight,
                clipMaxHeight = clipMaxHeight,
                topBandSolid = statusBarHeight,
                topBand = topBand,
                bottomContentHeightDp = bottomContentHeightDp,
                fullscreenCanvas = fullscreenCanvas,
                animatedArtwork = animatedArtwork,
                isVideoBackdrop = isVideoBackdrop,
                showVideoOverlay = showVideoOverlay,
                onToggleVideoOverlay = { showVideoOverlay = !showVideoOverlay },
                showSubtitle = showSubtitle,
                onToggleSubtitle = { showSubtitle = !showSubtitle },
                onCoverBitmap = { coverBitmap = it },
                onClipStill = { still, height ->
                    clipStill = still
                    clipFrameHeight = height
                },
                effects = { effects.value },
            )
        }

        // Android plays the animated artwork on a SurfaceView, which no Compose mask or alpha reaches:
        // masked like the cover, the clip still ended in a hard edge across the page. So the page is
        // laid back OVER the clip's lower part instead — the same mesh at the same size and seam, so it
        // matches the page beneath it pixel for pixel — faded in over the stretch the cover dissolves
        // over, ending at the clip's bottom edge.
        if (meshPage && animatedArtwork != null && clipFrameHeight > 0.dp) {
            AppleMusicMeshBackdrop(
                mesh = mesh,
                seam = meshSeam,
                modifier = Modifier.appleMusicFadeBetween(transparentAt = clipFrameHeight - artworkFade, opaqueAt = clipFrameHeight),
            )
        }

        // The wash under the status bar: as dark as the sleeve's own top band is bright, 16%–65%,
        // so a dark cover is left almost untouched and a bright one still carries white icons.
        if (topBand > 0.dp) {
            val topWashAlpha by animateFloatAsState(
                targetValue = TOP_WASH_MIN_ALPHA + (TOP_WASH_MAX_ALPHA - TOP_WASH_MIN_ALPHA) * (mesh?.topLuminance ?: 1f),
                animationSpec = tween(TOP_WASH_FADE_MS),
                label = "appleMusicTopWash",
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(topBand)
                        .background(topWashBrush(topWashAlpha)),
            )
        }

        // Top scrim, fullscreen CANVAS ONLY: a canvas replaces the whole page, so the status bar and
        // grabber would otherwise float on raw picture; a video letterboxes and the strip around it
        // is still the artwork gradient. Rides controlsAlpha — a scrim is an accessory of the
        // controls, and left painted after they auto-hid it covered most of the canvas for nothing.
        if (fullscreenCanvas && controlsAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .alpha(controlsAlpha)
                        .fillMaxWidth()
                        .fillMaxHeight(0.22f)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                1f to Color.Transparent,
                            ),
                        ),
            )
        }

        // Owner's spec, verbatim: a BLACK→TRANSPARENT gradient under the title row + cluster
        // while a canvas plays — solid black at the very bottom, fading out upward.
        // Height is a FRACTION of this Box, never a dp computed from screenInfo: a zero/stale
        // hDP silently collapsed this scrim to nothing, which is why it kept "not existing".
        if (fullscreenCanvas && controlsAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .alpha(controlsAlpha)
                        .fillMaxWidth()
                        // Tall enough that the controls fill its lower 60%, so the title always sits
                        // at the same depth in it. A fixed 60% of the page left the title in the
                        // faint top of the ramp once the lyric strip and the action row made the
                        // controls taller.
                        .fillMaxHeight(((bottomContentHeightDp.dp / 0.6f) / maxHeight.coerceAtLeast(1.dp)).coerceIn(0.6f, 1f))
                        .background(
                            Brush.verticalGradient(
                                // Ramps harder early: at the old stops the TITLE row sat at only
                                // ~56% black over the video while the lower controls had 76-92%.
                                0f to Color.Transparent,
                                0.22f to Color.Black.copy(alpha = 0.45f),
                                0.50f to Color.Black.copy(alpha = 0.82f),
                                1f to Color.Black.copy(alpha = 0.97f),
                            ),
                        ),
            )
        }

        // Box, not Column: the real content and the idle overlay below must OVERLAP (Z-stack),
        // not lay out one after another — matches NowPlayingContentM3Expressive's exact structure
        // for this same canvas-unfocused-overlay pattern.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            // Hung from the bottom of the page. Whatever height is left above it and below the
            // artwork is the mesh continuing the artwork's own colours.
            // Fork: when the player grows out of the mini player this block (title, artist, transport,
            // lyric line) rises in with it.
            MiniPlayerAnchor.controlsSlidePx = with(localDensity) { CONTROLS_RISE.toPx() }
            Column(
                modifier =
                    Modifier
                        .alpha(controlsAlpha)
                        .graphicsLayer { translationY = MiniPlayerAnchor.controlsOffset() }
                        .onGloballyPositioned { coords ->
                            bottomContentHeightDp =
                                with(localDensity) { coords.size.height.toDp().value.toInt() }
                        },
            ) {
                AppleMusicMainTitleRow(
                    state = state,
                    actions = actions,
                    typography = typography,
                    onOpenQueue = { onSelectView(AppleMusicView.QUEUE) },
                    onOpenLyrics = { onSelectView(AppleMusicView.LYRICS) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppleMusicLyricStrip(
                    state = state,
                    typography = typography,
                    onClick = { onSelectView(AppleMusicView.LYRICS) },
                )
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
                    // The lyric sits right on the progress bar: it is read with the bar, not the title.
                    topPadding = 2.dp,
                    showActionRow = false,
                    onOpenLyrics = { onSelectView(AppleMusicView.LYRICS) },
                )
            }
            // Idle overlay — replaces the (now invisible) title row + cluster while a fullscreen
            // canvas has auto-hidden them, mirroring M3E's canvas-unfocused overlay verbatim.
            if (fullscreenCanvas) {
                AnimatedVisibility(
                    visible = !state.showControlLayout,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(bottomContentHeightDp.dp)
                                .clickable(
                                    onClick = { actions.onToggleControls() },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        // The page's OWN colour, not black. This scrim is what the artwork fades
                        // INTO, so it has to be the same tone the page is painted with below it —
                        // appleMusicGradientColorAt(seed, 1f), i.e. the thumbnail's colour darkened,
                        // exactly like the bottom stop of backdropBrush. Fading to black instead
                        // laid a dark slab over a tinted page and left a visible seam where the two
                        // met, which read as "black smudge into transparent".
                        //
                        // Fully opaque at the bottom for the same reason: at 0.85 the artwork still
                        // showed through the last few pixels and tinted the seam a different colour
                        // than the page continuing beneath it.
                        val pageScrimColor = appleMusicGradientColorAt(seedColor, 1f)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        smoothScrimBrush(
                                            from = pageScrimColor.copy(alpha = 0f),
                                            to = pageScrimColor,
                                        ),
                                    ),
                        )
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom =
                                            with(localDensity) {
                                                WindowInsets.systemBars.getBottom(localDensity).toDp()
                                            } + 16.dp,
                                    ),
                        ) {
                            AnimatedVisibility(
                                visible = state.currentLyricLineIndex > -1,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                val lineText =
                                    state.screenData.lyricsData
                                        ?.lyrics
                                        ?.lines
                                        ?.getOrNull(state.currentLyricLineIndex)
                                        ?.words
                                        ?.stripRichSyncTimestamps()
                                if (!lineText.isNullOrBlank()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        LyricText(
                                            text = lineText,
                                            style = typography.idleLyric,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 2.dp)
                                                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                    .focusable(),
                                        )
                                        val translatedLineText =
                                            state.screenData.lyricsData
                                                ?.translatedLyrics
                                                ?.first
                                                ?.lines
                                                ?.getOrNull(state.currentLyricLineIndex)
                                                ?.words
                                                ?.stripRichSyncTimestamps()
                                        if (!translatedLineText.isNullOrBlank()) {
                                            LyricText(
                                                text = translatedLineText,
                                                alignmentText = lineText,
                                                style = typography.idleTranslated,
                                                maxLines = 1,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 2.dp)
                                                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                        .focusable(),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // The DEFAULT style shows its full NowPlayingTrackInfoRow here — 55dp thumbnail
                            // and the ⊕ / ☆ / ⋯ actions. This is that row, not a stripped-down copy of it.
                            // It types with the COMPACT slots, not the main ones: this is a track header
                            // floating over a canvas, the same job the Lyrics/Queue header does.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            ) {
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(state.screenData.thumbnailURL)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(state.screenData.thumbnailURL)
                                            .crossfade(300)
                                            .build(),
                                    placeholder = rememberHolderPainter(),
                                    error = rememberHolderPainter(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.screenData.nowPlayingTitle,
                                        style = typography.compactTitle,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                .focusable(),
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = state.screenData.artistName,
                                        style = typography.compactArtist,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                .focusable(),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                AppleMusicHeaderActions(state = state, actions = actions)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fork: YouTube Music's two-part Song | Video pill — headphones and a video glyph in one dark
 * capsule, the chosen half lit. The light slides between the halves rather than jumping.
 */
@Composable
private fun AppleMusicVersionSwitch(
    videoSelected: Boolean,
    onSelect: (video: Boolean) -> Unit,
) {
    val highlightOffset by animateDpAsState(
        targetValue = if (videoSelected) VERSION_SWITCH_SEGMENT_WIDTH else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "appleMusicVersionSwitch",
    )
    Box(
        modifier =
            Modifier
                .size(width = VERSION_SWITCH_SEGMENT_WIDTH * 2, height = VERSION_SWITCH_HEIGHT)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(start = highlightOffset)
                    .size(width = VERSION_SWITCH_SEGMENT_WIDTH, height = VERSION_SWITCH_HEIGHT)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
        )
        Row {
            listOf(false to SimpIcons.Headphones, true to SimpIcons.SmartDisplay).forEach { (video, icon) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(width = VERSION_SWITCH_SEGMENT_WIDTH, height = VERSION_SWITCH_HEIGHT)
                            .clip(CircleShape)
                            .clickable { onSelect(video) },
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (video) "Video" else "Song",
                        tint = Color.White.copy(alpha = if (video == videoSelected) 1f else 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * The frosted cover art behind the page, with the artwork-derived gradient washed over it. Shared
 * with the fullscreen lyrics landscape layout. Emits straight into the caller's Box — in both
 * places that Box is the glass backdrop source, and anything stacked after this call (the canvas
 * black layer here) must keep drawing on top of it.
 */
@Composable
internal fun BoxScope.AppleMusicArtworkBackdrop(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    seedColor: Color,
) {
    // The artwork bitmap feeds BOTH the frosted backdrop below and the palette every colour on
    // this page is derived from. The only thing that ever supplied it is the AsyncImage inside the
    // artwork pager, which lives in MAIN — and the Crossfade composes exactly one body, so on
    // QUEUE or LYRICS that pager does not exist. Changing track there fed nothing, and the page
    // fell back to a flat gradient.
    //
    // The loader below sits OUTSIDE the Crossfade so it covers every body, and it is an AsyncImage
    // rather than an imperative ImageLoader.execute(): the pager's AsyncImage demonstrably loads
    // this exact url while the execute() call did not, so this uses the path already proven to
    // work rather than a second one that has to be kept working.
    var backdropUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }

    // The approved mock's page gradient is THREE stops — a clearly-tinted top, ~55%-darkened by
    // mid-page (48%), warm near-black at the bottom. The first cut's two stops to near-black read
    // as a flat black page on any dark artwork (first device screenshots).
    val backdropBrush =
        remember(seedColor) {
            Brush.verticalGradient(
                0f to appleMusicGradientColorAt(seedColor, 0f),
                0.48f to appleMusicGradientColorAt(seedColor, 0.48f),
                1f to appleMusicGradientColorAt(seedColor, 1f),
            )
        }

    // Apple frosts the COVER ART into the page background — the colour and the soft blotches
    // of the artwork stay visible through it. A flat tinted gradient, which is what this used
    // to be, gets the hue right and loses everything else: the page reads as a solid colour
    // swatch rather than as the record it belongs to.
    //
    // Loaded straight from the url by AsyncImage rather than through the screen state's
    // decoded bitmap. The background IS an image, so there is no reason to route it through a
    // bitmap someone else has to remember to fill in — which is exactly what broke: the only
    // thing feeding that bitmap was the artwork pager inside MAIN, so on QUEUE or LYRICS a
    // track change left it null and the page fell back to a bare gradient.
    //
    // The palette still needs a bitmap, and it comes off this same load. One source, so the
    // frosted art and the tint over it cannot end up belonging to different songs.
    //
    // The heavy blur radius is safe because the whole style is gated behind Android 12 for
    // exactly this reason (isLyricsBlurSupported), and Crop + fillMaxSize means the artwork is
    // scaled far past its own resolution — at this blur that costs nothing visually.
    if (!backdropUrl.isNullOrBlank()) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(backdropUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(backdropUrl + "BIGGER")
                    .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
            // Same fallback the artwork pager carries: maxresdefault is missing for plenty of
            // videos, and without this the page would simply stay black.
            onError = {
                val fallback = backdropUrl?.replace("maxresdefault", "hqdefault")
                if (fallback != null && fallback != backdropUrl) backdropUrl = fallback
            },
            modifier = Modifier.fillMaxSize().blur(BACKDROP_BLUR_RADIUS, BlurredEdgeTreatment.Unbounded),
        )
    }
    // The tint still rides on top, but as a translucent wash rather than the whole background:
    // it keeps the vertical darkening that makes the controls readable at the bottom, while the
    // frosted artwork shows through it.
    Box(modifier = Modifier.fillMaxSize().alpha(BACKDROP_TINT_ALPHA).background(backdropBrush))
}

@Composable
internal fun AppleMusicMainTitleRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    // Fork: MAIN puts Queue here, beside ⋯, in place of the dock row under the transport.
    onOpenQueue: (() -> Unit)? = null,
    // Fork: and Lyrics beside Queue (a swipe up opens them too).
    onOpenLyrics: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typography.mainTitle,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = state.screenData.artistName,
                    style = typography.mainArtist,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                            .focusable()
                            .clickable { actions.onNavigateToArtist() },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (onOpenLyrics != null) {
            AppleMusicGlyphButton(
                icon = SimpIcons.Lyrics,
                onClick = onOpenLyrics,
                // Dimmed, not hidden, until the song's lyrics are known: the page still opens.
                tint = Color.White.copy(alpha = if (state.screenData.lyricsData != null) 1f else 0.4f),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        if (onOpenQueue != null) {
            AppleMusicGlyphButton(icon = SimpIcons.QueueMusic, onClick = onOpenQueue)
            Spacer(modifier = Modifier.width(12.dp))
        }
        // ⋯ is in the top bar on the Android player (onOpenQueue marks that caller).
        AppleMusicHeaderActions(
            state = state,
            actions = actions,
            showMore = onOpenQueue == null || getPlatform() != Platform.Android,
        )
    }
}

/**
 * One pager page. Current page: the cover stays composed (alpha 0 under a fullscreen canvas/video,
 * or once an animated artwork has faded in over it) so [NowPlayingContentActions.onArtworkBitmap]
 * keeps feeding the palette — otherwise the page colours would go stale on the next track if it
 * also opens on a canvas. Adjacent pages: a static thumbnail in the same full-width square.
 */
@Composable
private fun AppleMusicArtworkPage(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    localDensity: Density,
    page: Int,
    topChromeExtra: Dp,
    artworkTop: Dp,
    artworkHeight: Dp,
    clipMaxHeight: Dp,
    topBandSolid: Dp,
    topBand: Dp,
    bottomContentHeightDp: Int,
    fullscreenCanvas: Boolean,
    animatedArtwork: NowPlayingScreenData.CanvasData?,
    isVideoBackdrop: Boolean,
    showVideoOverlay: Boolean,
    onToggleVideoOverlay: () -> Unit,
    showSubtitle: Boolean,
    onToggleSubtitle: () -> Unit,
    onCoverBitmap: (ImageBitmap) -> Unit,
    onClipStill: (ImageBitmap?, Dp) -> Unit,
    // 0 while the player is growing out of the mini player, rising to 1 once it is open.
    effects: () -> Float = { 1f },
) {
    val pageTrack = state.artworkQueue.getOrNull(page)
    val isCurrentPage = page == state.currentOrderIndex
    // Fork: a video surface ignores the in-window player's clip and fade, so while the player grows
    // out of the mini player or shrinks back into it the page shows the still cover; the canvas,
    // video or animated artwork starts once the player is fully open.
    val playerSettled by remember { derivedStateOf { (MiniPlayerAnchor.progress?.invoke() ?: 0f) == 0f } }
    val pageShowsFullscreen = isCurrentPage && (fullscreenCanvas || isVideoBackdrop) && playerSettled
    val pageShowsClip = isCurrentPage && animatedArtwork != null && playerSettled
    // How much of the artwork dissolves into the page: 42% of the frame, on a smoothstep curve
    // (appleMusicVerticalFadeEdges), so there is no line where the fade starts.
    val artworkFade = artworkHeight * ARTWORK_FADE_FRACTION
    // Fork: once the cover is moved down off the top of the page its top edge would be a hard line
    // across the mesh, so it dissolves at the top too. The top band's blur is measured from the
    // page's top, so it is shifted into the cover box's own coordinates (and drops out entirely
    // once the cover starts below the band).
    val artworkTopFade = if (artworkTop > 0.dp) artworkHeight * ARTWORK_TOP_FADE_FRACTION else 0.dp
    val bandSolid = (topBandSolid - artworkTop).coerceAtLeast(0.dp)
    val bandFade = (topBand - artworkTop).coerceAtLeast(0.dp)

    // The animated artwork fades in over the cover once its still is up, rather than cutting in.
    var clipReady by remember(animatedArtwork?.url) { mutableStateOf(false) }
    val clipAlpha by animateFloatAsState(
        targetValue = if (pageShowsClip && clipReady) 1f else 0f,
        animationSpec = tween(ARTWORK_FADE_IN_MS),
        label = "appleMusicAnimatedArtworkIn",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCurrentPage) {
            var artworkUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }
            // Fork: the cover flying in from the mini player loads this same image.
            DisposableEffect(artworkUrl) {
                MiniPlayerAnchor.playerArtUrl = artworkUrl
                onDispose { MiniPlayerAnchor.playerArtUrl = null }
            }
            val coverRequest =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(artworkUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(artworkUrl + "BIGGER")
                    .crossfade(550)
                    .build()
            val coverAlpha = if (pageShowsFullscreen) 0f else 1f - clipAlpha
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = artworkTop)
                        .fillMaxWidth()
                        .height(artworkHeight)
                        // Fork: takes over from the cover flying in from the mini player.
                        .graphicsLayer { alpha = MiniPlayerAnchor.coverHandoffAlpha() },
            ) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = "",
                    onSuccess = {
                        val bitmap = it.result.image.toImageBitmap()
                        actions.onArtworkBitmap(bitmap)
                        onCoverBitmap(bitmap)
                    },
                    onError = {
                        val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                        if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                    },
                    contentScale = ContentScale.Crop,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    // The artwork DISSOLVES (alpha mask) instead of being covered by a colour
                    // overlay: that overlay had to land on exactly the page's colour at that Y,
                    // and any drift drew a hard horizontal line across the screen. Masking lets
                    // the real background show through — nothing left to match.
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(coverAlpha)
                            .graphicsLayer { alpha = effects() }
                            .appleMusicVerticalFadeEdges(topFade = artworkTopFade, bottomFade = artworkFade),
                )
                TopBandBlur(
                    model = coverRequest,
                    solidTo = bandSolid,
                    fadeTo = bandFade,
                    modifier = Modifier.alpha(coverAlpha).graphicsLayer { alpha = effects() },
                )
                // Fork: the plain cover, the same picture as the one flying in from the mini player,
                // shown while the player grows and faded out as the Apple Music look comes in.
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(coverAlpha)
                            .graphicsLayer { alpha = 1f - effects() },
                )
            }
            if (pageShowsClip && animatedArtwork != null) {
                AnimatedArtworkFrame(
                    canvas = animatedArtwork,
                    heightLimit = clipMaxHeight,
                    topBandSolid = topBandSolid,
                    topBand = topBand,
                    alpha = clipAlpha,
                    onStill = { still, height ->
                        clipReady = true
                        onClipStill(still, height)
                    },
                )
            }
            if (pageShowsFullscreen) {
                if (isVideoBackdrop) {
                    // Centre the video in the region ABOVE the controls (top → cluster), not in
                    // the whole screen: screen-centred, half of a 16:9 video sat behind the
                    // control cluster and its subtitles landed on the dock.
                    // The frame takes the video's own shape, fitted into that region, but never
                    // starts above the top chrome — status bar + grabber on Android, the glass
                    // dismiss button on Desktop — plus a gap: a tall video would otherwise run into
                    // them and put the fullscreen button under the status bar.
                    val topChrome =
                        with(localDensity) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(localDensity).toDp() } +
                            (if (getPlatform() == Platform.Desktop) DISMISS_BUTTON_INSET + DISMISS_BUTTON_SIZE else GRABBER_HEIGHT) +
                            topChromeExtra +
                            VIDEO_FRAME_TOP_GAP
                    BoxWithConstraints(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                // The page's own height minus the controls, not a window-based
                                // measure: the Desktop side panel is shorter than the window by its
                                // shell padding, which pushed a tall frame down into the gap above
                                // the title.
                                .fillMaxSize()
                                .padding(bottom = bottomContentHeightDp.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onToggleVideoOverlay() },
                    ) {
                        val ratio = state.videoAspectRatio
                        val frameHeight =
                            minOf(maxWidth / ratio, (maxHeight - topChrome - VIDEO_FRAME_BOTTOM_GAP).coerceAtLeast(0.dp))
                        val frameWidth = frameHeight * ratio
                        val frameTop = maxOf((maxHeight - frameHeight) / 2, topChrome)
                        // THE VIDEO FRAME. Everything over-video — the surface, the subtitle and
                        // the whole control overlay — is anchored to THIS box, so the fullscreen
                        // button sits on the video's own top-right corner and the subtitle button
                        // on its bottom-right, instead of being flung to the corners of the much
                        // taller zone (fullscreen ended up under the status bar, subtitles far
                        // below the picture).
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = frameTop)
                                    .size(width = frameWidth, height = frameHeight)
                                    // A vertical video stands clear of the screen edges, so it is rounded
                                    // like a card; wide ones run edge to edge and stay square.
                                    .then(if (ratio < 1f) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier),
                        ) {
                            MediaPlayerViewWithSubtitle(
                                playerName = MAIN_PLAYER,
                                // fillMaxWidth, never fillMaxSize: a free height lets the surface's
                                // own aspect ratio apply, which is what stops it being stretched.
                                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                                shouldShowSubtitle = showSubtitle,
                                shouldPip = false,
                                shouldScaleDownSubtitle = true,
                                timelineState = state.timelineState,
                                lyricsData = state.screenData.lyricsData?.lyrics,
                                translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                                isInPipMode = state.isInPipMode,
                                mainTextStyle = typo().bodyLarge,
                                translatedTextStyle = typo().bodyMedium,
                            )

                            // Classic/M3E's over-video controls, ported: fullscreen, ±5s, subtitles.
                            // A tap on the video toggles them; they auto-hide after 3s.
                            // Rendered INSIDE the video zone and drawn after the tap-catcher below,
                            // so its buttons are the topmost target — otherwise the catcher swallows
                            // every tap and the buttons look dead.
                            Crossfade(targetState = showVideoOverlay, label = "appleMusicVideoOverlay") { shown ->
                                if (shown) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.28f)),
                                    ) {
                                        IconButton(
                                            onClick = { actions.onEnterFullscreenVideo() },
                                            modifier = Modifier.align(Alignment.TopEnd),
                                        ) {
                                            Icon(imageVector = SimpIcons.Fullscreen, contentDescription = "", tint = Color.White)
                                        }
                                        Row(
                                            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                        ) {
                                            IconButton(
                                                onClick = { actions.onUIEvent(UIEvent.Backward) },
                                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.Replay5,
                                                    contentDescription = "",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp).alpha(0.8f),
                                                )
                                            }
                                            IconButton(
                                                onClick = { actions.onUIEvent(UIEvent.Forward) },
                                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.Forward5,
                                                    contentDescription = "",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp).alpha(0.8f),
                                                )
                                            }
                                        }
                                        if (state.screenData.lyricsData != null) {
                                            IconButton(
                                                onClick = { onToggleSubtitle() },
                                                modifier = Modifier.align(Alignment.BottomEnd),
                                            ) {
                                                Icon(
                                                    imageVector = if (showSubtitle) SimpIcons.SubtitlesOff else SimpIcons.Subtitles,
                                                    contentDescription = "",
                                                    tint = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Crossfade(targetState = state.screenData.canvasData?.isVideo, label = "appleMusicCanvasKind") { isVideo ->
                        if (isVideo == true) {
                            state.screenData.canvasData?.url?.let { url ->
                                // cropToBounds, NOT the default style's fill-height-and-overflow
                                // modifiers. Those drive MediaPlayerView's legacy path, which sizes
                                // the surface from a `widthPx` seeded to the SCREEN width and only
                                // corrects it once onVideoSizeChanged reports the real aspect ratio.
                                // For a 9:16 canvas the true width is far wider than the screen, so
                                // the first frame renders fitted and the next one jumps to cropped —
                                // the sideways flash when returning from the Lyrics tab, where the
                                // Crossfade had disposed this whole subtree and every remember with
                                // it. The crop path takes its size from Media3's own
                                // presentationState instead, so there is no wrong guess to correct,
                                // and it holds a shutter over the surface until the first frame is
                                // actually ready.
                                MediaPlayerView(
                                    url = url,
                                    cropToBounds = true,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else if (isVideo == false) {
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(state.screenData.canvasData?.url)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(state.screenData.canvasData?.url)
                                        .crossfade(550)
                                        .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                // Canvas only: a full-size tap-catcher toggles the player's controls. The VIDEO
                // case must NOT have one — it would sit above the over-video overlay and swallow
                // every tap meant for fullscreen/±5s/subtitles (its tap is handled by the video
                // zone Box instead).
                if (!isVideoBackdrop) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { actions.onToggleControls() },
                    )
                }
            }
        } else if (pageTrack != null) {
            val staticThumb = pageTrack.thumbnails?.maxByOrNull { it.width * it.height }?.url
            val thumbRequest =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(staticThumb)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(staticThumb)
                    .crossfade(300)
                    .build()
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = artworkTop)
                        .fillMaxWidth()
                        .height(artworkHeight),
            ) {
                AsyncImage(
                    model = thumbRequest,
                    contentDescription = pageTrack.title,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    modifier = Modifier.fillMaxSize().appleMusicVerticalFadeEdges(topFade = artworkTopFade, bottomFade = artworkFade),
                )
                // Blurred like the current page's, or its top band would turn sharp mid-swipe.
                TopBandBlur(model = thumbRequest, solidTo = bandSolid, fadeTo = bandFade)
            }
        }
    }
}

/**
 * Apple Music's animated artwork played this style's way, for the Classic and Material 3 Expressive
 * styles to lay over their own page: the clip edge to edge at the top, dissolving into a mesh of its
 * own colours, with the band under the style's top bar (which ends at [topChrome]) blurred and
 * darkened so the bar's white glyphs survive a bright sleeve. Fills its parent, the style's first
 * screen, and fades into [pageColor] at the bottom so scrolling past the fold shows no seam.
 *
 * Nothing shows until the clip's still is up: the style's own page stays in view until then, and
 * this page fades in over it instead of jumping from a square cover to a full-width one. [cover]
 * seeds the mesh until the still has been read.
 */
@Composable
internal fun AppleMusicAnimatedArtworkPage(
    canvas: NowPlayingScreenData.CanvasData,
    cover: ImageBitmap?,
    topChrome: Dp,
    pageColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.union(WindowInsets.captionBar).getTop(density).toDp() }
    var clipStill by remember(canvas.url) { mutableStateOf<ImageBitmap?>(null) }
    var clipFrameHeight by remember(canvas.url) { mutableStateOf(0.dp) }
    val mesh = rememberAppleMusicMesh(artwork = clipStill ?: cover, seed = canvas.url.hashCode())
    val pageAlpha by animateFloatAsState(
        targetValue = if (clipFrameHeight > 0.dp && mesh != null) 1f else 0f,
        animationSpec = tween(ARTWORK_FADE_IN_MS),
        label = "animatedArtworkPageIn",
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize().alpha(pageAlpha)) {
        val artworkFade = minOf(maxWidth, maxHeight * ARTWORK_MAX_HEIGHT_FRACTION) * ARTWORK_FADE_FRACTION
        val topBand = topChrome + TOP_BAND_STRIP
        AppleMusicMeshBackdrop(mesh = mesh, seam = clipFrameHeight)
        AnimatedArtworkFrame(
            canvas = canvas,
            heightLimit = maxHeight * ANIMATED_ARTWORK_MAX_HEIGHT_FRACTION,
            topBandSolid = statusBarHeight,
            topBand = topBand,
            alpha = 1f,
            onStill = { still, height ->
                clipStill = still
                clipFrameHeight = height
            },
        )
        // The page laid back over the clip's lower part, as in AppleMusicMainView: a mask never
        // reaches the video surface.
        if (clipFrameHeight > 0.dp) {
            AppleMusicMeshBackdrop(
                mesh = mesh,
                seam = clipFrameHeight,
                modifier = Modifier.appleMusicFadeBetween(transparentAt = clipFrameHeight - artworkFade, opaqueAt = clipFrameHeight),
            )
        }
        // The wash under the top bar, held through the status bar and eased out by the band's end:
        // the bar's title sits inside it, so it cannot fall off as fast as the grabber's does.
        val washAlpha by animateFloatAsState(
            targetValue = TOP_WASH_MIN_ALPHA + (TOP_WASH_MAX_ALPHA - TOP_WASH_MIN_ALPHA) * (mesh?.topLuminance ?: 1f),
            animationSpec = tween(TOP_WASH_FADE_MS),
            label = "animatedArtworkTopWash",
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(topBand)
                    .background(
                        smoothScrimBrush(
                            from = Color.Black.copy(alpha = washAlpha),
                            to = Color.Black.copy(alpha = 0f),
                            startFraction = statusBarHeight / topBand,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        smoothScrimBrush(
                            from = pageColor.copy(alpha = 0f),
                            to = pageColor,
                            startFraction = PAGE_COLOR_FADE_START,
                        ),
                    ),
        )
    }
}

/**
 * Apple Music's animated artwork in a frame of its own shape, full width and top-aligned, so none
 * of it is cropped. The clip's still sits under it: it sizes the frame (its proportions ARE the
 * clip's) and shows while the clip loads, so the frame never opens as a black box. [onStill] hands
 * the still and the frame's height back for the page's mesh, and for the copy of the page that
 * AppleMusicMainView lays over the frame's lower part — the frame itself carries no fade mask,
 * because a mask never reaches the video surface.
 */
@Composable
private fun BoxScope.AnimatedArtworkFrame(
    canvas: NowPlayingScreenData.CanvasData,
    heightLimit: Dp,
    topBandSolid: Dp,
    topBand: Dp,
    alpha: Float,
    onStill: (ImageBitmap?, Dp) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        var aspect by remember(canvas.url) { mutableFloatStateOf(DEFAULT_ANIMATED_ARTWORK_ASPECT) }
        val frameWidth = maxWidth
        val frameHeight = (frameWidth / aspect).coerceAtMost(heightLimit)
        // No still to wait for (an older cached row): show the frame straight away.
        LaunchedEffect(canvas.url, canvas.thumbUrl) {
            if (canvas.thumbUrl == null) onStill(null, frameHeight)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(frameHeight)
                    .alpha(alpha),
        ) {
            val stillRequest =
                canvas.thumbUrl?.let { still ->
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(still)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(still)
                        .build()
                }
            stillRequest?.let { request ->
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        val image = it.result.image
                        if (image.width > 0 && image.height > 0) aspect = image.width.toFloat() / image.height
                        onStill(image.toImageBitmap(), (frameWidth / aspect).coerceAtMost(heightLimit))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // The frame already has the clip's own shape, so "crop to bounds" crops nothing; it is
            // the path that sizes the surface from Media3's own presentation state (see the canvas
            // note in AppleMusicArtworkPage) and keeps a shutter up until the first frame is ready.
            MediaPlayerView(
                url = canvas.url,
                cropToBounds = true,
                modifier = Modifier.fillMaxSize(),
            )
            // Nothing can blur the video surface itself, so its band is the still's, blurred: at 32dp
            // it is a wash of the clip's colours, which barely move in the strip under the status bar.
            stillRequest?.let { TopBandBlur(model = it, solidTo = topBandSolid, fadeTo = topBand) }
        }
    }
}

/**
 * The band under the status bar and the grabber, blurred: a sharp, busy sleeve there fights their
 * glyphs. A Modifier.blur copy of the picture, solid down to [solidTo] and gone by [fadeTo] — the
 * artist page's technique. Below Android 12 blur is a no-op, the copy is then pixel-identical to
 * the picture under it, and only the page's dark wash remains. Nothing when [fadeTo] is zero.
 */
@Composable
private fun TopBandBlur(
    model: Any?,
    solidTo: Dp,
    fadeTo: Dp,
    modifier: Modifier = Modifier,
) {
    if (fadeTo <= 0.dp) return
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .fillMaxSize()
                .appleMusicFadeBetween(transparentAt = fadeTo, opaqueAt = solidTo)
                .blur(TOP_BAND_BLUR),
    )
}

// The status-bar gradient: alpha × (1 − p)^1.5, dark right at the top and falling off fast, drawn
// at 24 stops so no step shows.
private fun topWashBrush(alpha: Float): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(TOP_WASH_STEPS + 1) { i ->
                val p = i / TOP_WASH_STEPS.toFloat()
                p to Color.Black.copy(alpha = alpha * (1f - p).pow(1.5f))
            },
    )

// The top band's length below the status bar, the wash's range (16% on a black sleeve to 65% on a
// white one), its stops, its change on a skip (the mesh's own crossfade), and the band's blur (the
// artist page's).
private val TOP_BAND_STRIP = 32.dp
private const val TOP_WASH_STEPS = 24
private const val TOP_WASH_MIN_ALPHA = 0.16f
private const val TOP_WASH_MAX_ALPHA = 0.65f
private const val TOP_WASH_FADE_MS = 900
private val TOP_BAND_BLUR = 32.dp

// Radius of the frosted cover art behind the page. Large enough that no detail of the artwork
// survives as a shape — what is left is its colour and its broad light and dark areas, which is
// precisely what Apple's background is.
private val BACKDROP_BLUR_RADIUS = 80.dp

// Height of the Android top bar (⌄ · Song/Video · ⋯), right under the status bar. The artwork and
// a tall video frame start below it. Fork: was a 28dp grabber.
private val GRABBER_HEIGHT = APPLE_MUSIC_TOP_BAR_HEIGHT

// Desktop's glass dismiss button in the panel's top-left corner, which stands in for the grabber
// there — a tall video frame starts below it for the same reason.
private val DISMISS_BUTTON_INSET = 12.dp
private val DISMISS_BUTTON_SIZE = 48.dp

// Breathing room between that top chrome and a tall video frame.
private val VIDEO_FRAME_TOP_GAP = 16.dp

// The same room between a tall video frame and the title below it, which it otherwise touches.
private val VIDEO_FRAME_BOTTOM_GAP = 16.dp

// How much of the artwork-derived gradient sits over the frosted art. Enough to darken the page
// towards the bottom so the transport stays readable; not so much that it hides the art again.
private const val BACKDROP_TINT_ALPHA = 0.62f

// The bottom share of the artwork that dissolves into the page.
private const val ARTWORK_FADE_FRACTION = 0.42f

// The top share that dissolves once the cover is moved down off the top of the page.
private const val ARTWORK_TOP_FADE_FRACTION = 0.14f

// The Lyrics/Queue panel: a critically damped spring, so it settles without bouncing and carries
// the release velocity of a flick; how far a slow drag has to move it (as a share of the page) to
// commit; the flick in px/s that commits at any distance; and how dark the player under it gets.
private val PANEL_SPRING = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
private const val PANEL_COMMIT_FRACTION = 0.12f
private const val PANEL_FLING_VELOCITY = 600f
private const val PANEL_SCRIM_ALPHA = 0.45f

// How long after the player opens the hidden Lyrics page is built — past the sheet's slide-in, so
// the two never compete for the same frames.
private const val LYRICS_PREWARM_DELAY_MS = 600L

// The Song / Video pill: each half, its height, and the room left under it before the artwork.
private val VERSION_SWITCH_SEGMENT_WIDTH = 52.dp
private val VERSION_SWITCH_HEIGHT = 36.dp
// A full-width square cover fits a portrait page with room to spare; only a landscape window is
// short enough for this cap to bite.
private const val ARTWORK_MAX_HEIGHT_FRACTION = 0.62f
private const val EFFECTS_IN_MS = 900

// How far below its place the controls block starts when the player grows out of the mini player.
private val CONTROLS_RISE = 160.dp
private const val EFFECTS_DELAY_MS = 150

// A 3:4 animated artwork runs under the title row by design; this only stops a landscape window
// from turning it into the whole page.
private const val ANIMATED_ARTWORK_MAX_HEIGHT_FRACTION = 0.75f

// Apple's tall motion artwork (motionDetailTall), which the artwork source prefers — the frame's
// shape until the clip's still says otherwise.
private const val DEFAULT_ANIMATED_ARTWORK_ASPECT = 3f / 4f

// How long an animated artwork takes to replace the cover once it is ready.
private const val ARTWORK_FADE_IN_MS = 420

// Where, down the first screen, the animated artwork's page starts fading into the page below the
// fold — late enough that the mesh still reaches the controls at the bottom of the screen.
private const val PAGE_COLOR_FADE_START = 0.92f

// How long the LYRICS tab waits for a track's lyrics before deciding the track has none. Long
// enough to cover a normal fetch on a normal connection, short enough that a song with no lyrics
// does not leave the user staring at an empty page. Not a fixed budget for the request itself:
// lyrics arriving at any point cancel the wait outright.
private const val LYRICS_ABSENCE_GRACE_MS = 2_500L

// How long "No lyrics for this song" stays up before the page slides back to the player.
private const val NO_LYRICS_HOLD_MS = 8_000L
