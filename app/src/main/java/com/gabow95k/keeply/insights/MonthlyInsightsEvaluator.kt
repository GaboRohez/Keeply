package com.gabow95k.keeply.insights

import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import java.util.Calendar

enum class InsightKind {
    MOST_USED,
    RAN_OUT,
    BUY_MORE,
    EMPTY_TRACKING
}

data class InsightCard(
    val kind: InsightKind,
    val productName: String? = null,
    val amount: Double? = null,
    val showShoppingCta: Boolean = false
)

data class MonthlyInsights(
    val monthLabelKey: Int,
    val cards: List<InsightCard>,
    val showShoppingCta: Boolean
)

object MonthlyInsightsEvaluator {

    private const val MAX_CARDS = 3

    fun monthStartMillis(now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun evaluate(
        events: List<StockChangeEventEntity>,
        inventory: List<InventoryItemEntity>
    ): MonthlyInsights {
        val cards = mutableListOf<InsightCard>()

        val consumeByItem = events
            .filter {
                it.changeType == StockChangeEventEntity.TYPE_CONSUME ||
                        it.changeType == StockChangeEventEntity.TYPE_ADJUST_DOWN
            }
            .groupBy { it.inventoryItemId to it.productName }
            .mapValues { (_, list) ->
                list.sumOf { it.delta } to list.maxByOrNull { it.createdAt }
            }

        val topUsed = consumeByItem
            .filter { it.value.first > 0.0 }
            .maxByOrNull { it.value.first }

        if (topUsed != null) {
            cards += InsightCard(
                kind = InsightKind.MOST_USED,
                productName = topUsed.key.second,
                amount = topUsed.value.first,
                showShoppingCta = false
            )
        }

        val ranOutNames = events
            .filter {
                (it.changeType == StockChangeEventEntity.TYPE_CONSUME ||
                        it.changeType == StockChangeEventEntity.TYPE_ADJUST_DOWN) &&
                        it.quantityAfter <= 0.0 &&
                        it.quantityBefore > 0.0
            }
            .map { it.productName }
            .distinct()

        ranOutNames.take(1).forEach { name ->
            cards += InsightCard(
                kind = InsightKind.RAN_OUT,
                productName = name,
                showShoppingCta = true
            )
        }

        val recommended = inventory
            .filter { item ->
                val min = item.minQuantity
                item.quantity <= 0.0 || (min != null && item.quantity <= min)
            }
            .sortedBy { it.quantity }
            .map { it.name }
            .filterNot { name -> ranOutNames.contains(name) }
            .distinct()

        recommended.take(1).forEach { name ->
            cards += InsightCard(
                kind = InsightKind.BUY_MORE,
                productName = name,
                showShoppingCta = true
            )
        }

        if (cards.isEmpty()) {
            cards += InsightCard(kind = InsightKind.EMPTY_TRACKING)
        }

        val limited = cards.take(MAX_CARDS)
        return MonthlyInsights(
            monthLabelKey = Calendar.getInstance().get(Calendar.MONTH),
            cards = limited,
            showShoppingCta = limited.any { it.showShoppingCta }
        )
    }
}
