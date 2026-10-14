package com.krishna.infodeck.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krishna.infodeck.core.Fmt
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.data.Prefs
import com.krishna.infodeck.data.Repo
import com.krishna.infodeck.data.RingAction
import com.krishna.infodeck.data.RingLogEntity
import com.krishna.infodeck.data.RingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * B1 fix: "Tomorrow" is stored as -1 and must NOT be multiplied as minutes, which
 * used to push the snooze into the past. Pure, so it is unit-tested.
 */
object Snooze {
    const val TOMORROW = -1

    fun targetMillis(nowMillis: Long, setting: Int, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
        if (setting == TOMORROW) {
            val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)
            val tomorrow9 = now.toLocalDate().plusDays(1).atTime(9, 0).atZone(zone)
            return tomorrow9.toInstant().toEpochMilli()
        }
        val safe = setting.coerceIn(1, 24 * 60)
        if (safe != setting) Logger.e("Snooze", "snooze setting " + setting + " out of range, using " + safe)
        return nowMillis + safe * 60_000L
    }
}

/** Fires every ring that is due now (or was missed while the phone was off). */
class RingReceiver : BroadcastReceiver() {

    companion object { const val ACTION_RING = "com.krishna.infodeck.RING" }

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        bgScope.launch {
            try {
                fireDue(context)
                AlarmScheduler.planNext(context)
            } catch (t: Throwable) {
                Logger.e("RingReceiver", "ring pass failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fireDue(context: Context) {
        val db = AppDatabase.get(context)
        val repo = Repo(db)
        val prefs = Prefs(context).flow.first()
        Notifications.ensureChannel(context, prefs.vibrate)
        val now = LocalDateTime.now()

        repo.allSchedulerViews().forEach { (view, card) ->
            val s = view.entity
            if (!s.enabled || s.status == RingStatus.DONE) return@forEach
            val at = view.ringAt ?: return@forEach
            if (at.isAfter(now)) return@forEach
            // Never re-fire the same moment twice.
            val firedFor = s.lastFiredAt?.let { Fmt.fromMillis(it) }
            if (firedFor != null && !firedFor.isBefore(at)) return@forEach

            val late = at.isBefore(now.minusMinutes(5))
            val detail = repo.cardDetail(card.id)
            val categoryName = detail?.categoryName ?: ""
            // Never put user-entered data values (which can contain secrets) on a notification.
            val firstData = detail?.data?.firstOrNull()?.name ?: ""
            val baseLine = view.baseDate?.let { " - ${view.dateName} ${Fmt.dateDow(it)}" } ?: ""

            Notifications.show(
                context = context,
                schedulerId = s.id,
                cardId = card.id,
                title = card.label,
                ruleLine = view.ruleText + baseLine,
                contextLine = listOf(categoryName, firstData).filter { it.isNotBlank() }.joinToString(" - "),
                snoozeMinutes = prefs.snoozeMinutes
            )

            db.dao().updateScheduler(
                s.copy(status = RingStatus.RANG, lastFiredAt = System.currentTimeMillis(), snoozedUntil = null)
            )
            db.dao().insertRingLog(
                RingLogEntity(
                    schedulerId = s.id, cardId = card.id, cardLabel = card.label,
                    categoryName = categoryName, ruleText = view.ruleText,
                    firedAt = System.currentTimeMillis(), late = late
                )
            )
            db.dao().trimRingLog()
        }
    }
}

/** Notification actions: Done, Snooze, swipe-away. */
class ActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.krishna.infodeck.DONE"
        const val ACTION_SNOOZE = "com.krishna.infodeck.SNOOZE"
        const val ACTION_DISMISS = "com.krishna.infodeck.DISMISS"
        const val EXTRA_SCHEDULER_ID = "scheduler_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val id = intent.getLongExtra(EXTRA_SCHEDULER_ID, -1L)
        if (id <= 0) { Logger.e("ActionReceiver", "missing scheduler id for $action"); return }
        val pending = goAsync()
        bgScope.launch {
            try {
                val db = AppDatabase.get(context)
                val dao = db.dao()
                val s = dao.scheduler(id)
                if (s == null) { Logger.e("ActionReceiver", "scheduler $id gone"); return@launch }
                val now = System.currentTimeMillis()
                when (action) {
                    ACTION_DONE -> {
                        dao.updateScheduler(s.copy(status = RingStatus.DONE, snoozedUntil = null))
                        dao.markRingAction(id, RingAction.DONE, now)
                        Notifications.cancel(context, id)
                    }
                    ACTION_SNOOZE -> {
                        val minutes = Prefs(context).flow.first().snoozeMinutes
                        dao.updateScheduler(
                            s.copy(status = RingStatus.SNOOZED, snoozedUntil = Snooze.targetMillis(now, minutes))
                        )
                        dao.markRingAction(id, RingAction.SNOOZED, now)
                        Notifications.cancel(context, id)
                    }
                    ACTION_DISMISS -> dao.markRingAction(id, RingAction.DISMISSED, now)
                }
                AlarmScheduler.planNext(context)
            } catch (t: Throwable) {
                Logger.e("ActionReceiver", "action $action failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Boot / package replace / time change / timezone change -> re-plan. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        bgScope.launch {
            try {
                Logger.e("BootReceiver", "re-planning after ${intent?.action}")
                AlarmScheduler.planNext(context)
            } catch (t: Throwable) {
                Logger.e("BootReceiver", "re-plan failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
