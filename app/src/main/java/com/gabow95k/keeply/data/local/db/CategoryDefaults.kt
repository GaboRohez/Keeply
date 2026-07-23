package com.gabow95k.keeply.data.local.db

import com.gabow95k.keeply.data.local.entity.CategoryEntity

object CategoryDefaults {

    const val ICON_MEDICINE = "ic_cat_medicine"
    const val ICON_PERSONAL_CARE = "ic_cat_personal_care"
    const val ICON_CLEANING = "ic_cat_cleaning"
    const val ICON_KITCHEN = "ic_cat_kitchen"
    const val ICON_TOOLS = "ic_cat_tools"
    const val ICON_PETS = "ic_cat_pets"
    const val ICON_OTHER = "ic_cat_other"

    fun seedCategories(now: Long = System.currentTimeMillis()): List<CategoryEntity> = listOf(
        CategoryEntity(
            name = "Medicinas / Farmacia",
            iconKey = ICON_MEDICINE,
            colorHex = "#4F6CF1",
            sortOrder = 0,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Cuidado personal",
            iconKey = ICON_PERSONAL_CARE,
            colorHex = "#7B90F6",
            sortOrder = 1,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Limpieza",
            iconKey = ICON_CLEANING,
            colorHex = "#4CAF82",
            sortOrder = 2,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Cocina / Despensa",
            iconKey = ICON_KITCHEN,
            colorHex = "#F5A524",
            sortOrder = 3,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Herramientas",
            iconKey = ICON_TOOLS,
            colorHex = "#8E8E93",
            sortOrder = 4,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Mascotas",
            iconKey = ICON_PETS,
            colorHex = "#E57373",
            sortOrder = 5,
            isDefault = true,
            createdAt = now
        ),
        CategoryEntity(
            name = "Otros",
            iconKey = ICON_OTHER,
            colorHex = "#5C5C63",
            sortOrder = 6,
            isDefault = true,
            createdAt = now
        )
    )
}
