package com.gabow95k.keeply.data.local

import com.gabow95k.keeply.data.local.dao.StockChangeEventDao
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import kotlin.math.abs

object StockChangeLogger {

    suspend fun logConsume(
        dao: StockChangeEventDao,
        itemId: Long,
        productName: String,
        quantityBefore: Double,
        quantityAfter: Double
    ) {
        val delta = quantityBefore - quantityAfter
        if (delta <= 0.0) return
        dao.insert(
            StockChangeEventEntity(
                inventoryItemId = itemId,
                productName = productName,
                changeType = StockChangeEventEntity.TYPE_CONSUME,
                delta = delta,
                quantityBefore = quantityBefore,
                quantityAfter = quantityAfter
            )
        )
    }

    suspend fun logQuantityEdit(
        dao: StockChangeEventDao,
        itemId: Long,
        productName: String,
        quantityBefore: Double,
        quantityAfter: Double
    ) {
        val delta = abs(quantityAfter - quantityBefore)
        if (delta <= 0.0) return
        val type = when {
            quantityAfter < quantityBefore -> StockChangeEventEntity.TYPE_ADJUST_DOWN
            else -> StockChangeEventEntity.TYPE_ADJUST_UP
        }
        dao.insert(
            StockChangeEventEntity(
                inventoryItemId = itemId,
                productName = productName,
                changeType = type,
                delta = delta,
                quantityBefore = quantityBefore,
                quantityAfter = quantityAfter
            )
        )
    }

    suspend fun logAdd(
        dao: StockChangeEventDao,
        itemId: Long,
        productName: String,
        quantity: Double
    ) {
        dao.insert(
            StockChangeEventEntity(
                inventoryItemId = itemId,
                productName = productName,
                changeType = StockChangeEventEntity.TYPE_ADD,
                delta = quantity.coerceAtLeast(0.0),
                quantityBefore = 0.0,
                quantityAfter = quantity.coerceAtLeast(0.0)
            )
        )
    }
}
