package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paints the OS's own title bar to match the app's theme, where the OS has one that can be painted. */
@Composable
expect fun SyncWindowTitleBar(
    color: Color,
    isDark: Boolean,
)
