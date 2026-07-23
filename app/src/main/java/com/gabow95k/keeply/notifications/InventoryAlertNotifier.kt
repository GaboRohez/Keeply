package com.gabow95k.keeply.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gabow95k.keeply.R
import com.gabow95k.keeply.presentation.controller.ControllerActivity

class InventoryAlertNotifier(private val context: Context) {

    fun notifyIfNeeded(
        expiredNames: List<String>,
        expiringSoonNames: List<String>,
        outOfStockNames: List<String>,
        lowStockNames: List<String>
    ) {
        if (expiredNames.isNotEmpty()) {
            show(
                id = ID_EXPIRED,
                title = context.getString(R.string.notification_expired_title),
                body = buildBody(expiredNames, R.string.notification_expired_body)
            )
        }
        if (expiringSoonNames.isNotEmpty()) {
            show(
                id = ID_EXPIRING_SOON,
                title = context.getString(R.string.notification_expiring_title),
                body = buildBody(expiringSoonNames, R.string.notification_expiring_body)
            )
        }
        if (outOfStockNames.isNotEmpty()) {
            show(
                id = ID_OUT_OF_STOCK,
                title = context.getString(R.string.notification_out_of_stock_title),
                body = buildBody(outOfStockNames, R.string.notification_out_of_stock_body)
            )
        }
        if (lowStockNames.isNotEmpty()) {
            show(
                id = ID_LOW_STOCK,
                title = context.getString(R.string.notification_low_stock_title),
                body = buildBody(lowStockNames, R.string.notification_low_stock_body)
            )
        }
    }

    fun notifyShoppingPrompt(title: String, body: String) {
        show(id = ID_SHOPPING_PROMPT, title = title, body = body)
    }

    private fun buildBody(names: List<String>, pluralRes: Int): String {
        val preview = names.take(MAX_NAMES_IN_BODY).joinToString(", ")
        val extra = names.size - MAX_NAMES_IN_BODY
        val listText = if (extra > 0) {
            context.getString(R.string.notification_names_more, preview, extra)
        } else {
            preview
        }
        return context.getString(pluralRes, names.size, listText)
    }

    private fun show(id: Int, title: String, body: String) {
        val intent = Intent(context, ControllerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, KeeplyNotificationChannels.INVENTORY_ALERTS)
                .setSmallIcon(R.drawable.ic_nav_alerts)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val MAX_NAMES_IN_BODY = 3
        private const val ID_EXPIRED = 1001
        private const val ID_EXPIRING_SOON = 1002
        private const val ID_OUT_OF_STOCK = 1003
        private const val ID_LOW_STOCK = 1004
        private const val ID_SHOPPING_PROMPT = 1005
    }
}
