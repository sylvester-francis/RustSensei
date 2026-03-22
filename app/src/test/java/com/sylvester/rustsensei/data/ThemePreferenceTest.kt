package com.sylvester.rustsensei.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {

    @Test
    fun `fromString returns SYSTEM for SYSTEM`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromString("SYSTEM"))
    }

    @Test
    fun `fromString returns DARK for DARK`() {
        assertEquals(ThemePreference.DARK, ThemePreference.fromString("DARK"))
    }

    @Test
    fun `fromString returns LIGHT for LIGHT`() {
        assertEquals(ThemePreference.LIGHT, ThemePreference.fromString("LIGHT"))
    }

    @Test
    fun `fromString returns SYSTEM for unknown string`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString returns SYSTEM for empty string`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromString(""))
    }

    @Test
    fun `entries contains exactly 3 values`() {
        assertEquals(3, ThemePreference.entries.size)
    }
}
