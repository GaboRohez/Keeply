package com.gabow95k.keeply.notifications

import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.data.preferences.NotificationTimeSlot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object NotificationScheduleEvaluator {

    fun shouldNotifyNow(
        prefs: KeeplyPreferences,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!prefs.notificationsEnabled) return false

        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate()
        val hour = now.hour

        if (!isCadenceDay(today, prefs)) return false

        val activeSlot = prefs.notificationSlots.firstOrNull { slot ->
            hour in slot.hourStart until slot.hourEnd
        } ?: return false

        val stamp = slotStamp(today, activeSlot)
        if (prefs.lastNotifiedSlotStamp == stamp) return false

        prefs.lastNotifiedSlotStamp = stamp
        return true
    }

    fun isCadenceDay(today: LocalDate, prefs: KeeplyPreferences): Boolean {
        val anchorDay = prefs.notificationAnchorEpochDay
        if (anchorDay < 0L) return true
        val todayEpoch = today.toEpochDay()
        if (todayEpoch < anchorDay) return false
        val interval = prefs.notificationCadence.days.toLong()
        return (todayEpoch - anchorDay) % interval == 0L
    }

    fun ensureAnchor(prefs: KeeplyPreferences) {
        if (prefs.notificationAnchorEpochDay < 0L) {
            prefs.notificationAnchorEpochDay = LocalDate.now().toEpochDay()
        }
    }

    fun daysUntilNextCadence(prefs: KeeplyPreferences): Long {
        val today = LocalDate.now()
        val anchor = prefs.notificationAnchorEpochDay.takeIf { it >= 0 } ?: today.toEpochDay()
        val interval = prefs.notificationCadence.days.toLong()
        val todayEpoch = today.toEpochDay()
        if (todayEpoch < anchor) return ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(anchor))
        val rem = (todayEpoch - anchor) % interval
        return if (rem == 0L) 0L else interval - rem
    }

    private fun slotStamp(date: LocalDate, slot: NotificationTimeSlot): String =
        "${date.toEpochDay()}_${slot.name}"
}
