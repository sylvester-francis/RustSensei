package com.sylvester.rustsensei.content

data class RagChunk(
    val id: String,
    val text: String,
    val keywords: List<String>,
    val sourceId: String,
    val sourceTitle: String
)
