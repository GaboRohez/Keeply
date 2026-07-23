package com.gabow95k.keeply.presentation.botiquin

data class InventoryItemUi(
    val id: Long,
    val name: String,
    val categoryName: String,
    val stockLabel: String,
    val barcode: String?,
    val metaLabel: String,
    val photoPath: String?
)
