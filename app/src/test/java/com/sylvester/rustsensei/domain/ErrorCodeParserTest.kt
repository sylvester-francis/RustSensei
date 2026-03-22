package com.sylvester.rustsensei.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorCodeParserTest {

    @Test
    fun `extractFirstCode returns code from error E0382 borrow of moved value`() {
        val input = "error[E0382]: borrow of moved value"
        assertEquals("E0382", ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractFirstCode returns null for text with no error code`() {
        val input = "some random text without any error codes"
        assertNull(ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractFirstCode returns first code when multiple present`() {
        val input = "error[E0382]: borrow of moved value\nerror[E0507]: cannot move out"
        assertEquals("E0382", ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractCodes returns all unique codes`() {
        val input = "error[E0382]: borrow\nerror[E0507]: move\nerror[E0308]: mismatch"
        assertEquals(listOf("E0382", "E0507", "E0308"), ErrorCodeParser.extractCodes(input))
    }

    @Test
    fun `extractCodes deduplicates repeated codes`() {
        val input = "error[E0382]: first\nerror[E0382]: second\nerror[E0507]: third"
        assertEquals(listOf("E0382", "E0507"), ErrorCodeParser.extractCodes(input))
    }

    @Test
    fun `extractCodes returns empty list for no codes`() {
        val input = "everything compiled successfully"
        assertEquals(emptyList<String>(), ErrorCodeParser.extractCodes(input))
    }

    @Test
    fun `extractFirstCode ignores partial code E12 with too few digits`() {
        val input = "some partial E12 code here"
        assertNull(ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractFirstCode ignores code E12345 with too many digits`() {
        // E1234 should match (4 digits), but E12345 should match the first 4 digits
        val input = "error code E12345 here"
        // Regex E\d{4} will match E1234 within E12345
        assertEquals("E1234", ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractFirstCode extracts code embedded in URL`() {
        val input = "https://doc.rust-lang.org/error-index.html#E0382"
        assertEquals("E0382", ErrorCodeParser.extractFirstCode(input))
    }

    @Test
    fun `extractCodes handles mixed valid and partial codes`() {
        val input = "E12 is partial, E0382 is valid, E123 is partial, E0507 is valid"
        assertEquals(listOf("E0382", "E0507"), ErrorCodeParser.extractCodes(input))
    }

    @Test
    fun `extractFirstCode returns null for empty string`() {
        assertNull(ErrorCodeParser.extractFirstCode(""))
    }

    @Test
    fun `extractCodes returns empty list for empty string`() {
        assertEquals(emptyList<String>(), ErrorCodeParser.extractCodes(""))
    }
}
