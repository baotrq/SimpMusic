package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.compose.LocalPlatformContext
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Fork: what the mini player and the player tell each other so the one can turn into the other,
 * YouTube Music style. All bounds are in root coordinates, which the in-window player shares.
 */
internal object MiniPlayerAnchor {
    /** The mini player card, which the player grows out of. */
    var bounds by mutableStateOf<Rect?>(null)

    /** The cover in the mini player, its URL and its corner radius as a share of its width. */
    var artBounds by mutableStateOf<Rect?>(null)
    var artUrl by mutableStateOf<String?>(null)
    var artCornerFraction = 0.1f

    /** The bar's corner radius in px, or null for a pill (half its height). */
    var barCornerPx: Float? = null

    /** The bar's glass brightness, so a glass frame growing out of it matches it. */
    var glassLuminance by mutableStateOf<State<Float>?>(null)

    /**
     * A live recording of the bar's contents (text, buttons), drawn at the top of the growing frame
     * so the bar itself appears to grow rather than a copy of it.
     */
    var barFace by mutableStateOf<GraphicsLayer?>(null)

    /** Where the open player draws its cover, and from which URL (Apple Music style only), or null. */
    var playerArt by mutableStateOf<Rect?>(null)
    var playerArtUrl by mutableStateOf<String?>(null)

    /** A finger is on the mini player: the player is built, hidden, ahead of the tap. */
    var pressed by mutableStateOf(false)

    /** Set while a finger drags the mini player up: 1 = still the bar, 0 = fully open. */
    var dragFraction by mutableStateOf<Float?>(null)

    /** A drag let go short of opening: the player sinks back into the bar, then goes away. */
    var returning by mutableStateOf(false)

    /** Speed the drag was let go at, in progress per second, for the opening to carry on from. */
    var releaseVelocity = 0f

    /** True while the player fully covers the window, so the app under it can skip drawing. */
    var coversApp by mutableStateOf(false)

    /**
     * The in-window player's progress (0 open, 1 the bar), or null when there is none. Read inside
     * draw lambdas so they follow it frame by frame.
     */
    var progress by mutableStateOf<(() -> Float)?>(null)

    /**
     * Alpha for the player's own cover: hidden until the travelling one has fully landed, then shown
     * in the same frame it disappears. The two are the same picture in the same place by then, so
     * the swap is invisible; crossfading them while the travelling one was still closing in drew
     * both, a step apart.
     */
    fun coverHandoffAlpha(): Float {
        val p = progress?.invoke() ?: return 1f
        return if (playerArt == null || p <= 0f) 1f else 0f
    }

    /**
     * Alpha for the bar itself: hidden the moment the player starts growing out of it. The frame
     * starts out identical to it (its glass or colour, plus [barFace]), so it takes its place.
     */
    fun barAlpha(): Float {
        val p = progress?.invoke() ?: return 1f
        return if (p < 1f) 0f else 1f
    }

    /** How far below its place the player's controls block starts when the player grows from the bar. */
    var controlsSlidePx = 0f

    /** The controls block's offset at the current progress: rides in with the frame. */
    fun controlsOffset(): Float {
        val p = progress?.invoke() ?: return 0f
        return controlsSlidePx * p
    }

    /** Alpha for the bar's own cover: hidden while the travelling cover is away from it. */
    fun barCoverAlpha(): Float {
        val p = progress?.invoke() ?: return 1f
        return if (playerArt != null && p < 1f) 0f else 1f
    }
}

// Softer than Spring.StiffnessMediumLow: at that stiffness the cover covered most of its way in the
// first few frames and then stopped hard, which read as a snap rather than one glide.
private const val SHEET_STIFFNESS = Spring.StiffnessLow

// The spring ends once within this much progress of its target and jumps the rest. The default
// (0.01) is 1% of the frame's travel, ~20px on a phone: the frame snapped the last bit into the bar.
private val sheetSpring =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SHEET_STIFFNESS, visibilityThreshold = 0.0002f)

// Frames to let a player that was not built ahead finish building before it starts to grow.
private const val BUILD_FRAMES = 4

/**
 * The player inside the app's window. It grows out of the mini player and shrinks back into it,
 * following the finger either way, while the cover travels between the two; with no mini player it
 * slides from below. While [open] is false it is composed but invisible unless a drag on the mini
 * player is pulling it up.
 */
@Composable
internal fun InWindowNowPlayingSheet(
    onDismiss: () -> Unit,
    open: Boolean = true,
    /** The app's glass source when liquid glass is on: the frame then grows as glass, like the bar. */
    backdrop: PlatformBackdrop? = null,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val position = remember { Animatable(1f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val progress = remember { { if (dragging) dragPosition else position.value } }
    // How far the frame's top edge travels between open and the bar, so a finger and the edge move
    // together.
    val travelPx = { (MiniPlayerAnchor.bounds?.top ?: heightPx).coerceAtLeast(1f) }
    val settle: (Boolean, Float) -> Unit = { dismiss, velocity ->
        animationJob?.cancel()
        animationJob =
            scope.launch {
                if (dragging) position.snapTo(dragPosition)
                dragging = false
                position.animateTo(
                    if (dismiss) 1f else 0f,
                    sheetSpring,
                    initialVelocity = velocity,
                )
                if (dismiss) currentOnDismiss()
            }
    }
    // Composed before it opened (a finger on the bar built it ahead of the tap), or only now.
    val prewarmed = remember { !open }
    LaunchedEffect(open) {
        if (open) {
            // Not built ahead (a tap on a song elsewhere): let the player build while still, so the
            // growth runs smoothly instead of stalling a few frames in.
            if (!prewarmed) repeat(BUILD_FRAMES) { withFrameNanos { } }
            settle(false, MiniPlayerAnchor.releaseVelocity)
            MiniPlayerAnchor.releaseVelocity = 0f
        }
    }
    // A drag on the mini player moves the frame directly.
    LaunchedEffect(Unit) {
        snapshotFlow { MiniPlayerAnchor.dragFraction }.collect { fraction ->
            if (fraction != null && !open) {
                animationJob?.cancel()
                position.snapTo(fraction)
            }
        }
    }
    LaunchedEffect(MiniPlayerAnchor.returning) {
        if (MiniPlayerAnchor.returning && !open) {
            animationJob?.cancel()
            position.animateTo(1f, sheetSpring)
            MiniPlayerAnchor.returning = false
        }
    }
    LaunchedEffect(open) {
        snapshotFlow { open && !dragging && position.value == 0f }.collect { MiniPlayerAnchor.coversApp = it }
    }
    DisposableEffect(Unit) {
        MiniPlayerAnchor.progress = progress
        onDispose {
            MiniPlayerAnchor.coversApp = false
            MiniPlayerAnchor.progress = null
        }
    }
    val dismiss = { settle(true, 0f) }
    PlatformBackHandler(enabled = open, onBack = dismiss)

    // This detector stays still while only its child moves. Children get first refusal, so a
    // downward pull on Lyrics/Queue returns to the player before another pull dismisses it.
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                widthPx = it.width.toFloat()
                heightPx = it.height.toFloat()
            }.then(
                if (!open) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            val start =
                                awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                                    if (over > 0f && heightPx > 0f) {
                                        animationJob?.cancel()
                                        dragPosition = (position.value + over / travelPx()).coerceIn(0f, 1f)
                                        dragging = true
                                        change.consume()
                                    }
                                } ?: return@awaitEachGesture
                            tracker.addPosition(start.uptimeMillis, start.position)
                            val completed =
                                verticalDrag(start.id) { change ->
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    dragPosition = (dragPosition + change.positionChange().y / travelPx()).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            val velocity = if (completed) tracker.calculateVelocity().y else 0f
                            val shouldDismiss =
                                completed &&
                                    if (velocity < -900f) false else dragPosition > 0.22f || velocity > 900f
                            settle(shouldDismiss, velocity / travelPx())
                        }
                    }
                },
            ),
    ) {
        // Where the player's frame sits at progress p: the full window at 0, the mini player at 1.
        fun frameAt(p: Float): Pair<Rect, Float> {
            val origin =
                MiniPlayerAnchor.bounds?.takeIf { it.height > 0f }
                    ?: Rect(0f, heightPx, widthPx, heightPx * 2)
            return lerp(Rect(0f, 0f, widthPx, heightPx), origin, p) to lerp(0f, MiniPlayerAnchor.barCornerPx ?: (origin.height / 2), p)
        }
        // The frame is a layer moved to the frame's corner and clipped to its outline: a rounded
        // outline clip is native to the renderer, where a drawn Path clip over the whole player
        // tripled its frame time. Fully open, nothing is clipped at all. It starts out as the bar
        // itself (the bar's colour, title and artist, while the real bar hides) and turns into the
        // player as it grows.
        val barColor = MaterialTheme.colorScheme.surfaceContainer
        if (backdrop != null) {
            GlassFrame(backdrop, progress) { p -> frameAt(p) }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    val (frame, radius) = frameAt(p)
                    alpha = if (p >= 1f) 0f else 1f
                    translationX = frame.left
                    translationY = frame.top
                    clip = p > 0f
                    shape = FrameShape(frame.width, frame.height, radius)
                }.drawBehind {
                    val toBar = ((progress() - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    // Over glass the bar end is see-through, so the glass under it shows.
                    drawRect(
                        if (backdrop != null) {
                            Color.Black.copy(alpha = 1f - toBar)
                        } else {
                            androidx.compose.ui.graphics.lerp(Color.Black, barColor, toBar)
                        },
                    )
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The page stays in its final place and the growing frame uncovers it, YouTube
                        // Music style: only the frame and the travelling cover move, so it reads as
                        // one motion, and the page's own cover is already where the travelling one
                        // lands. It fades in over the first half of the growth, out over the last.
                        val p = progress()
                        val frame = frameAt(p).first
                        translationX = -frame.left
                        translationY = -frame.top
                        alpha = ((1f - p) / 0.5f).coerceIn(0f, 1f)
                    },
            ) {
                content(dismiss)
            }
            BarFace(barAlpha = { ((progress() - 0.7f) / 0.3f).coerceIn(0f, 1f) })
        }

        // The cover travelling to the player's cover spot from the bar. Re-laid out at its in-between rectangle each frame, so a wide video thumbnail
        // reshapes into the square cover instead of being stretched.
        val target = MiniPlayerAnchor.playerArt
        val from = MiniPlayerAnchor.artBounds
        // The player's own cover image (same request, so same cache entry and pixels), so handing
        // over changes nothing.
        val playerUrl = MiniPlayerAnchor.playerArtUrl
        val url = playerUrl ?: MiniPlayerAnchor.artUrl
        val fromCorner = MiniPlayerAnchor.artCornerFraction
        if (target != null && from != null && url != null && target.width > 0f) {
            val cornerShape =
                remember(from, fromCorner) {
                    RoundedCornerShape(ProgressCornerSize { lerp(0f, fromCorner * minOf(from.width, from.height), progress()) })
                }
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .apply { if (playerUrl != null) diskCacheKey(playerUrl + "BIGGER") }
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .layout { measurable, constraints ->
                            val r = lerp(target, from, progress())
                            val placeable =
                                measurable.measure(
                                    Constraints.fixed(r.width.roundToInt().coerceAtLeast(1), r.height.roundToInt().coerceAtLeast(1)),
                                )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(r.left.roundToInt(), r.top.roundToInt())
                            }
                        }.graphicsLayer {
                            val p = progress()
                            alpha = if (p >= 1f || p <= 0f) 0f else 1f
                            shape = cornerShape
                            clip = true
                        },
            )
        }
    }
}

/**
 * Liquid glass under the growing frame, so a glass bar grows as glass. Laid out at the frame's
 * rectangle each frame (the glass samples what is behind it by position, so it cannot be moved by a
 * layer transform), and dropped once the opaque player has covered it.
 */
@Composable
private fun GlassFrame(
    backdrop: PlatformBackdrop,
    progress: () -> Float,
    frameAt: (Float) -> Pair<Rect, Float>,
) {
    val layer = rememberGraphicsLayer()
    val luminance = MiniPlayerAnchor.glassLuminance ?: remember { mutableFloatStateOf(0.5f) }
    // A RoundedCornerShape, not a custom Shape: the glass lens accepts only corner-based shapes.
    val shape = remember { RoundedCornerShape(ProgressCornerSize { frameAt(progress()).second }) }
    Box(
        Modifier
            .layout { measurable, constraints ->
                val frame = frameAt(progress()).first
                val placeable =
                    measurable.measure(
                        Constraints.fixed(frame.width.roundToInt().coerceAtLeast(0), frame.height.roundToInt().coerceAtLeast(0)),
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(frame.left.roundToInt(), frame.top.roundToInt())
                }
            }.graphicsLayer {
                val p = progress()
                alpha = if (p < 0.4f || p >= 1f) 0f else 1f
            }.liquidGlass(backdrop, layer, luminance, shape = shape, interactive = false),
    )
}

// A corner radius read when the shape is drawn, so it follows the frame without recomposing.
private class ProgressCornerSize(
    private val radius: () -> Float,
) : CornerSize {
    override fun toPx(
        shapeSize: Size,
        density: Density,
    ): Float = radius()
}

/** The bar's own contents, at the top-left of the growing frame, fading out as it grows. */
@Composable
private fun BarFace(barAlpha: () -> Float) {
    val bar = MiniPlayerAnchor.bounds ?: return
    val face = MiniPlayerAnchor.barFace ?: return
    with(LocalDensity.current) {
        Box(
            Modifier
                .size(bar.width.toDp(), bar.height.toDp())
                .graphicsLayer { alpha = barAlpha() }
                .drawBehind { drawLayer(face) },
        )
    }
}

private class FrameShape(
    private val width: Float,
    private val height: Float,
    private val radius: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Rounded(RoundRect(0f, 0f, width, height, CornerRadius(radius)))
}
