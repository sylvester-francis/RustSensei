package com.sylvester.rustsensei.content

import android.content.Context
import android.util.Log
import org.json.JSONObject

class RagRetriever(private val context: Context) {

    private var chunks: List<RagChunk>? = null
    private var keywordIndex: Map<String, List<String>>? = null

    private fun ensureLoaded() {
        if (chunks != null && keywordIndex != null) return

        try {
            // Load chunks
            val chunksJson = context.assets.open("rag/chunks.json")
                .bufferedReader().use { it.readText() }
            val chunksObj = JSONObject(chunksJson)
            val chunksArray = chunksObj.getJSONArray("chunks")

            val loadedChunks = mutableListOf<RagChunk>()
            for (i in 0 until chunksArray.length()) {
                val chunk = chunksArray.getJSONObject(i)
                val keywords = mutableListOf<String>()
                val kw = chunk.getJSONArray("keywords")
                for (j in 0 until kw.length()) {
                    keywords.add(kw.getString(j).lowercase())
                }
                loadedChunks.add(RagChunk(
                    id = chunk.getString("id"),
                    text = chunk.getString("text"),
                    keywords = keywords,
                    sourceId = chunk.getString("sourceId"),
                    sourceTitle = chunk.getString("sourceTitle")
                ))
            }
            chunks = loadedChunks

            // Load keyword index
            val keywordsJson = context.assets.open("rag/keywords.json")
                .bufferedReader().use { it.readText() }
            val keywordsObj = JSONObject(keywordsJson)
            val loadedIndex = mutableMapOf<String, List<String>>()
            for (key in keywordsObj.keys()) {
                val chunkIds = mutableListOf<String>()
                val arr = keywordsObj.getJSONArray(key)
                for (i in 0 until arr.length()) {
                    chunkIds.add(arr.getString(i))
                }
                loadedIndex[key.lowercase()] = chunkIds
            }
            keywordIndex = loadedIndex
        } catch (e: Exception) {
            // If RAG files don't exist yet, use empty data
            chunks = emptyList()
            keywordIndex = emptyMap()
        }
    }

    fun retrieveContext(query: String, topK: Int = 3): String? {
        return try {
            ensureLoaded()

            val allChunks = chunks ?: return null
            val index = keywordIndex ?: return null

            if (allChunks.isEmpty()) return null

            // Extract query keywords
            val queryWords = query.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 }
                .toSet()

            // Find matching chunk IDs via keyword index
            val chunkScores = mutableMapOf<String, Int>()
            for (word in queryWords) {
                val matchingChunkIds = index[word] ?: continue
                for (chunkId in matchingChunkIds) {
                    chunkScores[chunkId] = (chunkScores[chunkId] ?: 0) + 1
                }
            }

            // Also do direct keyword matching on chunks
            for (chunk in allChunks) {
                val overlap = chunk.keywords.count { it in queryWords }
                if (overlap > 0) {
                    chunkScores[chunk.id] = (chunkScores[chunk.id] ?: 0) + overlap
                }
            }

            if (chunkScores.isEmpty()) return null

            // Get top-K chunks by score
            val topChunkIds = chunkScores.entries
                .sortedByDescending { it.value }
                .take(topK)
                .map { it.key }

            val chunkMap = allChunks.associateBy { it.id }
            val topChunks = topChunkIds.mapNotNull { chunkMap[it] }

            if (topChunks.isEmpty()) return null

            topChunks.joinToString("\n\n---\n\n") { chunk ->
                "From: ${chunk.sourceTitle}\n${chunk.text}"
            }
        } catch (e: Exception) {
            Log.e("RagRetriever", "Error in retrieveContext: ${e.message}", e)
            null
        }
    }
}
