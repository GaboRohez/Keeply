package com.gabow95k.keeply.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import java.util.concurrent.TimeUnit

class InventoryAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = KeeplyPreferences.getInstance(applicationContext)
        if (!prefs.notificationsEnabled) return Result.success()

        val force = tags.contains(InventoryAlertScheduler.ONE_SHOT_TAG) ||
                inputData.getBoolean(InventoryAlertScheduler.KEY_FORCE, false)

        if (!force && !NotificationScheduleEvaluator.shouldNotifyNow(prefs)) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val expiringLimit = now + TimeUnit.DAYS.toMillis(prefs.expiringSoonDays.toLong())
        val items = KeeplyDatabase.getInstance(applicationContext).inventoryItemDao().getAll()

        val expired = if (prefs.notifyExpired) {
            items.filter { item ->
                val date = item.expirationDate ?: return@filter false
                date < now
            }.map { it.name }
        } else {
            emptyList()
        }

        val expiringSoon = if (prefs.notifyExpiringSoon) {
            items.filter { item ->
                val date = item.expirationDate ?: return@filter false
                date in now..expiringLimit
            }.map { it.name }
        } else {
            emptyList()
        }

        val outOfStock = if (prefs.notifyOutOfStock) {
            items.filter { it.quantity <= 0.0 }.map { it.name }
        } else {
            emptyList()
        }

        val lowStock = if (prefs.notifyLowStock) {
            items.filter { item ->
                if (item.quantity <= 0.0) return@filter false
                val min = item.minQuantity ?: return@filter false
                item.quantity <= min
            }.map { it.name }
        } else {
            emptyList()
        }

        KeeplyNotificationChannels.ensureCreated(applicationContext)
        val notifier = InventoryAlertNotifier(applicationContext)

        if (expired.isNotEmpty() || expiringSoon.isNotEmpty() || outOfStock.isNotEmpty() || lowStock.isNotEmpty()) {
            notifier.notifyIfNeeded(
                expiredNames = expired,
                expiringSoonNames = expiringSoon,
                outOfStockNames = outOfStock,
                lowStockNames = lowStock
            )
        }

        if (prefs.notifyShoppingPrompts) {
            val calendar = java.util.Calendar.getInstance()
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val monthKey = calendar.get(java.util.Calendar.YEAR) * 100 +
                    (calendar.get(java.util.Calendar.MONTH) + 1)
            val dayKey = monthKey * 100 + day
            val restockCount = outOfStock.size + lowStock.size

            if (day >= 25 && prefs.lastMonthEndPromptMonth != monthKey) {
                notifier.notifyShoppingPrompt(
                    title = applicationContext.getString(R.string.notification_shopping_month_title),
                    body = applicationContext.getString(R.string.notification_shopping_month_body)
                )
                prefs.lastMonthEndPromptMonth = monthKey
            } else if (restockCount > 0 && prefs.lastLowStockPromptDay != dayKey) {
                notifier.notifyShoppingPrompt(
                    title = applicationContext.getString(R.string.notification_shopping_stock_title),
                    body = applicationContext.getString(
                        R.string.notification_shopping_stock_body,
                        restockCount
                    )
                )
                prefs.lastLowStockPromptDay = dayKey
            }
        }

        return Result.success()
    }
}
