package com.gabow95k.keeply.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class KeeplyPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, value) }

    var notifyExpired: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_EXPIRED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_EXPIRED, value) }

    var notifyExpiringSoon: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_EXPIRING_SOON, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_EXPIRING_SOON, value) }

    var notifyOutOfStock: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_OUT_OF_STOCK, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_OUT_OF_STOCK, value) }

    var notifyLowStock: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_LOW_STOCK, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY_LOW_STOCK, value) }

    var expiringSoonDays: Int
        get() = prefs.getInt(KEY_EXPIRING_SOON_DAYS, DEFAULT_EXPIRING_SOON_DAYS)
        set(value) = prefs.edit { putInt(KEY_EXPIRING_SOON_DAYS, value) }

    var notificationCadence: NotificationCadence
        get() = NotificationCadence.fromStorage(
            prefs.getString(KEY_NOTIFICATION_CADENCE, NotificationCadence.DAILY.name)
                ?: NotificationCadence.DAILY.name
        )
        set(value) = prefs.edit { putString(KEY_NOTIFICATION_CADENCE, value.name) }

    var notificationTimesPerDay: Int
        get() = prefs.getInt(KEY_NOTIFICATION_TIMES_PER_DAY, 1).coerceIn(1, 3)
        set(value) = prefs.edit { putInt(KEY_NOTIFICATION_TIMES_PER_DAY, value.coerceIn(1, 3)) }

    var notificationSlots: Set<NotificationTimeSlot>
        get() {
            val raw = prefs.getString(KEY_NOTIFICATION_SLOTS, null)
            if (raw.isNullOrBlank()) {
                return NotificationTimeSlot.defaultsForTimesPerDay(notificationTimesPerDay)
            }
            val parsed = raw.split(',')
                .mapNotNull { NotificationTimeSlot.fromStorage(it.trim()) }
                .toSet()
            return parsed.ifEmpty {
                NotificationTimeSlot.defaultsForTimesPerDay(notificationTimesPerDay)
            }
        }
        set(value) {
            val ordered = NotificationTimeSlot.entries.filter { it in value }
            prefs.edit {
                putString(KEY_NOTIFICATION_SLOTS, ordered.joinToString(",") { it.name })
            }
        }

    var notificationAnchorEpochDay: Long
        get() = prefs.getLong(KEY_NOTIFICATION_ANCHOR_DAY, -1L)
        set(value) = prefs.edit { putLong(KEY_NOTIFICATION_ANCHOR_DAY, value) }

    var lastNotifiedSlotStamp: String
        get() = prefs.getString(KEY_LAST_NOTIFIED_SLOT, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_LAST_NOTIFIED_SLOT, value) }

    fun applyTimesPerDay(times: Int) {
        notificationTimesPerDay = times
        notificationSlots = NotificationTimeSlot.defaultsForTimesPerDay(times)
    }

    companion object {
        const val PREFS_NAME = "keeply_prefs"
        const val DEFAULT_EXPIRING_SOON_DAYS = 7

        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFY_EXPIRED = "notify_expired"
        private const val KEY_NOTIFY_EXPIRING_SOON = "notify_expiring_soon"
        private const val KEY_NOTIFY_OUT_OF_STOCK = "notify_out_of_stock"
        private const val KEY_NOTIFY_LOW_STOCK = "notify_low_stock"
        private const val KEY_EXPIRING_SOON_DAYS = "expiring_soon_days"
        private const val KEY_NOTIFICATION_CADENCE = "notification_cadence"
        private const val KEY_NOTIFICATION_TIMES_PER_DAY = "notification_times_per_day"
        private const val KEY_NOTIFICATION_SLOTS = "notification_slots"
        private const val KEY_NOTIFICATION_ANCHOR_DAY = "notification_anchor_day"
        private const val KEY_LAST_NOTIFIED_SLOT = "last_notified_slot"

        @Volatile
        private var instance: KeeplyPreferences? = null

        fun getInstance(context: Context): KeeplyPreferences {
            return instance ?: synchronized(this) {
                instance ?: KeeplyPreferences(context).also { instance = it }
            }
        }
    }
}
