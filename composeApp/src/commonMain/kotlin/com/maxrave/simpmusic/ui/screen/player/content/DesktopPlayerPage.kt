package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.ui.component.DraggableItem
import com.maxrave.simpmusic.ui.component.FullscreenLyricsList
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.rememberDragDropState
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicMeshBackdrop
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicMesh
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.AppleMusicCrossfadeDeck
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.AppleMusicMainTitleRow
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.rememberAppleMusicCrossfadeDeckState
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.rememberCrossfadeMixWord
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.AppleMusicVersionSwitch
import com.maxrave.simpmusic.ui.screen.player.content.nhacnheo.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.queue

/** The right side of [DesktopPlayerPage]. The names are what SharedViewModel.lastPlayerViewTab holds. */
internal enum class DesktopPlayerTab { QUEUE, LYRICS }

// The capsule player floats over the page's bottom: 60dp tall, 20dp above the window's edge.
private val CAPSULE_CLEARANCE = 88.dp

// The page's dismiss button sits above the artwork, in the left column's top margin.
private val LEFT_COLUMN_TOP = 64.dp

// The title row under the artwork, the gap to it, and the Song | Video switch above it.
private val TITLE_ROW_RESERVE = 100.dp
private val VERSION_SWITCH_RESERVE = 48.dp

private const val CONTENT_FADE_MS = 180

// How far in from each edge the cover melts, as a share of its width.
internal const val COVER_MELT_FRACTION = 0.14f
private const val MELT_STEPS = 16

/**
 * Fork: the colours behind Desktop's full-size player: a mesh of the artwork's own colours. Recorded
 * as the glass source for the page's own buttons, which are its siblings.
 */
@Composable
internal fun DesktopPlayerBackdrop(
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
) {
    val screenData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val mesh = rememberAppleMusicMesh(artwork = screenData.bitmap, seed = screenData.thumbnailURL.hashCode())
    Box(modifier = modifier.background(Color.Black)) {
        AppleMusicMeshBackdrop(mesh = mesh, seam = 0.dp, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Fork: Desktop's full-size player: the cover melting into the artwork's colours on the left, Queue
 * and Lyrics as tabs on the right, beside the navigation strip. The transport stays in the capsule,
 * which floats over this page and refracts it. Its > collapses to the small panel beside Home.
 * [backdrop] is the source [DesktopPlayerBackdrop] is recorded into, a sibling of this page. How it
 * arrives and leaves is [DesktopPlayerMotion]'s.
 */
@Composable
internal fun DesktopPlayerPage(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    sharedViewModel: SharedViewModel,
    backdrop: PlatformBackdrop,
) {
    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor == Color.Black) seed else paletteColor
    val lastTab by sharedViewModel.lastPlayerViewTab.collectAsStateWithLifecycle()
    val tab = if (lastTab == DesktopPlayerTab.LYRICS.name) DesktopPlayerTab.LYRICS else DesktopPlayerTab.QUEUE

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                // Home stays composed underneath, and must not take this page's clicks and scrolls.
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(bottom = CAPSULE_CLEARANCE)) {
            DesktopPlayerArtworkColumn(
                state = state,
                actions = actions,
                sharedViewModel = sharedViewModel,
                modifier = Modifier.weight(5f).fillMaxHeight().padding(top = LEFT_COLUMN_TOP, start = 16.dp, end = 16.dp),
            )
            Column(modifier = Modifier.weight(4f).fillMaxHeight().padding(top = 16.dp, end = 24.dp)) {
                DesktopPlayerTabs(
                    selected = tab,
                    onSelect = { sharedViewModel.setLastPlayerViewTab(it.name) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        DesktopPlayerTab.QUEUE -> DesktopPlayerQueue(state = state, actions = actions)

                        DesktopPlayerTab.LYRICS ->
                            FullscreenLyricsList(
                                lyricsData = state.screenData.lyricsData,
                                sharedViewModel = sharedViewModel,
                                color = seedColor,
                                backdrop = backdrop,
                            )
                    }
                }
            }
        }
        LiquidGlassIconButton(
            backdrop = backdrop,
            imageVector = state.dismissIcon,
            // A round button's rim, as on the player's other Desktop glass buttons.
            highlight = Highlight(width = 1.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(48.dp),
            onClick = actions.onDismiss,
        )
    }
}

@Composable
private fun DesktopPlayerArtworkColumn(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
) {
    val typography = rememberAppleMusicTypography()
    val playerShowsVideo by sharedViewModel.playerShowsVideo.collectAsStateWithLifecycle()
    val canvas = state.screenData.canvasData
    val hasVideoVersion = canvas == null && state.screenData.isVideo && state.shouldShowVideo
    val showVideo = hasVideoVersion && playerShowsVideo
    val showCanvas = canvas != null && canvas.isVideo
    // The phone's two-cover crossfade. Only over a still cover, as there.
    val crossfadeDeck =
        rememberAppleMusicCrossfadeDeckState(
            isCrossfading = state.timelineState.isCrossfading,
            title = state.screenData.nowPlayingTitle,
            artist = state.screenData.artistName,
            coverUrl = state.screenData.thumbnailURL,
            enabled = canvas == null && !showVideo,
        )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val ratio = if (showVideo) state.videoAspectRatio else 1f
        val roomHeight = (maxHeight - TITLE_ROW_RESERVE - (if (hasVideoVersion) VERSION_SWITCH_RESERVE else 0.dp)).coerceAtLeast(120.dp)
        // Fits the frame into the column by whichever side runs out first, keeping its shape.
        val frameWidth: Dp = minOf(maxWidth, roomHeight * ratio)
        val frameHeight: Dp = frameWidth / ratio
        Column(
            // Every style row carries its own 20dp side gutters, so a column 40dp wider than the frame
            // lines the title up with the frame's edges. A frame narrower than that width is centred.
            modifier = Modifier.width(minOf(maxWidth, frameWidth + 40.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (hasVideoVersion) {
                AppleMusicVersionSwitch(
                    videoSelected = showVideo,
                    onSelect = { video -> sharedViewModel.setPlayerShowsVideo(video) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Box(
                modifier =
                    Modifier
                        .size(width = frameWidth, height = frameHeight)
                        .onGloballyPositioned { DesktopPlayerMotion.pageCover = it.boundsInWindow() }
                        .graphicsLayer { alpha = DesktopPlayerMotion.coverAlpha() },
            ) {
              Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        // The deck stands in for the cover while two songs fade into each other.
                        .graphicsLayer { alpha = if (crossfadeDeck.active) 0f else 1f }
                        // A video keeps its picture whole; the cover and a canvas melt into the page.
                        .then(if (showVideo) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier.meltEdges(COVER_MELT_FRACTION)),
              ) {
                DesktopPlayerCover(
                    state = state,
                    onBitmap = actions.onArtworkBitmap,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showVideo) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                    MediaPlayerViewWithSubtitle(
                        playerName = MAIN_PLAYER,
                        // fillMaxWidth, never fillMaxSize: a free height is what lets the surface's own
                        // aspect ratio apply instead of stretching the picture.
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        shouldShowSubtitle = false,
                        shouldPip = false,
                        timelineState = state.timelineState,
                        lyricsData = state.screenData.lyricsData?.lyrics,
                        translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                        isInPipMode = state.isInPipMode,
                        mainTextStyle = typo().bodyLarge,
                        translatedTextStyle = typo().bodyMedium,
                    )
                    IconButton(
                        onClick = actions.onEnterFullscreenVideo,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(imageVector = SimpIcons.Fullscreen, contentDescription = "", tint = Color.White)
                    }
                } else if (showCanvas) {
                    var firstFrame by remember(canvas.url) { mutableStateOf(false) }
                    val canvasAlpha by animateFloatAsState(
                        targetValue = if (firstFrame) 1f else 0f,
                        animationSpec = tween(CONTENT_FADE_MS),
                        label = "desktopPlayerCanvas",
                    )
                    MediaPlayerView(
                        url = canvas.url,
                        modifier = Modifier.fillMaxSize().alpha(canvasAlpha),
                        cropToBounds = true,
                        onFirstFrame = { firstFrame = true },
                    )
                }
              }
                // Outside the melted box: its particles and tilted covers must not dissolve with the
                // cover's edges. Drawn with the cover's fade at top and bottom, for the hand-back.
                AppleMusicCrossfadeDeck(
                    state = crossfadeDeck,
                    topFade = frameWidth * COVER_MELT_FRACTION,
                    bottomFade = frameWidth * COVER_MELT_FRACTION,
                )
                val mixWordAlpha by animateFloatAsState(
                    targetValue = if (crossfadeDeck.active && state.timelineState.isCrossfading) 1f else 0f,
                    animationSpec = tween(CONTENT_FADE_MS),
                    label = "desktopPlayerMixWord",
                )
                if (mixWordAlpha > 0f) {
                    Text(
                        text = rememberCrossfadeMixWord(state.screenData.nowPlayingTitle),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).alpha(mixWordAlpha),
                    )
                }
            }
            // The cover's lower edge has already melted away, so the title needs no gap of its own.
            Spacer(modifier = Modifier.height(if (showVideo) 20.dp else 0.dp))
            AppleMusicMainTitleRow(state = state, actions = actions, typography = typography)
        }
    }
}

@Composable
private fun DesktopPlayerCover(
    state: NowPlayingContentState,
    onBitmap: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    var artworkUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(artworkUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(artworkUrl)
                .crossfade(300)
                .build(),
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        // What the backdrop's colours are read from.
        onSuccess = { onBitmap(it.result.image.toImageBitmap()) },
        // maxresdefault is missing for plenty of videos.
        onError = {
            val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
            if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
        },
        modifier = modifier,
    )
}

/**
 * Dissolves all four edges into whatever is behind, by an alpha mask rather than a colour laid over
 * them, so there is nothing to match. On smoothstep, like the phone player's cover: a linear ramp has a
 * corner where it starts, which the eye reads as a line.
 */
internal fun Modifier.meltEdges(widthFraction: Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = size.width * widthFraction
            val stopsAcross = meltStops((fade / size.width).coerceAtMost(0.5f))
            val stopsDown = meltStops((fade / size.height).coerceAtMost(0.5f))
            drawRect(Brush.horizontalGradient(colorStops = stopsAcross), blendMode = BlendMode.DstIn)
            drawRect(Brush.verticalGradient(colorStops = stopsDown), blendMode = BlendMode.DstIn)
        }

private fun meltStops(edge: Float): Array<Pair<Float, Color>> {
    val ramp =
        (0..MELT_STEPS).map { step ->
            val t = step / MELT_STEPS.toFloat()
            t * edge to Color.Black.copy(alpha = t * t * (3f - 2f * t))
        }
    return (ramp + ramp.map { (at, color) -> 1f - at to color }.reversed()).toTypedArray()
}

/** Text tabs with a line under the chosen one. */
@Composable
private fun DesktopPlayerTabs(
    selected: DesktopPlayerTab,
    onSelect: (DesktopPlayerTab) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DesktopPlayerTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        stringResource(
                            when (tab) {
                                DesktopPlayerTab.QUEUE -> Res.string.queue
                                DesktopPlayerTab.LYRICS -> Res.string.lyrics
                            },
                        ).uppercase(),
                    style = typo().labelMedium,
                    color = Color.White.copy(alpha = if (isSelected) 1f else 0.55f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(if (isSelected) 2.dp else 1.dp)
                            .background(Color.White.copy(alpha = if (isSelected) 1f else 0.15f)),
                )
            }
        }
    }
}

/**
 * The whole queue, the current song included and marked, unlike the phone's upcoming-only list, so
 * the songs already played stay a click away. Rows carry their index in the queue directly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopPlayerQueue(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    musicServiceHandler: MediaPlayerHandler = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentOrderIndex.coerceAtLeast(0))
    // Keyed by track, not position: a radio queue drops played tracks off the front, and a position
    // key would change for every row and make the list jump. The occurrence number covers a repeat.
    val rowKeys =
        remember(state.artworkQueue) {
            val seen = HashMap<String, Int>(state.artworkQueue.size)
            state.artworkQueue.map { track ->
                val occurrence = seen.getOrElse(track.videoId) { 0 }
                seen[track.videoId] = occurrence + 1
                "${track.videoId}#$occurrence"
            }
        }
    val dragDropState = rememberDragDropState(listState) { from, to -> actions.onMoveQueueItem(from, to) }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(state.currentOrderIndex) {
        if (state.currentOrderIndex >= 0) listState.animateScrollToItem(state.currentOrderIndex)
    }
    // Endless and radio queues page in as you scroll, as in the queue sheet.
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastVisible.index >= layoutInfo.totalItemsCount - 3 && layoutInfo.totalItemsCount > 0
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }.collect {
            if (it && musicServiceHandler.queueData.value?.queueState == QueueData.StateSource.STATE_INITIALIZED) {
                musicServiceHandler.loadMore()
            }
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, dragOffset ->
                            change.consume()
                            dragDropState.onDrag(offset = dragOffset)
                            if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress
                            dragDropState
                                .checkForOverScroll()
                                .takeIf { it != 0f }
                                ?.let { delta ->
                                    overscrollJob = coroutineScope.launch { dragDropState.state.animateScrollBy(delta * 1.3f) }
                                } ?: run { overscrollJob?.cancel() }
                        },
                        onDragStart = { dragDropState.onDragStart(it) },
                        onDragEnd = {
                            dragDropState.onDragInterrupted(true)
                            overscrollJob?.cancel()
                        },
                        onDragCancel = {
                            dragDropState.onDragInterrupted()
                            overscrollJob?.cancel()
                        },
                    )
                },
    ) {
        itemsIndexed(
            state.artworkQueue,
            key = { index, track -> rowKeys.getOrElse(index) { track.videoId } },
        ) { index, track ->
            DraggableItem(dragDropState = dragDropState, index = index, modifier = Modifier) { _ ->
                SongFullWidthItems(
                    track = track,
                    isPlaying = index == state.currentOrderIndex,
                    modifier = Modifier.fillMaxWidth(),
                    onClickListener = { videoId ->
                        if (videoId == track.videoId) actions.onSeekToQueueIndex(index)
                    },
                    onMoreClickListener = { actions.onQueueItemMore(index, track) },
                )
            }
        }
    }
}
