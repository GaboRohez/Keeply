package com.gabow95k.keeply.shopping

import com.gabow95k.keeply.data.local.entity.InventoryItemEntity

object ShoppingListGenerator {

    data class Criteria(
        val categoryIds: Set<Long>,
        val includeOutOfStock: Boolean,
        val includeLowStock: Boolean,
        val includeExpired: Boolean,
        val nowMillis: Long = System.currentTimeMillis()
    )

    data class GeneratedItem(
        val name: String,
        val note: String?,
        val inventoryItemId: Long?
    )

    fun generate(
        items: List<InventoryItemEntity>,
        criteria: Criteria
    ): List<GeneratedItem> {
        if (criteria.categoryIds.isEmpty()) return emptyList()

        val matched = items.filter { item ->
            item.categoryId in criteria.categoryIds && matches(item, criteria)
        }

        return matched
            .groupBy { it.name.trim().lowercase() }
            .mapNotNull { (_, group) ->
                val primary = group.maxByOrNull { it.updatedAt } ?: return@mapNotNull null
                GeneratedItem(
                    name = primary.name.trim(),
                    note = buildNote(primary, criteria),
                    inventoryItemId = primary.id
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun matches(item: InventoryItemEntity, criteria: Criteria): Boolean {
        val outOfStock = criteria.includeOutOfStock && item.quantity <= 0.0
        val lowStock = criteria.includeLowStock &&
                item.quantity > 0.0 &&
                item.minQuantity != null &&
                item.quantity <= item.minQuantity
        val expired = criteria.includeExpired &&
                item.expirationDate != null &&
                item.expirationDate < criteria.nowMillis
        return outOfStock || lowStock || expired
    }

    private fun buildNote(item: InventoryItemEntity, criteria: Criteria): String? {
        val reasons = mutableListOf<String>()
        if (criteria.includeOutOfStock && item.quantity <= 0.0) reasons += "Agotado"
        if (criteria.includeLowStock &&
            item.quantity > 0.0 &&
            item.minQuantity != null &&
            item.quantity <= item.minQuantity
        ) {
            reasons += "Stock bajo"
        }
        if (criteria.includeExpired &&
            item.expirationDate != null &&
            item.expirationDate < criteria.nowMillis
        ) {
            reasons += "Caducado"
        }
        return reasons.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
