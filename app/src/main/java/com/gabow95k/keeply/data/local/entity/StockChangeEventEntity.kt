package com.gabow95k.keeply.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_change_events",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["inventoryItemId"]),
        Index(value = ["changeType"])
    ]
)
data class StockChangeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long?,
    val productName: String,
    /** CONSUME | ADJUST_DOWN | ADJUST_UP | ADD */
    val changeType: String,
    /** Absolute amount changed (always >= 0). */
    val delta: Double,
    val quantityBefore: Double,
    val quantityAfter: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CONSUME = "CONSUME"
        const val TYPE_ADJUST_DOWN = "ADJUST_DOWN"
        const val TYPE_ADJUST_UP = "ADJUST_UP"
        const val TYPE_ADD = "ADD"
    }
}
