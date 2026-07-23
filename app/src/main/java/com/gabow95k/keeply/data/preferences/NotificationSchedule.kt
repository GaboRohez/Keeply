package com.gabow95k.keeply.data.preferences

enum class NotificationCadence(val days: Int) {
    DAILY(1),
    EVERY_3_DAYS(3),
    WEEKLY(7);

    companion object {
        fun fromStorage(value: String): NotificationCadence =
            entries.firstOrNull { it.name == value } ?: DAILY
    }
}

enum class NotificationTimeSlot(val hourStart: Int, val hourEnd: Int) {
    MORNING(7, 10),
    AFTERNOON(13, 16),
    EVENING(19, 22);

    companion object {
        fun fromStorage(value: String): NotificationTimeSlot? =
            entries.firstOrNull { it.name == value }

        fun defaultsForTimesPerDay(timesPerDay: Int): Set<NotificationTimeSlot> =
            when (timesPerDay) {
                1 -> setOf(MORNING)
                2 -> setOf(MORNING, EVENING)
                else -> setOf(MORNING, AFTERNOON, EVENING)
            }
    }
}
