package com.sylvester.rustsensei.llm

import com.sylvester.rustsensei.data.ChatMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTemplateFormatterTest {

    @Test
    fun `formatMessages includes system prompt`() {
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "Hello")
        )
        val result = ChatTemplateFormatter.formatMessages(messages)
        assertTrue(result.contains("<|im_start|>system"))
        assertTrue(result.contains("RustSensei"))
    }

    @Test
    fun `formatMessages includes user message`() {
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "What is ownership?")
        )
        val result = ChatTemplateFormatter.formatMessages(messages)
        assertTrue(result.contains("What is ownership?"))
        assertTrue(result.contains("<|im_start|>user"))
    }

    @Test
    fun `formatMessages ends with assistant prompt`() {
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "Hello")
        )
        val result = ChatTemplateFormatter.formatMessages(messages)
        assertTrue(result.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun `formatMessages injects RAG context`() {
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "Hello")
        )
        val result = ChatTemplateFormatter.formatMessages(
            messages,
            ragContext = "Ownership means each value has one owner."
        )
        assertTrue(result.contains("[CONTEXT]"))
        assertTrue(result.contains("Ownership means each value has one owner."))
        assertTrue(result.contains("[/CONTEXT]"))
    }

    @Test
    fun `formatMessages truncates long RAG context`() {
        val longContext = "a".repeat(5000)
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "Hello")
        )
        val result = ChatTemplateFormatter.formatMessages(
            messages,
            ragContext = longContext
        )
        assertTrue(result.contains("[...truncated]"))
        assertFalse(result.contains("a".repeat(5000)))
    }

    @Test
    fun `formatMessages applies sliding window for long conversations`() {
        val messages = (1..100).map { i ->
            ChatMessage(
                id = i.toLong(),
                conversationId = 1,
                role = if (i % 2 == 1) "user" else "assistant",
                content = "Message number $i with some content that takes space " + "x".repeat(100)
            )
        }
        val result = ChatTemplateFormatter.formatMessages(messages, contextLength = 2048)
        // Should include the last message
        assertTrue(result.contains("Message number 100"))
        // Should NOT include the first message (it was truncated by the window)
        assertFalse(result.contains("Message number 1 "))
    }

    @Test
    fun `formatMessages with null ragContext does not inject CONTEXT tags`() {
        val messages = listOf(
            ChatMessage(id = 1, conversationId = 1, role = "user", content = "Hello")
        )
        val result = ChatTemplateFormatter.formatMessages(messages, ragContext = null)
        assertFalse(result.contains("[CONTEXT]"))
    }
}
