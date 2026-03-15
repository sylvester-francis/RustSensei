package com.sylvester.rustsensei.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepositoryTest {

    @Test
    fun `ThreadLocal SimpleDateFormat produces correct date format`() {
        val dateFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
        val result = dateFormat.get()!!.format(Date())
        // Should be in yyyy-MM-dd format
        assertNotNull(result)
        assertEquals(10, result.length) // "2026-03-16" is always 10 chars
        assertEquals('-', result[4])
        assertEquals('-', result[7])
    }

    @Test
    fun `ThreadLocal SimpleDateFormat is thread-safe`() {
        val dateFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }

        val results = mutableListOf<String>()
        val threads = (1..10).map {
            Thread {
                repeat(100) {
                    val result = dateFormat.get()!!.format(Date())
                    synchronized(results) {
                        results.add(result)
                    }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // All results should be valid dates
        assertEquals(1000, results.size)
        results.forEach { date ->
            assertEquals(10, date.length)
            assertEquals('-', date[4])
            assertEquals('-', date[7])
        }
    }
}
