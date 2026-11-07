package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.pm.ActivityInfo

// Android's own line between a phone and a large screen.
private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600

/** A bar phone or a Fold's cover screen, as opposed to an unfolded Fold, a tablet or DeX. */
val Activity.isPhoneSizedScreen: Boolean
    get() = resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_SMALLEST_WIDTH_DP

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
