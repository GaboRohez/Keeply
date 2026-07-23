package com.gabow95k.keeply.domain.model

data class UserProfile(
    val id: Long = 1L,
    val name: String = "",
    val age: Int? = null,
    val bloodType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
