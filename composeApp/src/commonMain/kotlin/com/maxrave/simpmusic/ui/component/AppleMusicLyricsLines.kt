package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.theme.typo

// Keep inactive lyrics readable anywhere in the viewport. Their grey text already distinguishes
// them from the current lyric; focus no longer depends on distance from the playing line.
private const val PRE_ROLL_LINE_ALPHA = 0.6f

// The unsung REMAINDER of the line currently being sung — "ce" in "dan|ce". It has to be clearly
// grey against the white of what has already been sung, or the wipe carries no contrast and the
// line never reads as lighting up. Classic's DimRichPendingColor is LightGray at 60%, far too
// close to white for that.
internal val AppleMusicPendingWordColor = Color(0xFF8E8E8E)

/**
 * Apple sets lyrics far larger than body copy — a line fills most of the width and wraps after a
 * few words, which is what makes the page readable at arm's length while the phone sits on a desk.
 * The app's own headlineLarge is 23sp, sized for list headers rather than for this, so the size is
 * overridden here while the weight and font family still come from the theme.
 */
internal val AppleMusicLyricFontSize = 28.sp
// Taken from AMLL's stylesheet (packages/core/src/styles/lyric-player.module.css) rather than
// measured off a screenshot. Everything there is expressed in `em`, so it scales with whatever
// font size the host picks — these are those ratios resolved against AppleMusicLyricFontSize.
//
//   .lyricLineWrapper { padding: 0.4em <x> }  -> 0.4 * 32 = 12.8dp above AND below each line
//   .lyricMainLine    — no line-height at all -> the browser default, ~1.2em
//
// Note which one is bigger: the leading INSIDE a wrapped line is tighter than the padding between
// two lines, and that is what makes a wrapped lyric read as one thought instead of two.
internal val AppleMusicLyricLineHeight = 34.sp
internal val AppleMusicLyricGap = 20.dp

// Sizing from .lyricSubLine { font-size: max(0.5em, 10px); line-height: 1.5em } plus the flex
// `gap: 0.3em` above it.
//
// Its `opacity: 0.3` is deliberately NOT copied. That rule dresses AMLL's romanisation/ruby row —
// a pronunciation aid you glance at — whereas this row is the TRANSLATION, which the user turned
// on specifically to read. At 0.3 it was unreadable even on the line being sung. It follows the
// line's own colour instead: white-ish while sung, grey with the rest.
internal val AppleMusicSubLineFontSize = 14.sp
internal val AppleMusicSubLineHeight = 21.sp
internal val AppleMusicMainToSubGap = 12.dp

// .lyricLineWrapper { border-radius: 0.25em } plus its :hover / :active background. There is no
// hover on a phone, so the press state takes the more visible of the two (#fff1). Note it is a
// tinted panel behind the WHOLE wrapper — original line and translation together — not a ripple:
// AMLL never uses one, and a ripple centred on a tap point reads as a button press.
// --lyric-line-padding-x: 20px on a narrow viewport. It lives on .lyricLineWrapper in AMLL, i.e.
// on the LINE, not on whatever screen happens to host the list — which is why LyricsView applies
// it itself rather than trusting each caller. FullscreenLyricsSheet passes a bare fillMaxSize(),
// so a caller-supplied gutter would have left the fullscreen sheet's text flush against both
// edges while the Now Playing tab looked right.
internal val AppleMusicLyricPaddingX = 20.dp

internal val AppleMusicLyricCornerRadius = 8.dp
internal val AppleMusicLyricPressedBackground = Color.White.copy(alpha = 0.067f)

// FlowRow lays wrapped words out itself and ignores lineHeight entirely, so the rich-sync path
// needs the same leading expressed as explicit spacing between its rows — and it has to stay
// TIGHTER than AppleMusicLyricGap, for the same reason: rows here are one lyric wrapping, while
// the gap separates two different lyrics. 38sp of leading over the ~37sp a 32sp line occupies
// leaves almost nothing, which matches AMLL letting the default leading stand.
internal val AppleMusicWrappedLineSpacing = 2.dp

// White, like Apple — NOT the Classic renderer's yellow. Slightly under full opacity so the
// translation still reads as secondary to the line it translates.
internal val AppleMusicTranslatedColor = Color.White.copy(alpha = 0.78f)

// A shade under the translation's: on a line carrying original + reading + translation, the
// reading is the one you stop needing once you know the song, so it recedes first.
internal val AppleMusicRomanizedColor = Color.White.copy(alpha = 0.62f)

// The line NOT being sung is grey, full stop — white at reduced opacity is still white on a dark
// page, which is why every line looked equally lit no matter how the alpha was tuned. This is the
// single biggest signal separating the sung line from the rest; blur and opacity only refine it.
internal val AppleMusicInactiveLineColor = Color(0xFF9B9B9B)

// Passing this to a rich-synced line is what ENABLES AMLL's emphasis maths in AnimatedWord; the
// alpha and radius here are placeholders, because that code computes both from the word's own
// duration and its position within the wipe. Only the colour is read as-is.
//
// Deliberately NOT applied to line-synced lyrics: those carry no per-word timing, so there is no
// sustain to reflect, and a constant halo there is pure decoration — which is what made the first
// attempt look wrong.
internal val AppleMusicActiveLineGlow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 0f)

// No glow constant for line-synced lyrics on purpose. Without per-word timing there is no single
// word to flare, and lighting the whole line instead just makes it look smudged — the sung line is
// marked by being WHITE against grey, and that is all.

/** Animate lyric emphasis independently of the fixed viewport blur zones. */
@Composable
fun Modifier.appleMusicLyricFocus(
    hasActiveLine: Boolean = true,
    // An unsynced sheet has no sung line and never will have one, so every line IS the sung line:
    // full opacity, no blur. Deliberately NOT folded into [hasActiveLine], which is false in two
    // situations that want opposite treatments — this one, and the pre-roll of a SYNCED sheet,
    // where a sung line is coming and PRE_ROLL_LINE_ALPHA is dimmer on purpose.
    allLinesCurrent: Boolean = false,
): Modifier {
    val targetAlpha = if (allLinesCurrent || hasActiveLine) 1f else PRE_ROLL_LINE_ALPHA

    val lineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(400), label = "appleMusicLyricAlpha")

    return this.alpha(lineAlpha)
}

/**
 * One line-synced lyric line, Apple Music style: same size for every line (the Classic renderer
 * swaps headlineLarge/headlineMedium instead), white, aligned to its text direction, with the translation underneath.
 * Emphasis is applied by the caller through [appleMusicLyricFocus] to the lyric and translation.
 */
@Composable
fun AppleMusicLyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isCurrent: Boolean,
    // Sits between the original and the translation. Shares the sub-line type scale with the
    // translation, since both are secondary to the lyric itself and a second size at this scale
    // would just look like a mistake.
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
        LyricText(
            text = originalWords,
            modifier = Modifier.fillMaxWidth(),
            color = if (isCurrent) Color.White else AppleMusicInactiveLineColor,
            style =
                typo().headlineLarge.copy(
                    fontSize = AppleMusicLyricFontSize,
                    lineHeight = AppleMusicLyricLineHeight,
                ),
        )
        if (romanizedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            LyricText(
                text = romanizedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style =
                    typo().bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                // Follows the line's own state exactly as the translation does: readable white-ish
                // on the sung line, the same grey as the lyric everywhere else.
                color = if (isCurrent) AppleMusicRomanizedColor else AppleMusicInactiveLineColor,
            )
        }
        if (translatedWords != null) {
            Spacer(modifier = Modifier.height(AppleMusicMainToSubGap))
            LyricText(
                text = translatedWords,
                alignmentText = originalWords,
                modifier = Modifier.fillMaxWidth(),
                style =
                    typo().bodyMedium.copy(
                        fontSize = AppleMusicSubLineFontSize,
                        lineHeight = AppleMusicSubLineHeight,
                    ),
                // The colour still follows the line's own state; AMLL carries the whole sub-line at
                // a flat 0.3 opacity on top of that, which is applied as a modifier so it composes
                // with appleMusicLyricFocus's own dimming instead of fighting it.
                color = if (isCurrent) AppleMusicTranslatedColor else AppleMusicInactiveLineColor,
            )
        }
        Spacer(modifier = Modifier.height(AppleMusicLyricGap))
    }
}
