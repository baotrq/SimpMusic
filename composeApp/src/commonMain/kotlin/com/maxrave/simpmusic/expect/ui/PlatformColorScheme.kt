package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/** Keeps the system bar icon appearance in sync with the app theme (Android only; no-op elsewhere). */
@Composable
expect fun SystemBarAppearanceEffect(isDark: Boolean)
