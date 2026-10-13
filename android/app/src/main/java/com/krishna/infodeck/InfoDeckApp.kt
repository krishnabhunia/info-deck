package com.krishna.infodeck

import android.app.Application
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.work.SweepWorker

class InfoDeckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.attach(AppDatabase.get(this))
        SweepWorker.schedule(this)
    }
}
