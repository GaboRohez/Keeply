package com.gabow95k.keeply.presentation.shopping

data class ShoppingListUi(
    val id: Long,
    val name: String,
    val meta: String
)

data class ShoppingItemUi(
    val id: Long,
    val name: String,
    val note: String?,
    val isChecked: Boolean
)
