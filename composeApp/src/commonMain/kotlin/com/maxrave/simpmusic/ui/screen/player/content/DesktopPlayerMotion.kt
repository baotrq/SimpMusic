package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.component.DESKTOP_STRIP_NARROW_WIDTH
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlin.math.roundToInt

internal const val DESKTOP_PLAYER_MOTION_MS = 400

/** The player's sizes, as values of [DesktopPlayerMotion.position]. */
internal const val DESKTOP_PLAYER_CLOSED = 0
internal const val DESKTOP_PLAYER_PANEL = 1
internal const val DESKTOP_PLAYER_FULL = 2

// The gap every Desktop card keeps from the window's edges and from each other.
private val CARD_MARGIN = 8.dp

// Everything left of the full-size page: the strip's margin, the narrow strip, the gap after it.
private val FULL_PAGE_LEFT = CARD_MARGIN + DESKTOP_STRIP_NARROW_WIDTH + CARD_MARGIN

// Over its last stretch each layer fades, so Queue and Lyrics are gone before the page has narrowed.
private const val LAYER_FADE_SPAN = 0.6f

/**
 * Fork: one number drives every animation of Desktop's player, so nothing can disagree with anything
 * else: 0 closed, 1 the small panel, 2 full size. Read only in layout and draw lambdas, so a frame of
 * the motion recomposes nothing.
 *
 * - **0 to 1:** the panel slides in from the window's right edge, over the content.
 * - **1 to 2:** the panel's left edge moves out to the strip. What is inside never moves or resizes:
 *   each layer is laid out at its final size against the right edge and the surface is only a window
 *   onto it. The cover flies between its two spots above it all.
 * - The content stays still underneath at its final width. It changes width only where the surface
 *   covers the difference: on the way in once the panel has arrived, on the way out before it leaves.
 */
internal object DesktopPlayerMotion {
    val position = Animatable(0f)

    var target by mutableIntStateOf(DESKTOP_PLAYER_CLOSED)

    var pageCover by mutableStateOf<Rect?>(null)
    var panelCover by mutableStateOf<Rect?>(null)

    /** Plain field, written in layout, read in the next draw: never a reason to recompose. */
    var rowWidthPx = 0

    fun isFlying(): Boolean = position.value > DESKTOP_PLAYER_PANEL && position.value < DESKTOP_PLAYER_FULL

    /** The real covers hide while their stand-in is in the air. */
    fun coverAlpha(): Float = if (isFlying()) 0f else 1f

    /** Whether the content under the player should be at the narrow width. */
    fun contentNarrow(): Boolean = target >= DESKTOP_PLAYER_PANEL && position.value >= DESKTOP_PLAYER_PANEL

    /** True once the page hides the content completely, so it need not be drawn. */
    fun contentCovered(): Boolean = position.value >= DESKTOP_PLAYER_FULL

    /** How far left of the window's centre the capsule sits: on the centre of what is left of the panel. */
    fun capsuleShiftPx(density: Density): Float {
        val p = position.value
        val share = if (p <= DESKTOP_PLAYER_PANEL) p else DESKTOP_PLAYER_FULL - p
        return panelSlotPx(rowWidthPx, density) * share / 2f
    }

    /**
     * The room the panel takes at the window's right edge, its gap included: the share Now Playing has
     * always had (35% of the width beside the old 80dp rail, less 8dp).
     */
    fun panelSlotPx(
        rowWidth: Int,
        density: Density,
    ): Int =
        with(density) {
            (((rowWidth - 80.dp.roundToPx()) * 0.35f).roundToInt() - CARD_MARGIN.roundToPx()).coerceIn(0, rowWidth)
        }
}

private fun Density.panelCardWidthPx(rowWidth: Int): Int = (DesktopPlayerMotion.panelSlotPx(rowWidth, this) - CARD_MARGIN.roundToPx()).coerceAtLeast(0)

/**
 * Fork: the player's one surface, drawn above the content and below the capsule. [page] and [panel]
 * are composed only while they can be seen, so at rest only one of them exists.
 */
@Composable
internal fun DesktopPlayerLayers(
    sharedViewModel: SharedViewModel,
    page: @Composable (PlatformBackdrop) -> Unit,
    panel: @Composable () -> Unit,
) {
    val motion = DesktopPlayerMotion
    val showPage by remember { derivedStateOf { motion.target == DESKTOP_PLAYER_FULL || motion.position.value > DESKTOP_PLAYER_PANEL } }
    val showPanel by remember { derivedStateOf { motion.target != DESKTOP_PLAYER_FULL || motion.position.value < DESKTOP_PLAYER_FULL } }
    val backdrop = rememberBackdrop(Color.Black)
    val glass = LocalLiquidGlassEnabled.current

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = CARD_MARGIN, end = CARD_MARGIN, bottom = CARD_MARGIN)
                    .layout { measurable, constraints ->
                        val rowWidth = constraints.maxWidth + CARD_MARGIN.roundToPx()
                        // The layers read it from here, in the same pass, before the content has been measured.
                        motion.rowWidthPx = rowWidth
                        val p = motion.position.value
                        val panelWidth = panelCardWidthPx(rowWidth)
                        val pageWidth = constraints.maxWidth - (FULL_PAGE_LEFT).roundToPx()
                        val width =
                            if (p <= DESKTOP_PLAYER_PANEL) {
                                panelWidth
                            } else {
                                panelWidth + ((pageWidth - panelWidth) * (p - DESKTOP_PLAYER_PANEL)).roundToInt()
                            }
                        // Sliding in from past the window's edge, by the width and its gap.
                        val slide =
                            if (p < DESKTOP_PLAYER_PANEL) ((1f - p) * (panelWidth + CARD_MARGIN.roundToPx())).roundToInt() else 0
                        val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(constraints.maxWidth - width + slide, 0)
                        }
                    }.clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
        ) {
            if (showPage) {
                DesktopPlayerBackdrop(
                    sharedViewModel = sharedViewModel,
                    modifier =
                        Modifier
                            .pinnedToEnd { windowWidth -> windowWidth - FULL_PAGE_LEFT.roundToPx() - CARD_MARGIN.roundToPx() }
                            .then(if (glass) Modifier.layerBackdrop(backdrop) else Modifier),
                )
                Box(
                    modifier =
                        Modifier
                            .pinnedToEnd { windowWidth -> windowWidth - FULL_PAGE_LEFT.roundToPx() - CARD_MARGIN.roundToPx() }
                            .graphicsLayer {
                                alpha = ((motion.position.value - (DESKTOP_PLAYER_FULL - LAYER_FADE_SPAN)) / LAYER_FADE_SPAN).coerceIn(0f, 1f)
                            },
                ) {
                    page(backdrop)
                }
            }
            if (showPanel) {
                Box(
                    modifier =
                        Modifier
                            .pinnedToEnd { windowWidth -> panelCardWidthPx(windowWidth) }
                            .graphicsLayer {
                                alpha = ((DESKTOP_PLAYER_PANEL + LAYER_FADE_SPAN - motion.position.value) / LAYER_FADE_SPAN).coerceIn(0f, 1f)
                            },
                ) {
                    panel()
                }
            }
        }
    }
}

/**
 * Laid out at a fixed width against the surface's right edge, however wide the surface is at the moment,
 * so what is inside never moves or reflows. [width] is given the window's row width, which comes from
 * the plain field the content's layout writes.
 */
private fun Modifier.pinnedToEnd(width: Density.(Int) -> Int): Modifier =
    layout { measurable, constraints ->
        val fixedWidth = width(DesktopPlayerMotion.rowWidthPx).coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(fixedWidth, constraints.maxHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(constraints.maxWidth - fixedWidth, 0)
        }
    }

/**
 * Fork: the cover in the air between the panel's spot and the page's. Both spots are read live, and the
 * spot being flown to exists from the motion's first frame because its layer is already composed.
 */
@Composable
internal fun DesktopCoverFlight(sharedViewModel: SharedViewModel) {
    val motion = DesktopPlayerMotion
    val flying by remember { derivedStateOf { motion.isFlying() } }
    if (!flying) return
    val screenData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
        AsyncImage(
            model = screenData.thumbnailURL,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .layout { measurable, constraints ->
                        val from = motion.panelCover ?: fallbackPanelCover(this, origin)
                        val to = motion.pageCover ?: from
                        val t = (motion.position.value - DESKTOP_PLAYER_PANEL).coerceIn(0f, 1f)
                        val left = from.left + (to.left - from.left) * t - origin.x
                        val top = from.top + (to.top - from.top) * t - origin.y
                        val size = (from.width + (to.width - from.width) * t).roundToInt().coerceAtLeast(1)
                        val heightPx = (from.height + (to.height - from.height) * t).roundToInt().coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(size, heightPx))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(left.roundToInt(), top.roundToInt())
                        }
                    }.meltEdges(COVER_MELT_FRACTION),
        )
    }
}

// Used only for the first frames of a first expand, before the panel's style has reported its spot.
private fun fallbackPanelCover(
    density: Density,
    origin: Offset,
): Rect {
    val rowWidth = DesktopPlayerMotion.rowWidthPx
    val width = density.panelCardWidthPx(rowWidth).toFloat()
    val margin = with(density) { CARD_MARGIN.toPx() }
    val left = origin.x + rowWidth - margin - width
    return Rect(left, origin.y + margin, left + width, origin.y + margin + width)
}
