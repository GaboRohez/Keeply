package com.gabow95k.keeply.domain.model

data class InventoryItem(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val brand: String? = null,
    val formType: String? = null,
    val unit: String? = null,
    val quantity: Double = 0.0,
    val minQuantity: Double? = null,
    val expirationDate: Long? = null,
    val barcode: String? = null,
    val photoPath: String? = null,
    val notes: String? = null,
    val location: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
