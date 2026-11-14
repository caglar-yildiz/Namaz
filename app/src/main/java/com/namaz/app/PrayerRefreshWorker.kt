package com.namaz.app

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** WorkManager survives process death and device reboot; scheduling is not exact. */
class PrayerRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext
        try {
            val city = ReminderRefresh.city(app)
            if (city == null || !ReminderPreferences.enabled(app, "Namaz vakitleri")) {
                PrayerTimesService.cancelAll(app)
            } else {
                PrayerTimesService.scheduleNextDay(app, city)
            }
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}

object PrayerRefreshWork {
    private const val PERIODIC = "prayer_refresh_periodic"
    private const val IMMEDIATE = "prayer_refresh_immediate"
    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensurePeriodic(context: Context) {
        val work = PeriodicWorkRequestBuilder<PrayerRefreshWorker>(12, TimeUnit.HOURS)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC, ExistingPeriodicWorkPolicy.KEEP, work
        )
    }

    fun refreshNow(context: Context) {
        ensurePeriodic(context)
        val work = OneTimeWorkRequestBuilder<PrayerRefreshWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE, ExistingWorkPolicy.REPLACE, work
        )
    }
}
