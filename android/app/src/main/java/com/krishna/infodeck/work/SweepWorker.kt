package com.krishna.infodeck.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.krishna.infodeck.alarm.AlarmScheduler
import com.krishna.infodeck.core.Logger
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Belt-and-braces daily re-plan at 00:05 (D1 C3 row 2). */
class SweepWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = try {
        AlarmScheduler.planNext(applicationContext)
        Result.success()
    } catch (t: Throwable) {
        Logger.e("SweepWorker", "daily sweep failed", t)
        Result.retry()
    }

    companion object {
        private const val NAME = "infodeck_daily_sweep"

        fun schedule(context: Context) = Logger.guardUnit("SweepWorker.schedule") {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(0, 5))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next)
            val req = PeriodicWorkRequestBuilder<SweepWorker>(Duration.ofDays(1))
                .setInitialDelay(delay)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
