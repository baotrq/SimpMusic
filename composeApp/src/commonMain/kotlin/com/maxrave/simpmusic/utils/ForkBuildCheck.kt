package com.maxrave.simpmusic.utils

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fork: this app's own update check. The checker (ForkUpdater) lives in androidApp, which this module
 * cannot see, so MainActivity hands it in here while it is alive. Null on Desktop, in debug builds and
 * in vivoAndroidApp, where Settings → About falls back to the upstream check.
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
}
