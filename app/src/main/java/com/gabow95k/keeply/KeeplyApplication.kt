package com.gabow95k.keeply

import android.app.Application
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.notifications.InventoryAlertScheduler
import com.gabow95k.keeply.notifications.KeeplyNotificationChannels

class KeeplyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        KeeplyNotificationChannels.ensureCreated(this)
        if (KeeplyPreferences.getInstance(this).notificationsEnabled) {
            InventoryAlertScheduler.schedule(this)
        }
    }
}
