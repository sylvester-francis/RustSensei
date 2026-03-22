package com.sylvester.rustsensei.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModeTest {

    @Test
    fun `fromString returns DIRECT for DIRECT`() {
        assertEquals(ChatMode.DIRECT, ChatMode.fromString("DIRECT"))
    }

    @Test
    fun `fromString returns SOCRATIC for SOCRATIC`() {
        assertEquals(ChatMode.SOCRATIC, ChatMode.fromString("SOCRATIC"))
    }

    @Test
    fun `fromString returns RUBBER_DUCK for RUBBER_DUCK`() {
        assertEquals(ChatMode.RUBBER_DUCK, ChatMode.fromString("RUBBER_DUCK"))
    }

    @Test
    fun `fromString returns DIRECT for unknown string`() {
        assertEquals(ChatMode.DIRECT, ChatMode.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString returns DIRECT for empty string`() {
        assertEquals(ChatMode.DIRECT, ChatMode.fromString(""))
    }

    @Test
    fun `entries contains exactly 3 values`() {
        assertEquals(3, ChatMode.entries.size)
    }
}
