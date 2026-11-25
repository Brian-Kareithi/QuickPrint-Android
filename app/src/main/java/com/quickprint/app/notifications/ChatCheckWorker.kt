package com.quickprint.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quickprint.app.data.Prefs
import java.util.concurrent.TimeUnit

/**
 * Background check for shop replies while the app is closed. Shows nothing unless a reply arrived.
 *
 * For 2 hours after the customer messages a shop it runs about every minute (a reply is most likely
 * then), afterwards every 15 minutes (Android's minimum). While the app is open, MainActivity checks
 * every 10 seconds instead. Android may stretch these intervals when the phone is idle (Doze).
 */
class ChatCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ChatChecker.checkAll(applicationContext)
        val prefs = Prefs(applicationContext)
        if (System.currentTimeMillis() < prefs.fastCheckUntil && prefs.allChats().isNotEmpty()) {
            // APPEND_OR_REPLACE: the next check waits for this one to finish (REPLACE would cancel this run).
            ChatChecks.enqueueFast(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        return Result.success()
    }
}

object ChatChecks {
    private const val PERIODIC = "chat-check-periodic"
    private const val FAST = "chat-check-fast"
    private const val FAST_WINDOW_MS = 2 * 60 * 60 * 1000L

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Background checks every 15 minutes while the phone has any chat. Safe to call often. */
    fun schedule(context: Context) {
        if (Prefs(context).allChats().isEmpty()) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ChatCheckWorker>(15, TimeUnit.MINUTES).setConstraints(online).build(),
        )
    }

    /** The customer just messaged a shop: check about every minute for the next 2 hours. */
    fun boost(context: Context) {
        Prefs(context).fastCheckUntil = System.currentTimeMillis() + FAST_WINDOW_MS
        schedule(context)
        enqueueFast(context, ExistingWorkPolicy.KEEP) // one is already coming within a minute
    }

    internal fun enqueueFast(context: Context, policy: ExistingWorkPolicy) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            FAST,
            policy,
            OneTimeWorkRequestBuilder<ChatCheckWorker>().setInitialDelay(60, TimeUnit.SECONDS).setConstraints(online).build(),
        )
    }
}
