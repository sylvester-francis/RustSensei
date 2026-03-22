package com.sylvester.rustsensei.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface ReminderScheduler {
    fun scheduleReminders()
    fun cancelReminders()
}

class ReminderSchedulerImpl @Inject constructor(
    private val context: Context
) : ReminderScheduler {

    companion object {
        private const val WORK_NAME = "study_reminder"
        private const val REPEAT_INTERVAL_HOURS = 4L
    }

    override fun scheduleReminders() {
        val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(
            REPEAT_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
