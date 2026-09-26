package com.gabow95k.keeply.shopping

import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListGeneratorTest {

    private val now = 2_000_000L

    @Test
    fun generate_filtersCriteriaCategoriesDeduplicatesAndSorts() {
        val items = listOf(
            item(1, "Arroz", category = 1, quantity = 0.0, updatedAt = 1),
            item(2, " arroz ", category = 1, quantity = 0.0, updatedAt = 2),
            item(3, "Jabón", category = 1, quantity = 1.0, minimum = 2.0),
            item(4, "Leche", category = 1, quantity = 5.0, expiration = now - 1),
            item(5, "Oculto", category = 2, quantity = 0.0)
        )

        val result = ShoppingListGenerator.generate(
            items,
            ShoppingListGenerator.Criteria(
                categoryIds = setOf(1),
                includeOutOfStock = true,
                includeLowStock = true,
                includeExpired = true,
                nowMillis = now
            )
        )

        assertEquals(listOf("arroz", "Jabón", "Leche"), result.map { it.name })
        assertEquals(2L, result.first().inventoryItemId)
        assertEquals("Agotado", result.first().note)
        assertEquals("Stock bajo", result[1].note)
        assertEquals("Caducado", result[2].note)
    }

    @Test
    fun generate_returnsEmptyWithoutCategoriesOrMatches() {
        val product = item(1, "Arroz", category = 1, quantity = 10.0)
        assertTrue(
            ShoppingListGenerator.generate(
                listOf(product),
                ShoppingListGenerator.Criteria(emptySet(), true, true, true, now)
            ).isEmpty()
        )
        assertTrue(
            ShoppingListGenerator.generate(
                listOf(product),
                ShoppingListGenerator.Criteria(setOf(1), true, true, true, now)
            ).isEmpty()
        )
    }

    private fun item(
        id: Long,
        name: String,
        category: Long,
        quantity: Double,
        minimum: Double? = null,
        expiration: Long? = null,
        updatedAt: Long = 0
    ) = InventoryItemEntity(
        id = id,
        categoryId = category,
        name = name,
        quantity = quantity,
        minQuantity = minimum,
        expirationDate = expiration,
        updatedAt = updatedAt
    )
}
