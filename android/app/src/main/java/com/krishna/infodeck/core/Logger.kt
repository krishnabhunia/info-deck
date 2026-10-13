package com.krishna.infodeck.core

import android.util.Log
import kotlinx.coroutines.CancellationException
import com.krishna.infodeck.data.AppDatabase
import com.krishna.infodeck.data.ErrorLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Every guard and every swallowed failure writes here; the entries surface on
 * Settings > Error Logs (three-companions rule, companion 2).
 */
object Logger {
    private const val TAG = "InfoDeck"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var db: AppDatabase? = null

    fun attach(database: AppDatabase) { db = database }

    fun e(tag: String, message: String, t: Throwable? = null) {
        Log.e(TAG, "$tag: $message", t)
        val d = db ?: return
        scope.launch {
            try {
                d.dao().insertError(
                    ErrorLogEntity(
                        ts = System.currentTimeMillis(),
                        tag = tag,
                        message = message,
                        detail = t?.let { "${it::class.java.simpleName}: ${it.message}" }
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Logger sink failed", e)   // never throw from the logger itself
            }
        }
    }

    /** Total function: runs the block, logs and returns the fallback on any failure. */
    inline fun <T> guard(tag: String, fallback: T, block: () -> T): T = try {
        block()
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        e(tag, "guarded failure", t)
        fallback
    }

    inline fun guardUnit(tag: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) {
            if (t is CancellationException) throw t
            e(tag, "guarded failure", t)
        }
    }
}
