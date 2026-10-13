package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.FastForward
import com.maxrave.simpmusic.ui.icon.FastRewind
import com.maxrave.simpmusic.ui.icon.KeyboardArrowUp
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.VolumeDown
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.live_badge
import simpmusic.composeapp.generated.resources.lyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

/** Which body the dock is currently showing. Held by the top-level Apple Music composable. */
internal enum class AppleMusicView { MAIN, LYRICS, QUEUE }

/**
 * The page gradient's colour at [fraction] of the screen height: the seed barely darkened at the
 * top, ~32%-darkened by mid-page (48%), ~78%-darkened at the bottom. Used to BUILD the gradient
 * and to land the artwork fade on the gradient's own colour at the exact Y where it ends.
 */
internal fun appleMusicGradientColorAt(
    seedColor: Color,
    fraction: Float,
): Color {
    // Every stop is the SEED darkened, never a fixed near-black: the old bottom stop was a
    // hardcoded warm black, so on the artwork view — where only the lower half of the gradient
    // is visible under the artwork — the page read as plain black instead of tinted.
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

internal val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)

// Artist, times and captions.
internal val AppleMusicSecondaryText = Color.White.copy(alpha = 0.55f)
internal val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)
internal val AppleMusicTrackInactive = Color.White.copy(alpha = 0.26f)
internal val AppleMusicTrackActive = Color.White.copy(alpha = 0.92f)

@Immutable
internal data class AppleMusicTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val queueSectionHeader: TextStyle,
    val queueSectionSubtitle: TextStyle,
    val times: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
    val lyricStrip: TextStyle,
)

/**
 * Maps every Apple Music text slot 1:1 onto the [typo] roles the OTHER styles use for the same
 * element — no custom sizes, no custom scaling (owner's rule: this style types like the rest of
 * the app). Precedents: M3E's track title/artist row (titleMedium/bodyMedium), LyricsView's
 * in-player line (headlineMedium), SongFullWidthItems rows (titleSmall/bodySmall), the queue
 * sheet's section headers (titleMedium) and M3E's canvas overlay lines (bodyMedium white/yellow).
 *
 * Secondary text is a translucent white rather than typo()'s body grey: an opaque
 * #A8A8A8 is darker than a bright sleeve's page and read as a dark grey smudge on it, where a
 * translucent white stays lighter than whatever it sits on.
 */
@Composable
internal fun rememberAppleMusicTypography(): AppleMusicTypography {
    val t = typo()
    return AppleMusicTypography(
        // Fork: one step up from the app's 18/13, which read small on the player.
        mainTitle = t.titleMedium.copy(fontSize = 22.sp),
        mainArtist = t.bodyMedium.copy(fontSize = 15.sp, color = AppleMusicSecondaryText),
        // The compact header — Lyrics, Queue, and the row under a running canvas — carries the
        // SIZES FullscreenLyricsSheet uses for the same job (LyricsView.kt: labelSmall over
        // bodySmall), while mainTitle/mainArtist stay 18/13 because they head the controller
        // layout. Borrow the size from labelSmall but keep the titleMedium ROLE: typo() bakes a
        // color into every role — title* carry titleColor, body*/label* carry bodyColor — so
        // switching the title to labelSmall outright would also switch it to the subtitle's grey.
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall.copy(color = AppleMusicSecondaryText),
        queueSectionHeader = t.titleMedium,
        queueSectionSubtitle = t.bodySmall.copy(color = AppleMusicSecondaryText),
        times = t.bodyMedium.copy(color = AppleMusicSecondaryText),
        footer = t.bodySmall.copy(color = AppleMusicSecondaryText),
        idleLyric = t.bodyMedium.copy(color = Color.White),
        idleTranslated = t.bodyMedium.copy(color = Color.Yellow),
        // The line over the progress bar is the inline lyric line Classic and M3E draw under their
        // artwork: labelSmall in white, the same role and colour.
        lyricStrip = t.labelSmall.copy(color = Color.White),
    )
}

/**
 * True alpha fade at the top/bottom edges of a scrolling region (DstIn mask): content dissolves
 * into whatever is behind it — the missing "scrim" on the lyrics list's hard-clipped edges —
 * without painting a color and without touching the wrapped component.
 *
 * Both ramps follow smoothstep, the curve [com.maxrave.simpmusic.extension.smoothScrimBrush] uses and
 * for the reason it gives: a linear ramp has a corner where it leaves 0, and the eye reads that corner
 * as an edge — on the artwork it was a visible line where the picture started to dissolve.
 */
internal fun Modifier.appleMusicVerticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush = Brush.verticalGradient(colorStops = fadeEdgeStops(topStop, bottomStop)),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Transparent at [transparentAt] and beyond it, opaque at [opaqueAt] and beyond it, on the same
 * smoothstep curve as [appleMusicVerticalFadeEdges] — a fade placed anywhere in the element rather
 * than at its edge, in either direction.
 */
internal fun Modifier.appleMusicFadeBetween(
    transparentAt: Dp,
    opaqueAt: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops = fadeEdgeStops(1f, 1f),
                        startY = transparentAt.toPx(),
                        endY = opaqueAt.toPx(),
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

private const val FADE_EDGE_STEPS = 24

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

// Transparent → opaque over [0, topStop], held, then opaque → transparent over [bottomStop, 1]. A
// zero-length ramp is skipped rather than drawn as a pile of stops at one position.
private fun fadeEdgeStops(
    topStop: Float,
    bottomStop: Float,
): Array<Pair<Float, Color>> =
    buildList {
        if (topStop > 0f) {
            for (i in 0..FADE_EDGE_STEPS) {
                val t = i / FADE_EDGE_STEPS.toFloat()
                add(topStop * t to Color.Black.copy(alpha = smoothstep(t)))
            }
        } else {
            add(0f to Color.Black)
        }
        if (bottomStop < 1f) {
            for (i in 0..FADE_EDGE_STEPS) {
                val t = i / FADE_EDGE_STEPS.toFloat()
                add(bottomStop + (1f - bottomStop) * t to Color.Black.copy(alpha = 1f - smoothstep(t)))
            }
        } else {
            add(1f to Color.Black)
        }
    }.toTypedArray()

/**
 * Press-to-swell ("phồng to ra") like the liquid-glass buttons: the control springs up while a
 * finger is on it and settles back on release.
 *
 * Detection is a plain awaitEachGesture down/up watch, NOT GlassInteraction's drag inspector —
 * that one only starts its animation from a DRAG start, so a normal tap on these buttons never
 * animated anything. `requireUnconsumed = false` keeps it working under the wrapped `clickable`,
 * and nothing here consumes events, so clicks and slider drags still land.
 */
@Composable
internal fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * Fork: the Lyrics/Queue panel's drag, split in two. A page only CLAIMS a gesture ([onClaim]) and
 * keeps the finger to itself while it lasts; the shell follows the finger and measures the flick
 * from a layer that does not move ([appleMusicPanelFingerTracker]).
 *
 * The split is the fix, not a tidy-up. The pages slide with the panel, so a finger measured in a
 * page's own coordinates moves only by however much the page did NOT move under it — about half.
 * Drags came out half as long and every flick read as slow, so a quick swipe down bounced back and
 * closing took a drag from the top of the screen to the bottom.
 */
@Stable
internal class AppleMusicPanelDrag(
    val onClaim: () -> Unit,
    val isClaimed: () -> Boolean,
)

/**
 * Fork: claims a vertical drag in ONE direction for the panel, without taking the other direction
 * away from the bottom sheet the player lives in. Only a drag that crosses touch slop in the
 * claimed direction — and more vertically than sideways — is consumed, so the sheet does not move
 * with it; a drag the other way is left alone and still drags the sheet as before. Once claimed,
 * the finger is kept from everything under this page until it lifts; the shell does the moving.
 *
 * [intercept] watches the INITIAL pass, ahead of every child: the player page uses it so a swipe
 * up that is slightly diagonal is not taken by the artwork pager first, and the Queue page while
 * its list is at the top. Otherwise children go first and a list's pull past its top reaches the
 * panel through [rememberAppleMusicPullDownConnection].
 */
@Composable
internal fun Modifier.appleMusicDirectionalDrag(
    enabled: Boolean,
    up: Boolean,
    drag: AppleMusicPanelDrag,
    onStart: () -> Unit = {},
    intercept: Boolean = false,
): Modifier {
    val currentDrag by rememberUpdatedState(drag)
    val currentOnStart by rememberUpdatedState(onStart)
    if (!enabled) return this
    return pointerInput(up, intercept) {
        val pass = if (intercept) PointerEventPass.Initial else PointerEventPass.Main
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            val pointerId = down.id
            val slop = viewConfiguration.touchSlop
            var dx = 0f
            var dy = 0f
            // Until the slop is crossed: claim only a mostly-vertical move in our direction.
            while (true) {
                val change = awaitPointerEvent(pass).changes.firstOrNull { it.id == pointerId } ?: return@awaitEachGesture
                if (!change.pressed) return@awaitEachGesture
                if (!intercept && change.isConsumed) return@awaitEachGesture
                val step = change.positionChange()
                dx += step.x
                dy += step.y
                if (abs(dx) > slop && abs(dx) >= abs(dy)) return@awaitEachGesture
                if (abs(dy) > slop) {
                    // Held still past a long press first: that is the queue's drag-to-reorder (or
                    // a press on a lyric), not a swipe.
                    if (change.uptimeMillis - down.uptimeMillis > viewConfiguration.longPressTimeoutMillis) return@awaitEachGesture
                    if ((dy < 0f) == up && abs(dy) > abs(dx)) {
                        change.consume()
                        break
                    }
                    return@awaitEachGesture
                }
            }
            currentOnStart()
            currentDrag.onClaim()
            while (true) {
                val change = awaitPointerEvent(pass).changes.firstOrNull { it.id == pointerId } ?: break
                change.consume()
                if (!change.pressed) break
            }
        }
    }
}

/**
 * Fork: a list's pull-down past its top, handed to the panel. The bottom sheet would otherwise
 * take that overscroll and dismiss the whole player; here it claims the gesture for the panel
 * instead, and from then on the list keeps still while the finger moves the panel.
 *
 * Nothing that reaches past the list is ever handed on to the sheet while the panel is up: a quick
 * flick down that scrolls the list to its top used to carry its leftover momentum into the sheet,
 * which settled on that velocity and closed the whole player — straight to the home screen.
 */
@Composable
internal fun rememberAppleMusicPullDownConnection(drag: AppleMusicPanelDrag): NestedScrollConnection {
    val currentDrag by rememberUpdatedState(drag)
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (currentDrag.isClaimed()) available else Offset.Zero

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y <= 0f) return Offset.Zero
                // A finger pulling past the top claims the panel; a fling's leftover is swallowed.
                if (source == NestedScrollSource.UserInput) currentDrag.onClaim()
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity = if (currentDrag.isClaimed()) available else Velocity.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = available
        }
    }
}

/**
 * Fork: the shell's side of [AppleMusicPanelDrag]. Watches every finger on the page in the INITIAL
 * pass without consuming anything, from a layer that never moves, so its positions and its
 * velocity are the finger's own. Once a page has claimed the gesture it hands each step to
 * [onDelta] (px, down positive), and when the finger lifts [onEnd] gets the release velocity in
 * px/s. [onRelease] runs at the end of every gesture, claimed or not, to clear the claim.
 */
internal fun Modifier.appleMusicPanelFingerTracker(
    isClaimed: () -> Boolean,
    onDelta: (Float) -> Unit,
    onEnd: (Float) -> Unit,
    onRelease: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onRelease()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var following = false
            while (true) {
                val change =
                    awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) break
                if (isClaimed()) {
                    following = true
                    onDelta(change.positionChangeIgnoreConsumed().y)
                }
            }
            if (following) onEnd(tracker.calculateVelocity().y)
            onRelease()
        }
    }

// How far the compact header has to be dragged sideways to change track, and the flick that does
// it at any distance.
private val TRACK_SWIPE_DISTANCE = 64.dp
private const val TRACK_SWIPE_VELOCITY = 900f

// How long the header keeps showing a swiped-to song before falling back to the screen data, in
// case the player never reports the change (a failed load, a queue edit in between).
private const val PENDING_TRACK_TIMEOUT_MS = 4_000L

/**
 * Fork: swipe the compact header left for the next track, right for the previous one. The row
 * follows the finger directly and springs back once released, so the gesture feels attached to
 * it; the track change itself is just the transport command. Vertical drags are left alone, so
 * the panel's own swipe down still works from the header.
 */
@Composable
internal fun Modifier.appleMusicTrackSwipe(
    enabled: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier {
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    var shift by remember { mutableFloatStateOf(0f) }
    val settle = remember { Animatable(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    if (!enabled) return this
    // Measure on the stationary parent; translating the detector distorts the finger's velocity.
    return pointerInput(Unit) {
        val threshold = TRACK_SWIPE_DISTANCE.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var travelled = 0f
            var origin = 0f
            val start =
                awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    settleJob?.cancel()
                    origin = shift
                    travelled = over
                    shift = origin + travelled
                } ?: return@awaitEachGesture
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            tracker.addPosition(start.uptimeMillis, start.position)
            val completed =
                horizontalDrag(start.id) { change ->
                    travelled += change.positionChange().x
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    shift = origin + travelled
                }
            if (completed) {
                currentEvent.changes.firstOrNull { it.id == start.id }?.let { up ->
                    tracker.addPosition(up.uptimeMillis, up.position)
                }
            }
            val velocity = if (completed) tracker.calculateVelocity().x else 0f
            val direction =
                when {
                    completed && (travelled < -threshold || velocity < -TRACK_SWIPE_VELOCITY) -> -1f
                    completed && (travelled > threshold || velocity > TRACK_SWIPE_VELOCITY) -> 1f
                    else -> 0f
                }
            settleJob = scope.launch {
                settle.snapTo(shift)
                if (direction != 0f) {
                    if (direction < 0f) currentOnNext() else currentOnPrevious()
                }
                // Continue from the release position. No second swipe or jump to the opposite side.
                settle.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialVelocity = velocity,
                ) { shift = value }
            }
        }
    }.graphicsLayer {
        translationX = shift
        alpha = 1f - (abs(shift) / (size.width.coerceAtLeast(1f) * 0.8f)).coerceIn(0f, 1f)
    }
}

/**
 * One 25dp glyph button with a tight circular ripple — [IconButton] sized and clipped exactly
 * like Classic/M3E's plain icon buttons (Info, PlaylistAdd, Queue, Replay5…), not a custom
 * Box+clickable and not an unconstrained [com.maxrave.simpmusic.ui.component.RippleIconButton].
 */
@Composable
internal fun AppleMusicGlyphButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.appleMusicPressInflate().size(size).clip(CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = tint)
    }
}

/**
 * Fork: ⊕ (YouTube-liked) and ☆ (favourite) removed — favourite still lives in the ⋯ sheet.
 * [showPlayPause] puts a play/pause in their place on the LYRICS/QUEUE compact header; the MAIN
 * title row leaves it off since the big transport is right below it.
 */
@Composable
internal fun AppleMusicHeaderActions(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
    showPlayPause: Boolean = false,
    // Fork: off on the Android player, where ⋯ sits in the top bar instead.
    showMore: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showPlayPause) {
            Box(
                modifier =
                    Modifier
                        .appleMusicPressInflate()
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { actions.onUIEvent(UIEvent.PlayPause) },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = state.controllerState.isPlaying, label = "appleMusicHeaderPlayPause") { isPlaying ->
                    Icon(
                        imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
        if (showMore) AppleMusicGlyphButton(icon = SimpIcons.MoreVert, onClick = { actions.onShowMoreSheet() })
    }
}

/**
 * 56dp rounded thumbnail + title/artist column + [AppleMusicHeaderActions], used where the
 * artwork isn't on screen (LYRICS and QUEUE bodies).
 */
@Composable
internal fun AppleMusicCompactHeader(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
    // Fork: the Lyrics page lets the header be swiped sideways to change track.
    swipeToChangeTrack: Boolean = false,
) {
    // Fork: the song a swipe has just asked for, read straight off the queue. The player takes a
    // moment to switch, so without this the row slid back in still showing the OLD song and then
    // jumped to the new one a beat later — which read as a second swipe. It is shown until the
    // screen data catches up with it (or gives up after a few seconds).
    var pendingIndex by remember { mutableStateOf<Int?>(null) }
    var titleAtSwipe by remember { mutableStateOf("") }
    val pendingTrack = pendingIndex?.let { state.artworkQueue.getOrNull(it) }
    LaunchedEffect(state.screenData.nowPlayingTitle, state.currentOrderIndex) {
        if (pendingIndex != null && state.currentOrderIndex == pendingIndex && state.screenData.nowPlayingTitle != titleAtSwipe) {
            pendingIndex = null
        }
    }
    LaunchedEffect(pendingIndex) {
        if (pendingIndex != null) {
            delay(PENDING_TRACK_TIMEOUT_MS)
            pendingIndex = null
        }
    }
    val swipeTo: (Int) -> Unit = { step ->
        val target = (pendingIndex ?: state.currentOrderIndex) + step
        if (state.artworkQueue.getOrNull(target) != null) {
            if (pendingIndex == null) titleAtSwipe = state.screenData.nowPlayingTitle
            pendingIndex = target
        }
        actions.onUIEvent(if (step > 0) UIEvent.Next else UIEvent.SkipToPrevious)
    }
    val title = pendingTrack?.title ?: state.screenData.nowPlayingTitle
    val artist = pendingTrack?.artists?.joinToString { it.name } ?: state.screenData.artistName
    val thumbnail = pendingTrack?.thumbnails?.maxByOrNull { it.width * it.height }?.url ?: state.screenData.thumbnailURL
    // The picture last on screen stands in while the next one loads, instead of the grey holder.
    var lastThumbnail by remember { mutableStateOf<String?>(null) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appleMusicTrackSwipe(
                    enabled = swipeToChangeTrack,
                    onNext = { if (state.controllerState.isNextAvailable) swipeTo(1) },
                    onPrevious = { if (state.controllerState.isPreviousAvailable) swipeTo(-1) },
                ).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumbnail)
                    .placeholderMemoryCacheKey(lastThumbnail)
                    .crossfade(300)
                    .build(),
            onSuccess = { lastThumbnail = thumbnail },
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Ellipsis, not marquee: a marquee in this narrow header scrolls constantly and
            // snapshots as garbage ("Vill Be Okay … Eve" in the first device screenshots).
            Text(
                text = title,
                style = typography.compactTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pendingTrack == null && state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = artist,
                    style = typography.compactArtist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { actions.onNavigateToArtist() },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(
            state = state,
            actions = actions,
            showPlayPause = true,
            showMore = getPlatform() != Platform.Android,
        )
    }
}

/** 8.4dp track shared by the progress bar and the volume row — same visual language, only the color and callbacks differ. */
@Composable
internal fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    // The slider's "phồng" is a track that thickens while touched/dragged — same spring feel as
    // the button inflate, expressed the way a bar can.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    // 20% thicker than the original 7dp/14dp at the owner's request, so the bar is easier to hit.
    // The swollen height still fits the 18dp shells both callers wrap it in.
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 16.8.dp else 8.4.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "appleMusicSliderInflate",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            interactionSource = interactionSource,
            track = {
                // Hand-drawn instead of SliderDefaults.Track. M3 rounds each half of the track with
                // TWO different radii: the OUTER end with trackCornerSize (height/2 — fully round)
                // and the INNER end, where the two halves meet, with trackInsideCornerSize (2dp —
                // reads as square). At value 0 the inactive half owns the entire bar, so its square
                // inner end lands on the LEFT while its round outer end sits on the right — the
                // mismatched bar. Clipping ONE container and drawing the played portion inside it
                // keeps both ends of the bar equally round, and gives the played portion the flat
                // right edge Apple's own bar has.
                val fraction = value.coerceIn(0f, 1f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppleMusicTrackInactive),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(activeColor),
                    )
                }
            },
            thumb = {
                // No thumb — the approved mock and Apple's own bars are track-only; the
                // active/inactive split marks the position (same call the MiniPlayer makes).
                Spacer(Modifier.size(0.dp))
            },
        )
    }
}

/** Elapsed time, the "Crossfading" label while crossfading, and the remaining time as "-m:ss". */
@Composable
internal fun AppleMusicTimesRow(
    state: NowPlayingContentState,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    // `total` stays -1 until the player reports a duration, and SimpleMediaState.Ready carries
    // ONLY the duration — never a position. After a queue restore nothing is playing, and the
    // position poll only runs while isPlaying, so no later event arrives to correct it. Deriving
    // elapsed from total therefore zeroed BOTH numbers at once, which is why a restored queue read
    // 00:00 / -00:00: the played time was never actually unknown, TimeLine.current held it.
    val knownTotal = state.timelineState.total.takeIf { it > 0L }
    val elapsedMs =
        if (knownTotal != null) {
            // Still derived from the slider while the duration IS known, so this number tracks the
            // finger while scrubbing instead of waiting for the player to report the seek back.
            (knownTotal * (state.sliderValue / 100f)).roundToLong()
        } else {
            state.timelineState.current.coerceAtLeast(0L)
        }
    // Clamp BEFORE formatDuration — it renders any negative as "--:--", and the remaining time
    // must never show that at the end of a track whose length IS known. An UNKNOWN length is
    // exactly what that string is for, so null deliberately takes the negative path below.
    val remainingMs = knownTotal?.let { (it - elapsedMs).coerceAtLeast(0L) }
    // A Box, not a three-way weighted Row: the quality line ("154 kbps · 48 kHz") is far wider than
    // the codec pill it replaced, and a third of the row wrapped it onto two lines. Pinned to the
    // row's own centre it has everything between the two times, and a minute rolling over on
    // either side cannot nudge it.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                // Blank for a live broadcast: where it sits in the broadcast's seek window means nothing.
                text = if (state.timelineState.isLive) "" else formatDuration(elapsedMs),
                style = typography.times,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Left,
            )
            Text(
                // No leading "-" when the length is unknown: "---:--" reads as a negative amount of
                // nothing. formatDuration's own out-of-range string is the app's established way to
                // say "no value here".
                text =
                    if (state.timelineState.isLive) {
                        stringResource(Res.string.live_badge)
                    } else {
                        remainingMs?.let { "-" + formatDuration(it) } ?: formatDuration(-1L)
                    },
                style = typography.times,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Right,
            )
        }
        // ONE slot holding two alternatives — and both of them stay composed, swapping on alpha
        // rather than on presence. AnimatedVisibility removes its content from the LAYOUT, so the
        // slot would take the height of whichever state was up, and with NEITHER showing (no
        // quality known, not crossfading) it would collapse to nothing — every swap resizing this
        // row and shoving the transport below it. Holding both keeps the height fixed. The
        // cross-fade looks identical — alpha is what fadeIn/fadeOut animated anyway.
        Box(contentAlignment = Alignment.Center) {
            // Sweep head for the "Crossfading" shimmer, 0..1. Runs UNCONDITIONALLY — put behind the
            // crossfade check it would restart from zero every time the label appears, which is the
            // same reason the other two styles declare it outside their own visibility gate.
            val sweepTransition = rememberInfiniteTransition(label = "appleMusicCrossfadeSweep")
            val crossfadeSweep by sweepTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(3200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "appleMusicSweepHead",
            )
            val crossfadeLabelAlpha by animateFloatAsState(
                targetValue = if (state.timelineState.isCrossfading) 1f else 0f,
                label = "appleMusicCrossfadeLabelAlpha",
            )
            Box(modifier = Modifier.alpha(crossfadeLabelAlpha)) {
                // Identical treatment to Classic and M3 Expressive: a highlight swept through the
                // glyphs with a text brush — no overlay, no clipping.
                val shimmerSpan = 140f
                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                val labelColor = typography.times.color
                Text(
                    text = stringResource(Res.string.crossfading),
                    style =
                        typography.times.copy(
                            brush =
                                Brush.horizontalGradient(
                                    0f to labelColor.copy(alpha = 0.45f),
                                    // The sweep head is PURE white, not the resting label colour — that
                                    // colour is an adaptive grey, and a grey gleam reads as no gleam.
                                    0.5f to Color.White,
                                    1f to labelColor.copy(alpha = 0.45f),
                                    startX = shimmerHead,
                                    endX = shimmerHead + shimmerSpan,
                                    tileMode = TileMode.Clamp,
                                ),
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            // Fork: the "259 kbps · 44.1 kHz" quality line that shared this slot is gone.
        }
    }
}

/** FastRewind(44dp) → Previous, Play/Pause(62dp, plain white — no container disc), FastForward(44dp) → Next. */
@Composable
internal fun AppleMusicTransportRow(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
    // Only the fullscreen lyrics page asks for these; the Now Playing page keeps its three buttons.
    showShuffleAndRepeat: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Mock: a tight centered cluster with a 58dp gap — NOT SpaceEvenly, which spreads the
        // rewind/forward glyphs to the screen edges (first device screenshots). With shuffle and
        // repeat on the two ends the five span the row instead, the way Apple's desktop player
        // lays them out — five buttons at a 58dp gap no longer fit beside the artwork.
        horizontalArrangement =
            if (showShuffleAndRepeat) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showShuffleAndRepeat) {
            AppleMusicGlyphButton(
                icon = SimpIcons.Shuffle,
                onClick = { onUIEvent(UIEvent.Shuffle) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (controllerState.isShuffle) 1f else 0.4f),
            )
        }
        IconButton(
            onClick = { if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous) },
            modifier = Modifier.appleMusicPressInflate().size(52.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastRewind,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isPreviousAvailable) 1f else 0.4f),
                modifier = Modifier.size(40.dp),
            )
        }
        // No loading state on this button. It is play/pause and nothing else: it stays pressable
        // and keeps showing the transport glyph even while the player is buffering, so the control
        // never disappears out from under a finger reaching for it.
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isPlaying, label = "appleMusicPlayPauseIcon") { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        IconButton(
            onClick = { if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next) },
            modifier = Modifier.appleMusicPressInflate().size(52.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastForward,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isNextAvailable) 1f else 0.4f),
                modifier = Modifier.size(40.dp),
            )
        }
        if (showShuffleAndRepeat) {
            val repeatState = controllerState.repeatState
            AppleMusicGlyphButton(
                icon = if (repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
                onClick = { onUIEvent(UIEvent.Repeat) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (repeatState !is RepeatState.None) 1f else 0.4f),
            )
        }
    }
}

@Composable
internal fun AppleMusicVolumeRow(
    controller: DeviceVolumeController,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = SimpIcons.VolumeDown,
            contentDescription = "",
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = controller.volumeFraction,
                activeColor = AppleMusicTrackActive,
                onValueChange = { controller.setVolumeFraction(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = SimpIcons.VolumeUp,
            contentDescription = "",
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One 44dp dock button: a light circle behind a DARK glyph while [active] (white-on-light was unreadable). */
@Composable
internal fun AppleMusicDockButton(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .appleMusicPressInflate()
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint =
                when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    active -> activeContentColor
                    else -> Color.White.copy(alpha = 0.85f)
                },
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Queue. Fork: the [Output | Listen Together] capsule is gone, and so is the Lyrics button — lyrics
 * open with a swipe up on MAIN (or a tap on the lyric strip) and close with a swipe down. Queue
 * keeps the position it had at the right end.
 */
@Composable
internal fun AppleMusicActionRow(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A quarter of the spare width at each end, measured as if the old capsule were still
        // there, so the button sits where it always did.
        val content = DOCK_BUTTON_SIZE * 2 + CAPSULE_SEGMENT_WIDTH * 2 + 1.dp
        val edgeInset = ((maxWidth - content) / 4).coerceAtLeast(0.dp)
        // Desktop keeps the Lyrics button: a mouse has no natural swipe up or down.
        val showLyricsButton = getPlatform() == Platform.Desktop
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = edgeInset),
            horizontalArrangement = if (showLyricsButton) Arrangement.SpaceBetween else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showLyricsButton) {
                AppleMusicDockButton(
                    icon = SimpIcons.Lyrics,
                    active = viewState == AppleMusicView.LYRICS,
                    activeColor = activeColor,
                    activeContentColor = activeContentColor,
                    enabled = lyricsAvailable,
                    onClick = {
                        onSelectView(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS)
                    },
                )
            }
            // Re-tapping the active tab returns to MAIN — the dock is a toggle, not one-way nav.
            AppleMusicDockButton(
                icon = SimpIcons.QueueMusic,
                active = viewState == AppleMusicView.QUEUE,
                activeColor = activeColor,
                activeContentColor = activeContentColor,
                onClick = {
                    onSelectView(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE)
                },
            )
        }
    }
}

private val DOCK_BUTTON_SIZE = 40.dp
private val CAPSULE_SEGMENT_WIDTH = 64.dp

/**
 * Progress bar + times + transport — the playback half of [AppleMusicBottomCluster], shared with
 * the fullscreen lyrics landscape layout. Emits straight into the caller's Column; the horizontal
 * gutter belongs to that Column, as it does in the cluster.
 */
@Composable
internal fun ColumnScope.AppleMusicPlaybackControls(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    showShuffleAndRepeat: Boolean = false,
) {
    // Fixed 18dp shell: the track swells on touch, but inside a CONSTANT footprint —
    // otherwise the growing slider re-measures this whole column and the artwork above
    // it visibly jumps. It also gives the bar a real 18dp touch target instead of 8.4dp.
    Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
        AppleMusicThinSlider(
            value = state.sliderValue / 100f,
            activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else AppleMusicTrackActive,
            onValueChange = { actions.onSliderChange(it * 100f) },
            onValueChangeFinished = actions.onSliderChangeFinished,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AppleMusicTimesRow(state = state, typography = typography, modifier = Modifier.padding(top = 6.dp))
    Spacer(modifier = Modifier.height(8.dp))
    AppleMusicTransportRow(
        controllerState = state.controllerState,
        onUIEvent = actions.onUIEvent,
        showShuffleAndRepeat = showShuffleAndRepeat,
    )
}

/**
 * Progress bar + times + transport + action row — the fixed block every Apple Music body (MAIN,
 * LYRICS, QUEUE) renders at the bottom, identically. On Desktop only the action row renders (no
 * slider/transport), matching the `Platform.Android` gate the other two styles use.
 */
@Composable
internal fun AppleMusicBottomCluster(
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
    modifier: Modifier = Modifier,
    topPadding: Dp = 8.dp,
    // Fork: MAIN carries Queue in its title row instead, so the row under the transport goes.
    showActionRow: Boolean = true,
    // Fork: MAIN's "⌃ Lyrics" pill under the transport — where the lyrics are, and a tap to open them.
    onOpenLyrics: (() -> Unit)? = null,
) {
    val localDensity = LocalDensity.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = topPadding)) {
        // Fork: no volume row and no output-name caption. deviceVolumeController, outputName and
        // onOpenOutput are left in the signature, unused, so upstream merges stay small.
        if (getPlatform() == Platform.Android) {
            AppleMusicPlaybackControls(state = state, actions = actions, typography = typography)
            if (showActionRow) Spacer(modifier = Modifier.height(8.dp))
        } else if (showActionRow) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (onOpenLyrics != null) {
            Spacer(modifier = Modifier.height(12.dp))
            // Always laid out, faded in only once the song's lyrics are known to exist, so the block
            // above it does not jump when they arrive a moment after the track starts.
            val hasLyrics = state.screenData.lyricsData != null
            val pillAlpha by animateFloatAsState(if (hasLyrics) 1f else 0f, label = "appleMusicLyricsPill")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .alpha(pillAlpha)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(enabled = hasLyrics, onClick = onOpenLyrics)
                        .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Icon(
                    imageVector = SimpIcons.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.lyrics),
                    style = typography.times.copy(color = Color.White.copy(alpha = 0.85f)),
                )
            }
        }
        if (showActionRow) {
            AppleMusicActionRow(
                viewState = viewState,
                onSelectView = onSelectView,
                lyricsAvailable = state.screenData.lyricsData != null,
                activeColor = activePillContainer,
                activeContentColor = activePillContent,
            )
        }
        Spacer(
            modifier =
                Modifier.height(
                    // Room above the gesture bar. Fork: 36dp → 20dp, for a more compact block.
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 20.dp,
                ),
        )
    }
}

// Fork: height of the Android player's top bar (⌄ · Song/Video · ⋯) under the status bar. Lyrics and
// Queue start below it.
internal val APPLE_MUSIC_TOP_BAR_HEIGHT = 48.dp
