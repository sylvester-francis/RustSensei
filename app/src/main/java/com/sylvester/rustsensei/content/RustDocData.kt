package com.sylvester.rustsensei.content

data class DocEntry(
    val id: String,
    val typeName: String,
    val module: String,
    val signature: String,
    val description: String,
    val methods: List<DocMethod>
)

data class DocMethod(
    val name: String,
    val signature: String,
    val description: String,
    val example: String
)

data class DocIndexEntry(
    val id: String,
    val name: String,
    val module: String
)
