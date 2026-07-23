package com.gabow95k.keeply.scanner

data class ProductLabelHints(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val formType: String? = null,
    val unit: String? = null,
    val quantity: String? = null,
    val expirationMillis: Long? = null
)
