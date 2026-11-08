package com.maxrave.simpmusic.expect.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.maxrave.simpmusic.extension.findActivity

/**
 * The bars are taken off at the view above Compose rather than per screen, because dozens of places
 * read WindowInsets.statusBars directly and would not see a Compose-level consumption. Sheets and
 * dialogs are windows of their own and keep the real insets.
 */
@Composable
actual fun ShellWindowInsets(
    enabled: Boolean,
    color: Color,
) {
    val context = LocalContext.current
    DisposableEffect(context, enabled, color) {
        val content = context.findActivity().findViewById<View>(android.R.id.content)
        if (enabled) {
            content.setBackgroundColor(color.toArgb())
            ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
                val barTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                val bars = insets.getInsets(barTypes)
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                WindowInsetsCompat
                    .Builder(insets)
                    .setInsets(barTypes, Insets.NONE)
                    // The keyboard's inset counts from the screen's edge, and this view already stops above the bar.
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, (ime.bottom - bars.bottom).coerceAtLeast(0)))
                    .build()
            }
            ViewCompat.requestApplyInsets(content)
        }
        onDispose {
            if (enabled) {
                ViewCompat.setOnApplyWindowInsetsListener(content, null)
                content.setPadding(0, 0, 0, 0)
                content.background = null
                ViewCompat.requestApplyInsets(content)
            }
        }
    }
}
