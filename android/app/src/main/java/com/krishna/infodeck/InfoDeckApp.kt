package com.krishna.infodeck

import android.app.Application
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.work.SweepWorker
import com.krishna.infodeck.update.UpdateManager
import com.krishna.infodeck.update.UpdateWorker
import com.krishna.infodeck.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InfoDeckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.attach(AppDatabase.get(this))
        SweepWorker.schedule(this)
        UpdateWorker.schedule(this)
        // Recheck at app start if the successful GitHub check is at least 24 hours old.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Remember the last stable version so a later beta can return to it.
                com.krishna.infodeck.update.ReturnPlanner.stableToRecord(com.krishna.infodeck.BuildConfig.VERSION_NAME)
                    ?.let { Prefs(this@InfoDeckApp).setLastStableVersion(it) }
            } catch (e: Exception) {
                Logger.e("InfoDeckApp", "could not record stable version", e)
            }
            try {
                val beta = Prefs(this@InfoDeckApp).flow.first().betaUpdates
                UpdateManager(this@InfoDeckApp).check(force = false, includeBeta = beta)
            } catch (e: Exception) {
                Logger.e("InfoDeckApp", "update check unavailable", e)
            }
        }
    }
}
