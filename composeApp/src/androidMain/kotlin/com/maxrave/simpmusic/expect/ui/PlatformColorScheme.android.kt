package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.maxrave.simpmusic.extension.getActivityOrNull

@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.getActivityOrNull()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // Light theme → dark icons; dark theme → light icons.
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }
}
