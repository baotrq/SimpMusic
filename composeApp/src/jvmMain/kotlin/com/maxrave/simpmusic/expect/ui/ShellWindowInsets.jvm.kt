package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun ShellWindowInsets(
    enabled: Boolean,
    color: Color,
) {
    // A Desktop window has no system bars drawn over it.
}
