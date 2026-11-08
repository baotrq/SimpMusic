package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.DisplayMetrics
import kotlin.math.hypot

// Android's own line between a phone and a large screen.
private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600

// The biggest phones stop just under this. Small or budget tablets, and any tablet set to a larger
// display size, can fall under 600dp, so the width alone calls them phones and they never rotate.
private const val TABLET_MIN_DIAGONAL_INCHES = 7f

/** A bar phone or a Fold's cover screen, as opposed to an unfolded Fold, a tablet or DeX. */
val Activity.isPhoneSizedScreen: Boolean
    get() =
        resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_SMALLEST_WIDTH_DP &&
            screenDiagonalInches() < TABLET_MIN_DIAGONAL_INCHES

@Suppress("DEPRECATION") // Display.getRealMetrics is what still reports xdpi and ydpi on every API level.
private fun Activity.screenDiagonalInches(): Float {
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getRealMetrics(metrics)
    if (metrics.xdpi <= 0f || metrics.ydpi <= 0f) return 0f
    return hypot(metrics.widthPixels / metrics.xdpi, metrics.heightPixels / metrics.ydpi)
}

/**
 * Fork: phone-sized screens stay in portrait. Called again on every configuration change, so a Fold is
 * locked on its cover screen and free once opened. A large screen only drops the portrait lock and
 * keeps any other request, which is fullscreen video asking for landscape.
 */
fun Activity.applyPhoneOrientationLock() {
    if (isPhoneSizedScreen) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
