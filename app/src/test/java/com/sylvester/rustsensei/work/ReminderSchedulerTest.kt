package com.sylvester.rustsensei.work

import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulerTest {

    @Test
    fun `ReminderSchedulerImpl implements ReminderScheduler`() {
        // Verify at the type level that ReminderSchedulerImpl is a subtype of ReminderScheduler
        assertTrue(ReminderScheduler::class.java.isAssignableFrom(ReminderSchedulerImpl::class.java))
    }

    @Test
    fun `ReminderScheduler interface declares scheduleReminders method`() {
        val methods = ReminderScheduler::class.java.declaredMethods.map { it.name }
        assertTrue("scheduleReminders should be declared", methods.contains("scheduleReminders"))
    }

    @Test
    fun `ReminderScheduler interface declares cancelReminders method`() {
        val methods = ReminderScheduler::class.java.declaredMethods.map { it.name }
        assertTrue("cancelReminders should be declared", methods.contains("cancelReminders"))
    }
}
