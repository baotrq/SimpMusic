package com.maxrave.simpmusic.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopForkUpdaterTest {
    @Test
    fun theFourthNumberCounts() {
        assertTrue(isNewer(candidate = "2.5.0.11", current = "2.5.0.10"))
        assertFalse(isNewer(candidate = "2.5.0.10", current = "2.5.0.11"))
        assertFalse(isNewer(candidate = "2.5.0.10", current = "2.5.0.10"))
    }

    @Test
    fun numbersCompareAsNumbersNotText() {
        assertTrue(isNewer(candidate = "2.5.0.10", current = "2.5.0.9"))
        assertTrue(isNewer(candidate = "2.10.0.0", current = "2.9.9.9"))
    }

    @Test
    fun missingPartsCountAsZero() {
        assertFalse(isNewer(candidate = "2.5.0", current = "2.5.0.0"))
        assertTrue(isNewer(candidate = "2.5.0.1", current = "2.5.0"))
        assertTrue(isNewer(candidate = "2.6", current = "2.5.0.12"))
    }

    @Test
    fun aLeadingVAndASuffixAreIgnored() {
        assertTrue(isNewer(candidate = "v2.5.0.11", current = "2.5.0.10"))
        assertFalse(isNewer(candidate = "2.5.0.10-hf", current = "2.5.0.10"))
    }
}
