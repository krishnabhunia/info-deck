package com.krishna.infodeck.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.krishna.infodeck.core.DueRules
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.data.Repo
import com.krishna.infodeck.data.RingStatus
import java.time.LocalDateTime

/**
 * One exact alarm for the earliest due ring; when it fires the next one is planned.
 * Re-planned on boot, time change, app open, every save and the daily sweep.
 */
object AlarmScheduler {

    private const val REQUEST = 4242

    suspend fun planNext(context: Context) = Logger.guardUnit("Alarm.plan") {
        val repo = Repo(AppDatabase.get(context))
        val now = LocalDateTime.now()
        var earliest: LocalDateTime? = null

        repo.allSchedulerViews().forEach { (view, _) ->
            val s = view.entity
            if (!s.enabled) return@forEach
            if (s.status == RingStatus.DONE) return@forEach
            val at = view.ringAt ?: return@forEach
            // Recovery: schedule a near-immediate ring for overdue items not fired before.
            // Without this, boot/sweep/reopen only consider future dates and silently lose reminders.
            if (at.isBefore(now) && !DueRules.needsDelivery(at, s.lastFiredAt)) return@forEach
            if (earliest == null || at.isBefore(earliest)) earliest = at
        }

        val am = context.getSystemService(AlarmManager::class.java)
        if (am == null) { Logger.e("Alarm.plan", "AlarmManager unavailable"); return@guardUnit }
        val pi = pendingIntent(context)
        am.cancel(pi)

        val target = earliest ?: run {
            Logger.e("Alarm.plan", "nothing to schedule (no future enabled ring)")
            return@guardUnit
        }
        val triggerAt = if (!target.isAfter(now)) System.currentTimeMillis() + 1_000L else Fmt.toMillis(target)
        try {
            if (Notifications.canUseExactAlarms(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                Logger.e("Alarm.plan", "exact alarms not permitted - using inexact window")
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (t: Throwable) {
            Logger.e("Alarm.plan", "scheduling failed, will retry on next re-plan", t)
        }
    }

    fun cancelAll(context: Context) = Logger.guardUnit("Alarm.cancel") {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val i = Intent(context, RingReceiver::class.java).setAction(RingReceiver.ACTION_RING)
        return PendingIntent.getBroadcast(
            context, REQUEST, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
