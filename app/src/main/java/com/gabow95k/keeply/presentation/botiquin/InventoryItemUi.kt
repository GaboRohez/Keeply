package com.gabow95k.keeply.presentation.botiquin

data class InventoryItemUi(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val quantity: Double,
    val stockLabel: String,
    val barcode: String?,
    val metaLabel: String,
    val photoPath: String?
)
