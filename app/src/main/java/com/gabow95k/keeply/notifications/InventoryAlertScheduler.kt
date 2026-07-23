package com.gabow95k.keeply.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object InventoryAlertScheduler {

    private const val UNIQUE_WORK_NAME = "keeply_inventory_alerts"
    private const val CHECK_INTERVAL_HOURS = 1L

    fun schedule(context: Context) {
        KeeplyNotificationChannels.ensureCreated(context)
        val request = PeriodicWorkRequestBuilder<InventoryAlertWorker>(
            CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun runOnceNow(context: Context) {
        KeeplyNotificationChannels.ensureCreated(context)
        val request = androidx.work.OneTimeWorkRequestBuilder<InventoryAlertWorker>()
            .addTag(ONE_SHOT_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    const val ONE_SHOT_TAG = "keeply_inventory_alerts_oneshot"
    const val KEY_FORCE = "force"
}
