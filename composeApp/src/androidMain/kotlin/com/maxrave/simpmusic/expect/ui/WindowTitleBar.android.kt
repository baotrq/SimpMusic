package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SyncWindowTitleBar(
    color: Color,
    isDark: Boolean,
) {
    // An app window on Android has no title bar of its own.
}
