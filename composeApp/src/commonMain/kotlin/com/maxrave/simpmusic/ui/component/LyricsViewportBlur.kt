package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Fixed focus zones: scrolling moves the words through the blur, never the blur through the words.
 * The bottom third blurs progressively, clear at its top edge and 6dp at the bottom, by crossfading
 * three copies of the lyrics (clear, 3dp, 6dp). The top fades into the 3dp copy while scrolled.
 */
@Composable
internal fun Modifier.lyricsViewportBlur(enabled: Boolean, topBlurred: Boolean): Modifier {
    if (!enabled) return this
    val source = rememberGraphicsLayer()
    val soft = rememberGraphicsLayer()
    val strong = rememberGraphicsLayer()
    val topAmount by animateFloatAsState(if (topBlurred) 1f else 0f, tween(200), label = "lyricsTopBlur")
    val rowHeight = with(LocalDensity.current) { AppleMusicLyricLineHeight.toDp() } + AppleMusicLyricGap
    return drawWithCache {
        val viewportHeight = size.height.coerceAtLeast(1f)
        val top = (rowHeight.toPx() * 1.5f).coerceAtMost(size.height * 0.3f) / viewportHeight
        val bottomStart = 2f / 3f
        // Each stop: (position, clear weight, 3dp weight, 6dp weight); the three always sum to 1.
        val stops = buildList {
            for (i in 0..24) {
                val t = i / 24f
                val clear = 1f - topAmount * (1f - t * t * (3f - 2f * t))
                add(listOf(top * t, clear, 1f - clear, 0f))
            }
            for (i in 0..24) {
                val t = i / 24f
                // 0 = clear, 1 = 3dp, 2 = 6dp: blur strength rises smoothly toward the bottom edge.
                val level = 2f * t * t * (3f - 2f * t)
                add(
                    listOf(
                        bottomStart + (1f - bottomStart) * t,
                        (1f - level).coerceAtLeast(0f),
                        (1f - abs(level - 1f)).coerceAtLeast(0f),
                        (level - 1f).coerceAtLeast(0f),
                    ),
                )
            }
        }
        fun mask(weight: Int) =
            Brush.verticalGradient(colorStops = stops.map { it[0] to Color.Black.copy(alpha = it[weight]) }.toTypedArray())
        val clearMask = mask(1)
        val softMask = mask(2)
        val strongMask = mask(3)
        val paint = Paint()
        soft.renderEffect = BlurEffect(3.dp.toPx(), 3.dp.toPx(), TileMode.Decal)
        strong.renderEffect = BlurEffect(6.dp.toPx(), 6.dp.toPx(), TileMode.Decal)
        onDrawWithContent {
            source.record { this@onDrawWithContent.drawContent() }
            soft.record { drawLayer(source) }
            strong.record { drawLayer(source) }
            drawIntoCanvas { canvas ->
                fun masked(layer: GraphicsLayer, brush: Brush) {
                    canvas.saveLayer(Rect(Offset.Zero, size), paint)
                    drawLayer(layer)
                    drawRect(brush, blendMode = BlendMode.DstIn)
                    canvas.restore()
                }
                masked(source, clearMask)
                masked(soft, softMask)
                masked(strong, strongMask)
            }
        }
    }
}
