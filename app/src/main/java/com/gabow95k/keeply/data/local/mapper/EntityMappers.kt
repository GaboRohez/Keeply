package com.gabow95k.keeply.data.local.mapper

import com.gabow95k.keeply.data.local.entity.CategoryEntity
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import com.gabow95k.keeply.domain.model.Category
import com.gabow95k.keeply.domain.model.InventoryItem
import com.gabow95k.keeply.domain.model.UserProfile

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    age = age,
    bloodType = bloodType,
    phone = phone,
    email = email,
    notes = notes,
    updatedAt = updatedAt
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    age = age,
    bloodType = bloodType,
    phone = phone,
    email = email,
    notes = notes,
    updatedAt = updatedAt
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isDefault = isDefault,
    createdAt = createdAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isDefault = isDefault,
    createdAt = createdAt
)

fun InventoryItemEntity.toDomain(): InventoryItem = InventoryItem(
    id = id,
    categoryId = categoryId,
    name = name,
    brand = brand,
    formType = formType,
    unit = unit,
    quantity = quantity,
    minQuantity = minQuantity,
    expirationDate = expirationDate,
    barcode = barcode,
    photoPath = photoPath,
    notes = notes,
    location = location,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun InventoryItem.toEntity(): InventoryItemEntity = InventoryItemEntity(
    id = id,
    categoryId = categoryId,
    name = name,
    brand = brand,
    formType = formType,
    unit = unit,
    quantity = quantity,
    minQuantity = minQuantity,
    expirationDate = expirationDate,
    barcode = barcode,
    photoPath = photoPath,
    notes = notes,
    location = location,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun com.gabow95k.keeply.data.local.entity.ShoppingListEntity.toDomain() =
    com.gabow95k.keeply.domain.model.ShoppingList(
        id = id,
        name = name,
        sourceType = sourceType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun com.gabow95k.keeply.domain.model.ShoppingList.toEntity() =
    com.gabow95k.keeply.data.local.entity.ShoppingListEntity(
        id = id,
        name = name,
        sourceType = sourceType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity.toDomain() =
    com.gabow95k.keeply.domain.model.ShoppingListItem(
        id = id,
        listId = listId,
        name = name,
        note = note,
        inventoryItemId = inventoryItemId,
        isChecked = isChecked,
        sortOrder = sortOrder,
        createdAt = createdAt
    )

fun com.gabow95k.keeply.domain.model.ShoppingListItem.toEntity() =
    com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity(
        id = id,
        listId = listId,
        name = name,
        note = note,
        inventoryItemId = inventoryItemId,
        isChecked = isChecked,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
