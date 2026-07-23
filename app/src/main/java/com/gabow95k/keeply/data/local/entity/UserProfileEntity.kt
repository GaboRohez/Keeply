package com.gabow95k.keeply.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = SINGLE_PROFILE_ID,
    val name: String = "",
    val age: Int? = null,
    val bloodType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1L
    }
}
