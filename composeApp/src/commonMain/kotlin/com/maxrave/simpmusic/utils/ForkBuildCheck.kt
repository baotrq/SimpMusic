package com.maxrave.simpmusic.utils

/**
 * Fork: Settings → About → Check for new build. The checker (ForkUpdater) lives in androidApp, which
 * this module cannot see, so MainActivity hands it in here while it is alive. Null on Desktop, in
 * debug builds and in vivoAndroidApp, and the row is hidden then.
 */
object ForkBuildCheck {
    var check: (() -> Unit)? = null
}
