package com.gabow95k.keeply.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.gabow95k.keeply.R

object KeeplyNotificationChannels {

    const val INVENTORY_ALERTS = "inventory_alerts"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            INVENTORY_ALERTS,
            context.getString(R.string.notification_channel_inventory),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_inventory_desc)
        }
        manager.createNotificationChannel(channel)
    }
}
