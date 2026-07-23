package com.gabow95k.keeply.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["expirationDate"]),
        Index(value = ["name"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
