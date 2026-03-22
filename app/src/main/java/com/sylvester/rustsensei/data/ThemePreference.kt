package com.sylvester.rustsensei.data

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT;

    companion object {
        fun fromString(value: String): ThemePreference =
            entries.find { it.name == value } ?: SYSTEM
    }
}
