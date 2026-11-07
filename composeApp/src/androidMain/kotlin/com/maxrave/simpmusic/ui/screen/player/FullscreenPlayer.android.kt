package com.maxrave.simpmusic.ui.screen.player

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maxrave.simpmusic.extension.applyPhoneOrientationLock
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.extension.isPhoneSizedScreen

@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(true) {
        onLaunch.invoke()
        val activity = context.findActivity()
        // Fork: phone-sized screens stay in portrait, fullscreen video included.
        if (!activity.isPhoneSizedScreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val window = context.findActivity().window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            insetsController.apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // Back to free on a large screen, back to the lock on a phone.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity.applyPhoneOrientationLock()
            onDispose.invoke()
        }
    }

    LaunchedEffect(true) {
        val activity = context.findActivity()
        val window = activity.window

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}