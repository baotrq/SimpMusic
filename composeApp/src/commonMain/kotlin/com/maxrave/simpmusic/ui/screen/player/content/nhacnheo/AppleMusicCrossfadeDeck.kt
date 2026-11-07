package com.maxrave.simpmusic.ui.screen.player.content.nhacnheo

import com.maxrave.simpmusic.ui.screen.player.content.*
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.expect.ui.toReadableBitmap
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringArrayResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfade_mix_words
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** The song on screen, as the deck remembers it after the player has moved on to the next one. */
@Stable
internal data class DeckSong(
    val key: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
)

/**
 * The two-cover crossfade, Spotify-style. When a fade starts, the outgoing song's cover shrinks
 * from the full artwork into the left slot while the incoming one slides in on the right, joined
 * by a stream of glowing particles flowing from one to the other. When the fade ends, the old cover leaves to the left and the new one grows
 * back into the full artwork, landing exactly where the pager's own cover sits, so the hand-back
 * to it is invisible.
 */
@Stable
internal class AppleMusicCrossfadeDeckState {
    /** 0 = the plain artwork, 1 = both covers in their slots. Rises as a fade starts, falls as it ends. */
    internal val deck = Animatable(0f)

    /** Drifts 0 → 1 while the two songs play over each other: the old cover dims, the new one fills in. */
    internal val mix = Animatable(0f)

    internal var exiting by mutableStateOf(false)
    internal var outgoing by mutableStateOf<DeckSong?>(null)
    internal var incoming by mutableStateOf<DeckSong?>(null)

    /** A fade has started but the incoming song is not known yet: the old cover is held full-size. */
    internal var holding by mutableStateOf(false)

    /** Whether the deck is drawn at all; the pager's own cover is hidden while it is. */
    val active: Boolean get() = holding || deck.value > 0f || deck.isRunning
}

/**
 * Tracks the song on screen and drives the deck. [enabled] is false where the artwork is not a still
 * cover (a canvas, a video, animated artwork): the deck then stays down and the page is untouched.
 */
@Composable
internal fun rememberAppleMusicCrossfadeDeckState(
    isCrossfading: Boolean,
    title: String,
    artist: String,
    coverUrl: String?,
    enabled: Boolean,
): AppleMusicCrossfadeDeckState {
    val state = remember { AppleMusicCrossfadeDeckState() }
    val key = "$title\u0000$artist"
    var current by remember { mutableStateOf<DeckSong?>(null) }
    var previous by remember { mutableStateOf<DeckSong?>(null) }
    var changedAt by remember { mutableStateOf<TimeMark?>(null) }
    LaunchedEffect(key, coverUrl) {
        val song = DeckSong(key, title, artist, coverUrl)
        if (current?.key != key) {
            previous = current
            changedAt = TimeSource.Monotonic.markNow()
        }
        current = song
        if (state.incoming?.key == key) state.incoming = song
    }

    LaunchedEffect(isCrossfading, enabled) {
        if (isCrossfading && enabled) {
            val now = current ?: return@LaunchedEffect
            // The player announces the fade before the screen moves on to the next song, so the song
            // on screen is usually still the outgoing one; wait for the next to arrive. If the song
            // has only just changed, the announcement came second and the switch has already happened.
            val justChanged = changedAt?.let { it.elapsedNow() < RECENT_CHANGE } == true
            val out: DeckSong
            val inc: DeckSong
            if (justChanged && previous != null) {
                out = previous!!
                inc = now
            } else {
                out = now
                state.deck.snapTo(0f)
                state.mix.snapTo(0f)
                state.outgoing = out
                state.incoming = null
                state.exiting = false
                state.holding = true
                val next =
                    withTimeoutOrNull(INCOMING_WAIT) {
                        snapshotFlow { current }.first { it != null && it.key != out.key }
                    }
                if (next == null) {
                    state.holding = false
                    return@LaunchedEffect
                }
                inc = next
            }
            state.outgoing = out
            state.incoming = inc
            state.exiting = false
            state.mix.snapTo(0f)
            state.holding = false
            coroutineScope {
                launch { state.deck.animateTo(1f, tween(DECK_IN_MS, easing = FastOutSlowInEasing)) }
                launch { state.mix.animateTo(1f, tween(MIX_DRIFT_MS, easing = LinearOutSlowInEasing)) }
            }
        } else if (state.holding) {
            state.holding = false
        } else if (state.deck.value > 0f) {
            state.exiting = true
            coroutineScope {
                launch { state.mix.animateTo(1f, tween(DECK_OUT_MS / 2)) }
                launch { state.deck.animateTo(0f, tween(DECK_OUT_MS, easing = FastOutSlowInEasing)) }
            }
            state.exiting = false
        }
    }
    return state
}

/** The word shown for a fade ("Mixing", "Cooking"…). Picked from the incoming song, so every label agrees. */
@Composable
internal fun rememberCrossfadeMixWord(seed: String): String {
    val words = stringArrayResource(Res.array.crossfade_mix_words)
    return remember(seed, words) { words.getOrNull(abs(seed.hashCode() % words.size.coerceAtLeast(1))).orEmpty() }
}

/**
 * Draws the deck over the artwork rectangle. [topFade] / [bottomFade] are the cover's own dissolving
 * edges, applied to whichever cover fills the rectangle so it matches the pager's cover exactly at
 * the hand-over.
 */
@Composable
internal fun AppleMusicCrossfadeDeck(
    state: AppleMusicCrossfadeDeckState,
    topFade: Dp,
    bottomFade: Dp,
    modifier: Modifier = Modifier,
) {
    if (!state.active) return
    val outgoing = state.outgoing ?: return
    // Null while holding: only the old cover is drawn, full-size, until the next song is known.
    val incoming = state.incoming
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val full = Rect(0f, 0f, w, h)
        // Both covers and the particle stream between them fit across the width.
        val size = minOf(h * 0.78f, w * 0.36f)
        val gap = w * 0.16f
        val centerY = h / 2f
        val leftCenterX = w / 2f - (size + gap) / 2f
        val rightCenterX = w / 2f + (size + gap) / 2f

        fun slot(
            cx: Float,
            scale: Float,
        ): Rect {
            val s = size * scale
            return Rect(cx - s / 2f, centerY - s / 2f, cx + s / 2f, centerY + s / 2f)
        }

        fun leftRect(): Rect {
            val d = state.deck.value
            val target = slot(leftCenterX, 1f - 0.12f * state.mix.value)
            // In: shrinks out of the full artwork. Out: slides away to the left.
            return if (!state.exiting) {
                lerp(full, target, d)
            } else {
                target.translate(-(target.right + size * 0.2f) * (1f - d), 0f)
            }
        }

        fun rightRect(): Rect {
            val d = state.deck.value
            val target = slot(rightCenterX, 0.9f + 0.1f * state.mix.value)
            // In: slides in from the right. Out: grows back into the full artwork.
            return if (!state.exiting) {
                target.translate((w - target.left + size * 0.2f) * (1f - d), 0f)
            } else {
                lerp(full, target, d)
            }
        }

        val corner = with(density) { COVER_CORNER.toPx() }
        val shadow = with(density) { 18.dp.toPx() }
        val d = state.deck.value
        // Only the cover that fills the rectangle carries the dissolving edges, fading them out as
        // it shrinks into its slot (and back in as it grows out of it).
        val edgeShare = 1f - d

        // Each cover's own colour, for the particles that flow from one to the other.
        var outgoingColor by remember(outgoing.coverUrl) { mutableStateOf(Color.White) }
        var incomingColor by remember(incoming?.coverUrl) { mutableStateOf(Color.White) }

        // The particles go underneath both covers, so they seem to come out from behind one and
        // disappear behind the other.
        if (incoming != null) {
            ParticleStream(
                left = { leftRect() },
                right = { rightRect() },
                fromColor = { outgoingColor },
                toColor = { incomingColor },
                visibility = { state.deck.value * state.deck.value },
                progress = { state.mix.value },
            )
        }

        // Outgoing cover (left), drawn before the incoming one so it sits above it while they overlap.
        DeckCover(
            url = outgoing.coverUrl,
            onColor = { outgoingColor = it },
            rect = ::leftRect,
            corner = { corner * state.deck.value },
            elevation = { shadow * state.deck.value },
            alpha = {
                val dim = 1f - 0.45f * state.mix.value
                if (state.exiting) dim * state.deck.value else dim
            },
            rotation = { -TILT_DEGREES * state.deck.value },
            fadeModifier =
                if (!state.exiting && edgeShare > 0f) {
                    Modifier.appleMusicVerticalFadeEdges(topFade = topFade * edgeShare, bottomFade = bottomFade * edgeShare)
                } else {
                    Modifier
                },
        )

        if (incoming == null) return@BoxWithConstraints

        DeckCover(
            url = incoming.coverUrl,
            onColor = { incomingColor = it },
            rect = ::rightRect,
            corner = { corner * state.deck.value },
            elevation = { shadow * state.deck.value },
            alpha = { if (state.exiting) 1f else state.deck.value.coerceAtLeast(0.001f) },
            rotation = { TILT_DEGREES * state.deck.value },
            fadeModifier =
                if (state.exiting && edgeShare > 0f) {
                    Modifier.appleMusicVerticalFadeEdges(topFade = topFade * edgeShare, bottomFade = bottomFade * edgeShare)
                } else {
                    Modifier
                },
        )
    }
}

@Composable
private fun DeckCover(
    url: String?,
    onColor: (Color) -> Unit,
    rect: () -> Rect,
    corner: () -> Float,
    elevation: () -> Float,
    alpha: () -> Float,
    rotation: () -> Float,
    fadeModifier: Modifier,
) {
    val request =
        ImageRequest
            .Builder(LocalPlatformContext.current)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            // The same key the pager's cover uses, so both read one cached image.
            .diskCacheKey(url + "BIGGER")
            .build()
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        onSuccess = { result ->
            runCatching { coverGlowColor(result.result.image.toImageBitmap()) }.getOrNull()?.let(onColor)
        },
        modifier =
            Modifier
                .placeAt(rect)
                .graphicsLayer {
                    shadowElevation = elevation()
                    shape = RoundedCornerShape(corner())
                    clip = true
                    rotationZ = rotation()
                    this.alpha = alpha()
                }.then(fadeModifier),
    )
}

/** One glowing dot's fixed traits; its position comes from the clock. */
private class Particle(
    val from: Float, // -1..1: where on the old cover's edge it leaves, top to bottom
    val to: Float, // -1..1: where on the new cover's edge it arrives
    val lane: Float, // -1..1: how far above or below its straight path the arc bows
    val speed: Float, // trips per second
    val phase: Float, // 0..1: where along the trip it starts
    val size: Float, // radius multiplier
    val wobble: Float, // phase of the small sideways drift
)

/**
 * Glowing dots drifting from the old cover to the new one along gentle arcs, each one changing from
 * the old cover's colour to the new one's as it crosses. They fade in as they leave a cover and out
 * as they reach the other, so the stream reads as music pouring across. The flow quickens as the
 * mix moves on.
 */
@Composable
private fun ParticleStream(
    left: () -> Rect,
    right: () -> Rect,
    fromColor: () -> Color,
    toColor: () -> Color,
    visibility: () -> Float,
    progress: () -> Float,
) {
    val particles =
        remember {
            val random = Random(PARTICLE_SEED)
            List(PARTICLE_COUNT) {
                Particle(
                    from = random.nextFloat() * 2f - 1f,
                    to = random.nextFloat() * 2f - 1f,
                    lane = random.nextFloat() * 2f - 1f,
                    speed = 0.28f + random.nextFloat() * 0.3f,
                    phase = random.nextFloat(),
                    size = 0.6f + random.nextFloat() * 0.8f,
                    wobble = random.nextFloat() * 2f * PI.toFloat(),
                )
            }
        }
    // A plain clock rather than a repeating animation: every dot has its own speed, so a loop that
    // restarts would make them all jump at once.
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { seconds = (it - start) / 1_000_000_000f }
    }
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = visibility() }) {
        val l = left()
        val r = right()
        // Dots leave from well inside the old cover's right side and sink well into the new one's
        // left side (the new cover is drawn over them), so the stream fills the whole space between.
        val startX = l.right - l.width * 0.25f
        val endX = r.left + r.width * 0.25f
        if (endX <= startX) return@Canvas
        // They leave from anywhere along the old cover's height and arrive anywhere along the new
        // one's, so the stream is as tall as the covers rather than a thin line between their middles.
        val fromHalf = l.height * 0.48f
        val toHalf = r.height * 0.48f
        val bowHeight = minOf(l.height, r.height) * 0.35f
        val baseRadius = 2.6.dp.toPx()
        val from = fromColor()
        val to = toColor()
        // Faster as the mix goes on, without rewinding anyone: speed scales the clock, and the
        // clock only moves forward.
        val time = seconds * (1f + 0.6f * progress())
        for (p in particles) {
            val t = ((p.phase + time * p.speed) % 1f)
            val x = startX + (endX - startX) * t
            // A straight run from its point on the old cover to its point on the new one, bowed up or
            // down in between.
            val bow = sin(PI * t).toFloat()
            val y0 = l.center.y + p.from * fromHalf
            val y1 = r.center.y + p.to * toHalf
            val y = y0 + (y1 - y0) * t + p.lane * bowHeight * bow + sin(p.wobble + time * 3f) * 3.dp.toPx() * bow
            // Visible for most of the trip, fading only right at the ends.
            val fade = minOf(1f, bow * 2.2f)
            val color = lerp(from, to, t)
            val radius = baseRadius * p.size
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0f to color.copy(alpha = 0.45f * fade),
                        1f to Color.Transparent,
                        center = Offset(x, y),
                        radius = radius * 4f,
                    ),
                radius = radius * 4f,
                center = Offset(x, y),
            )
            drawCircle(color = lerp(color, Color.White, 0.55f).copy(alpha = fade), radius = radius, center = Offset(x, y))
        }
    }
}

/**
 * A cover's colour for the particles: the average of an 8x8 sample, weighted toward its most vivid
 * pixels so a mostly-grey sleeve with one bright accent gives the accent, and lifted toward white so
 * a dark sleeve still glows against the page.
 */
private fun coverGlowColor(bitmap: ImageBitmap): Color {
    val sample = bitmap.toReadableBitmap(8, 8).toPixelMap()
    var r = 0f
    var g = 0f
    var b = 0f
    var total = 0f
    for (x in 0 until sample.width) {
        for (y in 0 until sample.height) {
            val c = sample[x, y]
            val max = maxOf(c.red, c.green, c.blue)
            val min = minOf(c.red, c.green, c.blue)
            val weight = 0.1f + (max - min) * 3f
            r += c.red * weight
            g += c.green * weight
            b += c.blue * weight
            total += weight
        }
    }
    if (total <= 0f) return Color.White
    return lerp(Color(r / total, g / total, b / total), Color.White, 0.3f)
}

/** Lays the node out at [rect] (parent coordinates, px), recomputed every frame without recomposing. */
private fun Modifier.placeAt(rect: () -> Rect): Modifier =
    layout { measurable, constraints ->
        val r = rect()
        val placeable =
            measurable.measure(
                Constraints.fixed(r.width.roundToInt().coerceAtLeast(1), r.height.roundToInt().coerceAtLeast(1)),
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(r.left.roundToInt(), r.top.roundToInt())
        }
    }

private val RECENT_CHANGE = 1500.milliseconds
private val INCOMING_WAIT = 4.seconds
private const val DECK_IN_MS = 650
private const val DECK_OUT_MS = 750
private const val MIX_DRIFT_MS = 8_000
private const val PARTICLE_COUNT = 40
private const val PARTICLE_SEED = 0x51A7
private const val TILT_DEGREES = 3f
private val COVER_CORNER = 14.dp
