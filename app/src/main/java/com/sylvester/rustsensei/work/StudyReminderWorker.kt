package com.sylvester.rustsensei.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sylvester.rustsensei.MainActivity
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StudyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val flashCardDao: FlashCardDao,
    private val progressRepository: ProgressRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "study_reminders"
        private const val NOTIFICATION_ID_FLASHCARDS = 1001
        private const val NOTIFICATION_ID_STREAK = 1002
    }

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val dueCards = flashCardDao.getDueCardCountSync(System.currentTimeMillis())
        if (dueCards > 0) {
            sendNotification(
                id = NOTIFICATION_ID_FLASHCARDS,
                title = applicationContext.getString(R.string.reminder_flashcards_title),
                body = applicationContext.resources.getQuantityString(
                    R.plurals.reminder_flashcards_body, dueCards, dueCards
                )
            )
        }

        val streak = progressRepository.calculateStreak()
        val hasStudiedToday = progressRepository.hasStudiedToday()
        if (streak > 0 && !hasStudiedToday) {
            sendNotification(
                id = NOTIFICATION_ID_STREAK,
                title = applicationContext.getString(R.string.reminder_streak_title),
                body = applicationContext.getString(R.string.reminder_streak_body, streak)
            )
        }

        return Result.success()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.study_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.study_reminders_channel_desc)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }
}
