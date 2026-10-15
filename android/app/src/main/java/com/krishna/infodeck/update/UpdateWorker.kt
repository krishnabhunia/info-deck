package com.krishna.infodeck.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.Prefs
import kotlinx.coroutines.flow.first
import java.time.Duration

/** Best-effort daily GitHub release check, on a network connection, without installing anything. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val beta = Prefs(applicationContext).flow.first().betaUpdates
        UpdateManager(applicationContext).check(force = false, includeBeta = beta)
        Result.success()
    } catch (t: Exception) {
        Logger.e("UpdateWorker", "GitHub update check failed", t)
        Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(Duration.ofDays(1))
                .setInitialDelay(Duration.ofDays(1)) // first check happens when the app starts
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "infodeck_github_update_check", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
