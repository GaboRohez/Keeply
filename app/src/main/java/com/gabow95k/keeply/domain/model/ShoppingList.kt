package com.gabow95k.keeply.domain.model

data class ShoppingList(
    val id: Long = 0,
    val name: String,
    val sourceType: String = "manual",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ShoppingListItem(
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val note: String? = null,
    val inventoryItemId: Long? = null,
    val isChecked: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
