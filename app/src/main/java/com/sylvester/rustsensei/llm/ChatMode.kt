package com.sylvester.rustsensei.llm

enum class ChatMode {
    DIRECT,
    SOCRATIC,
    RUBBER_DUCK;

    companion object {
        fun fromString(value: String): ChatMode =
            entries.find { it.name == value } ?: DIRECT
    }
}
