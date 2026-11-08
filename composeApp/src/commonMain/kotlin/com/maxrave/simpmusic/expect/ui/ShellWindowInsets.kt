package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fork: fits the Windows layout inside the system bars on a tablet, as a window sits inside a screen,
 * painting the bars' area in [color]. Every screen then reads zero bar insets, the same as on Desktop.
 */
@Composable
expect fun ShellWindowInsets(
    enabled: Boolean,
    color: Color,
)
