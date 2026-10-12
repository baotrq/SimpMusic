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
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Fixed focus zones: scrolling moves the words through the blur, never the blur through the words. */
@Composable
internal fun Modifier.lyricsViewportBlur(enabled: Boolean, topBlurred: Boolean): Modifier {
    if (!enabled) return this
    val source = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val topAmount by animateFloatAsState(if (topBlurred) 1f else 0f, tween(200), label = "lyricsTopBlur")
    val rowHeight = with(LocalDensity.current) { AppleMusicLyricLineHeight.toDp() } + AppleMusicLyricGap
    return drawWithCache {
        val viewportHeight = size.height.coerceAtLeast(1f)
        val top = (rowHeight.toPx() * 1.5f).coerceAtMost(size.height * 0.3f)
        val bottom = (rowHeight.toPx() * 2f).coerceAtMost(size.height * 0.4f)
        val clearStops = buildList {
            for (i in 0..24) {
                val t = i / 24f
                val smooth = t * t * (3f - 2f * t)
                add(top / viewportHeight * t to Color.Black.copy(alpha = 1f - topAmount * (1f - smooth)))
            }
            for (i in 0..24) {
                val t = i / 24f
                val smooth = t * t * (3f - 2f * t)
                add(1f - bottom / viewportHeight * (1f - t) to Color.Black.copy(alpha = 1f - smooth))
            }
        }
        val clearMask = Brush.verticalGradient(colorStops = clearStops.toTypedArray())
        val blurMask = Brush.verticalGradient(
            colorStops = clearStops.map { (stop, color) -> stop to color.copy(alpha = 1f - color.alpha) }.toTypedArray(),
        )
        val paint = Paint()
        blurred.renderEffect = BlurEffect(3.dp.toPx(), 3.dp.toPx(), TileMode.Decal)
        onDrawWithContent {
            source.record { this@onDrawWithContent.drawContent() }
            blurred.record { drawLayer(source) }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawLayer(source)
                drawRect(clearMask, blendMode = BlendMode.DstIn)
                canvas.restore()
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawLayer(blurred)
                drawRect(blurMask, blendMode = BlendMode.DstIn)
                canvas.restore()
            }
        }
    }
}
