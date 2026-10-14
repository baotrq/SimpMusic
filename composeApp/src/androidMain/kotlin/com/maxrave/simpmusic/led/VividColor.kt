package com.maxrave.simpmusic.led

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A point on the strip's colour wheel: [hue] in degrees, [saturation] 0 (white) .. 1 (pure hue).
 * Brightness is not part of it — that is the controller's own command.
 */
internal data class WheelColor(
    val hue: Float,
    val saturation: Float,
)

/**
 * The colour an LED strip should show for a piece of artwork, and how the strip walks between
 * two of them.
 */
internal object VividColor {
    private const val BINS = 24
    private const val MIN_SATURATION = 0.28f
    private const val MIN_VALUE = 0.22f

    /** Warm white for artwork with no real colour in it (black-and-white covers). */
    private val NEUTRAL = WheelColor(hue = 30f, saturation = 0.35f)

    /** How far one animation step may move. 2.5° every ~33 ms crosses half the wheel in ~2.4 s. */
    private const val HUE_STEP = 2.5f

    /** Perceptual-whiteness step: pure colour → the warm white is ~40 steps, ~1.4 s. */
    private const val WHITE_STEP = 0.02f

    /** The most prominent saturated hue of [pixels] (ARGB ints), at full saturation. */
    fun pick(pixels: IntArray): WheelColor {
        val weight = FloatArray(BINS)
        val sumR = FloatArray(BINS)
        val sumG = FloatArray(BINS)
        val sumB = FloatArray(BINS)
        var colourful = 0
        val hsv = FloatArray(3)
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            rgbToHsv(r, g, b, hsv)
            if (hsv[1] < MIN_SATURATION || hsv[2] < MIN_VALUE) continue
            colourful++
            val w = hsv[1] * hsv[2]
            val bin = ((hsv[0] / 360f) * BINS).toInt().coerceIn(0, BINS - 1)
            weight[bin] += w
            sumR[bin] += r * w
            sumG[bin] += g * w
            sumB[bin] += b * w
        }
        // Under ~3% colourful pixels the "dominant hue" is noise from a grey cover.
        if (colourful < pixels.size * 0.03f) return NEUTRAL

        // Score each bin with its neighbours so a hue split across a bin edge is not undercounted.
        var best = 0
        var bestScore = -1f
        for (i in 0 until BINS) {
            val s = weight[i] + 0.5f * (weight[(i + 1) % BINS] + weight[(i + BINS - 1) % BINS])
            if (s > bestScore) {
                bestScore = s
                best = i
            }
        }
        val w = weight[best]
        if (w <= 0f) return NEUTRAL
        rgbToHsv((sumR[best] / w).toInt(), (sumG[best] / w).toInt(), (sumB[best] / w).toInt(), hsv)
        // Full saturation: on a strip any third channel is added light, not depth. A red cover read
        // as FF 07 24 looked pink-white at full brightness, where FF 00 00 is the red it should be.
        return WheelColor(hsv[0], 1f)
    }

    /**
     * One step from [from] toward [to]: hue the short way round the wheel by at most [HUE_STEP],
     * saturation by at most [SATURATION_STEP]. Fixed-size steps rather than a timed fade, so a slow
     * Bluetooth write slows the walk instead of making it jump to catch up. Returns [to] once there.
     */
    fun step(
        from: WheelColor,
        to: WheelColor,
    ): WheelColor {
        var dh = to.hue - from.hue
        if (dh > 180f) dh -= 360f
        if (dh < -180f) dh += 360f
        // Whiteness moves in perceptual units: the strip adds white as linear light, and the eye
        // sees the first few percent of it (above all the strip's strong blue) as most of the
        // change. Stepped linearly, a colour → white walk read as a jump at the start; stepped in
        // whiteness^(1/2.2) the white eases in and the walk looks even. Endpoints are unchanged.
        val pFrom = perceptualWhite(from.saturation)
        val dp = perceptualWhite(to.saturation) - pFrom
        if (abs(dh) <= HUE_STEP && abs(dp) <= WHITE_STEP) return to
        val hue = ((from.hue + dh.coerceIn(-HUE_STEP, HUE_STEP)) % 360f + 360f) % 360f
        return WheelColor(hue, saturationOf(pFrom + dp.coerceIn(-WHITE_STEP, WHITE_STEP)))
    }

    private fun perceptualWhite(saturation: Float): Float = (1f - saturation.coerceIn(0f, 1f)).toDouble().pow(1.0 / PERCEPTUAL_GAMMA).toFloat()

    private fun saturationOf(perceptualWhite: Float): Float = 1f - perceptualWhite.coerceIn(0f, 1f).toDouble().pow(PERCEPTUAL_GAMMA).toFloat()

    private const val PERCEPTUAL_GAMMA = 2.2

    /**
     * PWM duty for the strip (0xRRGGBB). The colour is mixed on a constant-light wheel, then scaled
     * so its strongest channel is always FF, the way LED LAMP's own colour wheel does it.
     *
     * The scaling is not optional: this controller flickers on mid-range values. Tested from a
     * laptop on 2026-10-13, the same yellow → white fade flickered at both 30 and 8 writes/s when
     * its strongest channel sat around 7A, and was smooth at both rates with it held at FF. Overall
     * level is the controller's brightness command, never the colour.
     */
    fun toDuty(c: WheelColor): Int {
        val h = ((c.hue % 360f) + 360f) % 360f
        val f = (h % 120f) / 120f
        val (r, g, b) =
            when {
                h < 120f -> Triple(1f - f, f, 0f)
                h < 240f -> Triple(0f, 1f - f, f)
                else -> Triple(f, 0f, 1f - f)
            }
        val s = c.saturation.coerceIn(0f, 1f)
        val white = (1f - s) / 3f
        val mixed = floatArrayOf(r * s + white, g * s + white, b * s + white)
        val peak = mixed.max().coerceAtLeast(1e-6f)
        fun ch(v: Float) = (v / peak * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (ch(mixed[0]) shl 16) or (ch(mixed[1]) shl 8) or ch(mixed[2])
    }

    /** The same colour as a screen shows it, for the swatch in Settings. */
    fun toScreen(c: WheelColor): Int = hsvToRgb(((c.hue % 360f) + 360f) % 360f, c.saturation.coerceIn(0f, 1f), 1f)

    private fun rgbToHsv(
        r: Int,
        g: Int,
        b: Int,
        out: FloatArray,
    ) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val d = mx - mn
        val h =
            when {
                d == 0f -> 0f
                mx == rf -> 60f * (((gf - bf) / d) % 6f)
                mx == gf -> 60f * (((bf - rf) / d) + 2f)
                else -> 60f * (((rf - gf) / d) + 4f)
            }
        out[0] = if (h < 0f) h + 360f else h
        out[1] = if (mx == 0f) 0f else d / mx
        out[2] = mx
    }

    private fun hsvToRgb(
        h: Float,
        s: Float,
        v: Float,
    ): Int {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) =
            when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        fun ch(f: Float) = ((f + m) * 255f).toInt().coerceIn(0, 255)
        return (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
