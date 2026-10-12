package com.maxrave.simpmusic.expect.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun rememberDesktopModeScale(): Float {
    val configuration = LocalConfiguration.current
    return remember(configuration) { if (configuration.isSamsungDex()) DEX_SCALE else 1f }
}

// Samsung's documented check: DeX is on when the configuration's semDesktopModeEnabled equals the
// SEM_DESKTOP_MODE_ENABLED constant. Both are Samsung additions to Configuration, so on any other
// phone the lookup fails and the answer is simply "not DeX".
private fun Configuration.isSamsungDex(): Boolean =
    runCatching {
        val type = javaClass
        type.getField("SEM_DESKTOP_MODE_ENABLED").getInt(type) == type.getField("semDesktopModeEnabled").getInt(this)
    }.getOrDefault(false)

// About 15% larger in DeX.
private const val DEX_SCALE = 1.15f
