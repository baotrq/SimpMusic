package com.maxrave.simpmusic.utils

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fork: this app's own update check. On Android the checker (ForkUpdater) lives in androidApp, which
 * this module cannot see, so MainActivity hands it in here while it is alive; on Mac and Windows
 * DesktopForkUpdater does. Null on Linux, in debug builds and Gradle runs, and in vivoAndroidApp, where
 * Settings → About falls back to the upstream check.
 */
object ForkBuildCheck {
    class AvailableBuild(
        val tag: String,
        val title: String,
    )

    var check: (() -> Unit)? = null

    var install: (() -> Unit)? = null

    /** A newer release found by the check after launch; Home shows it as a card. */
    val available = MutableStateFlow<AvailableBuild?>(null)

    /** Desktop: what the Home card says while the update downloads, or once its installer is open. */
    val status = MutableStateFlow<String?>(null)

    /** Desktop: true while the update downloads, which keeps the card's Update button from starting a second one. */
    val downloading = MutableStateFlow(false)
}
