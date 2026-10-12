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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A sheet inside the DeX window, with the same drag-to-dismiss behavior as the phone sheet. */
@Composable
internal fun InWindowNowPlayingSheet(
    onDismiss: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val position = remember { Animatable(1f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val settle: (Boolean, Float) -> Unit = { dismiss, velocity ->
        animationJob?.cancel()
        animationJob = scope.launch {
            if (dragging) position.snapTo(dragPosition)
            dragging = false
            position.animateTo(
                if (dismiss) 1f else 0f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                initialVelocity = velocity,
            )
            if (dismiss) currentOnDismiss()
        }
    }
    LaunchedEffect(Unit) { settle(false, 0f) }
    val dismiss = { settle(true, 0f) }
    PlatformBackHandler(enabled = true, onBack = dismiss)

    // This detector stays still while only its child moves. Children get first refusal, so a
    // downward pull on Lyrics/Queue returns to the player before another pull dismisses it.
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over > 0f && heightPx > 0f) {
                            animationJob?.cancel()
                            dragPosition = (position.value + over / heightPx).coerceIn(0f, 1f)
                            dragging = true
                            change.consume()
                        }
                    } ?: return@awaitEachGesture
                    tracker.addPosition(start.uptimeMillis, start.position)
                    val completed = verticalDrag(start.id) { change ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        dragPosition = (dragPosition + change.positionChange().y / heightPx).coerceIn(0f, 1f)
                        change.consume()
                    }
                    val velocity = if (completed) tracker.calculateVelocity().y else 0f
                    val shouldDismiss = completed &&
                        if (velocity < -900f) false else dragPosition > 0.22f || velocity > 900f
                    settle(shouldDismiss, velocity / heightPx.coerceAtLeast(1f))
                }
            },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = (if (dragging) dragPosition else position.value) * heightPx
            },
        ) {
            content(dismiss)
        }
    }
}
