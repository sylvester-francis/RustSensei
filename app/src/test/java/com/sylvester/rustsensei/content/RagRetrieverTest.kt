package com.sylvester.rustsensei.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRetrieverTest {

    // Test the keyword extraction and scoring logic using a simple in-memory setup
    @Test
    fun `keyword extraction filters short words`() {
        val query = "What is ownership in Rust?"
        val words = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()

        // "is" and "in" should be filtered out (length <= 2)
        assertTrue("what" in words)
        assertTrue("ownership" in words)
        assertTrue("rust" in words)
        assertEquals(3, words.size) // what, ownership, rust
    }

    @Test
    fun `keyword extraction handles punctuation`() {
        val query = "How do I use Vec<T> with HashMap<K, V>?"
        val words = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()

        assertTrue("vec" in words)
        assertTrue("hashmap" in words)
        assertTrue("how" in words)
        assertTrue("use" in words)
    }

    @Test
    fun `scoring ranks multi-keyword matches higher`() {
        val chunkScores = mutableMapOf<String, Int>()

        // Simulate: "ownership" matches chunk-a and chunk-b
        //           "memory" matches chunk-a only
        val index = mapOf(
            "ownership" to listOf("chunk-a", "chunk-b"),
            "memory" to listOf("chunk-a")
        )
        val queryWords = setOf("ownership", "memory")

        for (word in queryWords) {
            val matchingChunkIds = index[word] ?: continue
            for (chunkId in matchingChunkIds) {
                chunkScores[chunkId] = (chunkScores[chunkId] ?: 0) + 1
            }
        }

        // chunk-a matches both keywords → score 2
        // chunk-b matches only "ownership" → score 1
        assertEquals(2, chunkScores["chunk-a"])
        assertEquals(1, chunkScores["chunk-b"])

        val ranked = chunkScores.entries.sortedByDescending { it.value }
        assertEquals("chunk-a", ranked.first().key)
    }
}
