package com.krishna.infodeck.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.krishna.infodeck.MainActivity
import com.krishna.infodeck.R
import com.krishna.infodeck.core.Logger

object Notifications {

    const val CHANNEL_ID = "reminders"
    const val GROUP_KEY = "com.krishna.infodeck.RINGS"
    private const val SUMMARY_ID = 1

    fun ensureChannel(context: Context, vibrate: Boolean) = Logger.guardUnit("Notif.channel") {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return@guardUnit
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val ch = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "InfoDeck card reminders"
                enableVibration(vibrate)
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }
    }

    fun show(
        context: Context, schedulerId: Long, cardId: Long,
        title: String, ruleLine: String, contextLine: String, snoozeMinutes: Int
    ) = Logger.guardUnit("Notif.show") {
        val nm = NotificationManagerCompat.from(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CARD_ID, cardId)
        }
        val openPi = PendingIntent.getActivity(
            context, schedulerId.toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val donePi = actionPi(context, schedulerId, ActionReceiver.ACTION_DONE, 100_000)
        val snoozePi = actionPi(context, schedulerId, ActionReceiver.ACTION_SNOOZE, 200_000)

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(ruleLine)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle("InfoDeck reminder")
                    .setContentText("Unlock to view reminder details")
                    .build()
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText("$ruleLine\n$contextLine"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(openPi)
            .addAction(0, "Done", donePi)
            .addAction(0, "Snooze ${snoozeLabel(snoozeMinutes)}", snoozePi)
            .setDeleteIntent(actionPi(context, schedulerId, ActionReceiver.ACTION_DISMISS, 300_000))
            .build()

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("InfoDeck")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        if (nm.areNotificationsEnabled()) {
            try {
                nm.notify(schedulerId.toInt(), n)
                nm.notify(SUMMARY_ID, summary)
            } catch (t: SecurityException) {
                Logger.e("Notif.show", "POST_NOTIFICATIONS missing", t)
            }
        } else {
            Logger.e("Notif.show", "notifications disabled by user; ring $schedulerId not shown")
        }
    }

    fun cancel(context: Context, schedulerId: Long) = Logger.guardUnit("Notif.cancel") {
        NotificationManagerCompat.from(context).cancel(schedulerId.toInt())
    }

    private fun actionPi(context: Context, schedulerId: Long, action: String, base: Int): PendingIntent {
        val i = Intent(context, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(ActionReceiver.EXTRA_SCHEDULER_ID, schedulerId)
        }
        return PendingIntent.getBroadcast(
            context, base + schedulerId.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun snoozeLabel(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 && minutes < 1440 -> "${minutes / 60} h"
        else -> "later"
    }

    fun canUseExactAlarms(context: Context): Boolean = Logger.guard("Notif.exact", false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) true
        else context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }
}
