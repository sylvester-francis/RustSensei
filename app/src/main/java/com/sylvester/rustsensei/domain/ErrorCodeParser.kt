package com.sylvester.rustsensei.domain

/**
 * Extracts Rust compiler error codes (E0XXX pattern) from raw error output.
 */
object ErrorCodeParser {

    private val ERROR_CODE_REGEX = Regex("""E\d{4}""")

    /**
     * Extract all unique error codes from the input text.
     * Returns codes in the order they first appear.
     */
    fun extractCodes(input: String): List<String> =
        ERROR_CODE_REGEX.findAll(input)
            .map { it.value }
            .distinct()
            .toList()

    /**
     * Extract the first error code, or null if none found.
     */
    fun extractFirstCode(input: String): String? =
        ERROR_CODE_REGEX.find(input)?.value
}
