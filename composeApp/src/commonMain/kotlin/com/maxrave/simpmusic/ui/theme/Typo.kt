package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

// Fork: the phone's own font (Samsung's One UI font, OnePlus Sans, Roboto…) instead of the bundled
// Poppins, so the app reads like the rest of the system. Weights still come from each style below;
// the system font has real bold and semibold cuts, where Poppins had to synthesize them.
@Composable
fun fontFamily(): FontFamily = FontFamily.Default

/**
 * When true, [typo] keeps the original always-light text colors (pure white titles, #A8A8A8 body)
 * regardless of theme. Immersive screens drawn over dark artwork provide `true` so their text stays
 * readable at light theme. Everything else leaves it false and gets theme-aware colors.
 */
val LocalForceDarkText = staticCompositionLocalOf { false }

@Composable
fun typo(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    forceDark: Boolean = LocalForceDarkText.current,
): Typography {
    val fontFamily = fontFamily()

    // Titles were pure white, everything else muted gray (#A8A8A8) in the old dark-only palette.
    // forceDark keeps that; otherwise both come from the scheme (theme-aware light/dark).
    val titleColor = if (forceDark) Color.White else colorScheme.onBackground
    val bodyColor = if (forceDark) Color(0xFFA8A8A8) else colorScheme.onSurfaceVariant

    // Right-to-left layouts only. Compose gives every paragraph the layout's direction unless told
    // otherwise (TextStyle.resolveTextDirection), so a Latin title or URL was laid out right-to-left:
    // its closing punctuation jumped to the front, and titles mixing Latin with Hebrew came out
    // scrambled. Content lets each paragraph take its direction from its own text. Content alone
    // would also move Latin text to the left edge, so Right keeps it on the layout's start edge,
    // where a TextView keeps it with gravity START.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val textDirection = if (rtl) TextDirection.Content else TextDirection.Unspecified
    val textAlign = if (rtl) TextAlign.Right else TextAlign.Unspecified

    val typo =
        Typography(
            /***
             * This typo().is use for the title of the Playlist, Artist, Song, Album, etc. in Home, Mood, Genre, Playlist, etc.
             */
            titleSmall =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = titleColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            titleMedium =
                TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = titleColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            titleLarge =
                TextStyle(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = titleColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            bodySmall =
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            bodyMedium =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            bodyLarge =
                TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            displayLarge =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            headlineMedium =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            headlineLarge =
                TextStyle(
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            labelMedium =
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            labelSmall =
                TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    color = bodyColor,
                    textDirection = textDirection,
                    textAlign = textAlign,
                ),
            // ...
        )
    return typo
}
