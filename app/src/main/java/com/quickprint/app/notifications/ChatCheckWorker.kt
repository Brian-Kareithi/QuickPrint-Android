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

class ChatCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ChatChecker.checkAll(applicationContext)
        val prefs = Prefs(applicationContext)
        if (System.currentTimeMillis() < prefs.fastCheckUntil && prefs.allChats().isNotEmpty()) {
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

    fun schedule(context: Context) {
        if (Prefs(context).allChats().isEmpty()) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ChatCheckWorker>(15, TimeUnit.MINUTES).setConstraints(online).build(),
        )
    }

    fun boost(context: Context) {
        Prefs(context).fastCheckUntil = System.currentTimeMillis() + FAST_WINDOW_MS
        schedule(context)
        enqueueFast(context, ExistingWorkPolicy.KEEP)
    }

    internal fun enqueueFast(context: Context, policy: ExistingWorkPolicy) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            FAST,
            policy,
            OneTimeWorkRequestBuilder<ChatCheckWorker>().setInitialDelay(60, TimeUnit.SECONDS).setConstraints(online).build(),
        )
    }
}
