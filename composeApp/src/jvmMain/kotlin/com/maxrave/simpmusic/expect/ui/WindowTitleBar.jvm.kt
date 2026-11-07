package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.maxrave.logger.Logger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Window

internal data class WindowTitleBarColors(
    val color: Color,
    val isDark: Boolean,
)

/** Written by the app's theme, which only exists inside the window; read by the window itself. */
internal object WindowTitleBarState {
    private val _colors = MutableStateFlow(WindowTitleBarColors(Color.Black, isDark = true))
    val colors: StateFlow<WindowTitleBarColors> = _colors.asStateFlow()

    fun set(colors: WindowTitleBarColors) {
        _colors.value = colors
    }
}

@Composable
actual fun SyncWindowTitleBar(
    color: Color,
    isDark: Boolean,
) {
    SideEffect { WindowTitleBarState.set(WindowTitleBarColors(color, isDark)) }
}

private interface Dwmapi : Library {
    fun DwmSetWindowAttribute(
        hwnd: Pointer,
        attribute: Int,
        value: IntByReference,
        size: Int,
    ): Int
}

// dwmapi.h. The first works from Windows 10 2004 and turns the bar dark or light; the other two are
// Windows 11 only and set its exact colours. An older Windows refuses those with an error and keeps
// what the first one gave it.
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_CAPTION_COLOR = 35
private const val DWMWA_TEXT_COLOR = 36

private val dwmapi: Dwmapi? by lazy {
    runCatching { Native.load("dwmapi", Dwmapi::class.java) }.getOrNull()
}

/** Windows only: Windows draws the native title bar, and it stays white unless told otherwise. */
internal fun applyWindowsTitleBar(
    window: Window,
    colors: WindowTitleBarColors,
) {
    val api = dwmapi ?: return
    runCatching {
        val hwnd = Native.getWindowPointer(window)
        fun set(
            attribute: Int,
            value: Int,
        ) = api.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), Int.SIZE_BYTES)
        set(DWMWA_USE_IMMERSIVE_DARK_MODE, if (colors.isDark) 1 else 0)
        set(DWMWA_CAPTION_COLOR, colors.color.toColorRef())
        set(DWMWA_TEXT_COLOR, if (colors.isDark) Color.White.toColorRef() else Color.Black.toColorRef())
    }.onFailure { Logger.w("WindowTitleBar", "Could not paint the title bar: ${it.message}") }
}

// COLORREF is 0x00BBGGRR.
private fun Color.toColorRef(): Int {
    val argb = toArgb()
    val red = argb shr 16 and 0xFF
    val green = argb shr 8 and 0xFF
    val blue = argb and 0xFF
    return red or (green shl 8) or (blue shl 16)
}
