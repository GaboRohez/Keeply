package com.gabow95k.keeply.insights

import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyInsightsEvaluatorTest {

    @Test
    fun evaluate_buildsUsageStatsAndRestockCards() {
        val events = listOf(
            event("Café", StockChangeEventEntity.TYPE_CONSUME, 3.0, 5.0, 2.0),
            event("Café", StockChangeEventEntity.TYPE_ADJUST_DOWN, 2.0, 2.0, 0.0),
            event("Arroz", StockChangeEventEntity.TYPE_ADD, 4.0, 0.0, 4.0)
        )
        val inventory = listOf(
            InventoryItemEntity(id = 1, categoryId = 1, name = "Café", quantity = 0.0),
            InventoryItemEntity(
                id = 2,
                categoryId = 1,
                name = "Jabón",
                quantity = 1.0,
                minQuantity = 2.0
            )
        )

        val result = MonthlyInsightsEvaluator.evaluate(events, inventory)

        assertEquals(3, result.cards.size)
        assertEquals(InsightKind.MOST_USED, result.cards[0].kind)
        assertEquals(InsightKind.RAN_OUT, result.cards[1].kind)
        assertEquals(InsightKind.BUY_MORE, result.cards[2].kind)
        assertTrue(result.showShoppingCta)
        assertEquals(5.0, result.stats.totalConsumed, 0.0)
        assertEquals(3, result.stats.movementCount)
        assertEquals(2, result.stats.productsTouched)
        assertEquals(100, result.stats.topProducts.first().progressPercent)
    }

    @Test
    fun evaluate_returnsEmptyTrackingStateWithoutActivityOrLowStock() {
        val inventory = listOf(
            InventoryItemEntity(id = 1, categoryId = 1, name = "Arroz", quantity = 5.0)
        )
        val result = MonthlyInsightsEvaluator.evaluate(emptyList(), inventory)
        assertEquals(listOf(InsightKind.EMPTY_TRACKING), result.cards.map { it.kind })
        assertEquals(0, result.stats.movementCount)
        assertTrue(result.stats.topProducts.isEmpty())
    }

    private fun event(
        name: String,
        type: String,
        delta: Double,
        before: Double,
        after: Double
    ) = StockChangeEventEntity(
        inventoryItemId = 1,
        productName = name,
        changeType = type,
        delta = delta,
        quantityBefore = before,
        quantityAfter = after
    )
}
