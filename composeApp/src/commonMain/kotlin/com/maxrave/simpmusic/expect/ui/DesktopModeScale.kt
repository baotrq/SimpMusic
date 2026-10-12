package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Fork: how much larger to draw the whole UI right now — above 1 only while an Android app runs in
 * a phone-powered desktop mode (Samsung DeX), where the window is read from further away than a
 * phone screen and everything looked a size too small. 1 everywhere else, including the desktop
 * app, which is laid out for a real monitor already.
 */
@Composable
expect fun rememberDesktopModeScale(): Float
