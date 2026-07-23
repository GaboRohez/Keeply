package com.gabow95k.keeply.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
