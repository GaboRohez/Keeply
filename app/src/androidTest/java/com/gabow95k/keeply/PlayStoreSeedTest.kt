package com.gabow95k.keeply

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabow95k.keeply.data.local.db.CategoryDefaults
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.ShoppingListEntity
import com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Creates deterministic, realistic local data used only for Play Store screenshots. */
@RunWith(AndroidJUnit4::class)
class PlayStoreSeedTest {

    @Test
    fun seedRepresentativeLocalData() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val db = KeeplyDatabase.getInstance(context)
            db.clearAllTables()

        val categories = CategoryDefaults.seedCategories()
        val categoryIds = categories.associate { category ->
            category.name to db.categoryDao().insert(category)
        }
        val now = System.currentTimeMillis()
        val day = TimeUnit.DAYS.toMillis(1)

        db.userProfileDao().upsert(
            UserProfileEntity(
                name = "Gabi",
                age = 30,
                bloodType = "O+",
                email = "gabi@example.com",
                updatedAt = now
            )
        )

        val coffeeId = db.inventoryItemDao().insert(
            product(
                categoryIds.getValue("Cocina / Despensa"),
                "Café orgánico",
                "Sierra Azul",
                2.0,
                3.0,
                now + 2 * day,
                "7501234567890",
                "Alacena",
                now - 5 * day
            )
        )
        val detergentId = db.inventoryItemDao().insert(
            product(
                categoryIds.getValue("Limpieza"),
                "Detergente líquido",
                "Casa Clara",
                0.0,
                1.0,
                null,
                "7509876543210",
                "Lavandería",
                now - 4 * day
            )
        )
        val riceId = db.inventoryItemDao().insert(
            product(
                categoryIds.getValue("Cocina / Despensa"),
                "Arroz integral",
                null,
                4.0,
                2.0,
                now + 90 * day,
                null,
                "Alacena",
                now - 3 * day
            )
        )
        val medicineId = db.inventoryItemDao().insert(
            product(
                categoryIds.getValue("Medicinas / Farmacia"),
                "Paracetamol 500 mg",
                "Salud",
                12.0,
                5.0,
                now + 30 * day,
                "7501122334455",
                "Botiquín",
                now - 2 * day
            )
        )
        val petFoodId = db.inventoryItemDao().insert(
            product(
                categoryIds.getValue("Mascotas"),
                "Alimento para gato",
                "Michi",
                1.0,
                2.0,
                now + 120 * day,
                null,
                "Alacena",
                now - day
            )
        )

        db.stockChangeEventDao().insert(
            stock(coffeeId, "Café orgánico", StockChangeEventEntity.TYPE_CONSUME, 2.0, 4.0, 2.0, now)
        )
        db.stockChangeEventDao().insert(
            stock(
                detergentId,
                "Detergente líquido",
                StockChangeEventEntity.TYPE_CONSUME,
                1.0,
                1.0,
                0.0,
                now - day
            )
        )
        db.stockChangeEventDao().insert(
            stock(riceId, "Arroz integral", StockChangeEventEntity.TYPE_ADD, 4.0, 0.0, 4.0, now - 2 * day)
        )

        val listId = db.shoppingListDao().insert(
            ShoppingListEntity(name = "Compra semanal", sourceType = ShoppingListEntity.SOURCE_AUTO)
        )
        db.shoppingListItemDao().insertAll(
            listOf(
                ShoppingListItemEntity(
                    listId = listId,
                    name = "Detergente líquido",
                    note = "Agotado",
                    inventoryItemId = detergentId,
                    sortOrder = 0
                ),
                ShoppingListItemEntity(
                    listId = listId,
                    name = "Café orgánico",
                    note = "Stock bajo",
                    inventoryItemId = coffeeId,
                    sortOrder = 1
                ),
                ShoppingListItemEntity(
                    listId = listId,
                    name = "Alimento para gato",
                    note = "Stock bajo",
                    inventoryItemId = petFoodId,
                    isChecked = true,
                    sortOrder = 2
                ),
                ShoppingListItemEntity(
                    listId = listId,
                    name = "Paracetamol 500 mg",
                    inventoryItemId = medicineId,
                    isChecked = true,
                    sortOrder = 3
                )
            )
        )

            KeeplyPreferences.getInstance(context).apply {
                acceptCurrentPrivacy()
                notificationsEnabled = false
                val calendar = Calendar.getInstance()
                val monthKey = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH) + 1
                val dayKey = monthKey * 100 + calendar.get(Calendar.DAY_OF_MONTH)
                lastMonthEndPromptMonth = monthKey
                lastLowStockPromptDay = dayKey
                lastTipPromptDay = dayKey
            }
        }
    }

    private fun product(
        categoryId: Long,
        name: String,
        brand: String?,
        quantity: Double,
        minQuantity: Double,
        expiration: Long?,
        barcode: String?,
        location: String,
        updatedAt: Long
    ) = InventoryItemEntity(
        categoryId = categoryId,
        name = name,
        brand = brand,
        formType = "Paquete",
        unit = "pzas",
        quantity = quantity,
        minQuantity = minQuantity,
        expirationDate = expiration,
        barcode = barcode,
        location = location,
        updatedAt = updatedAt
    )

    private fun stock(
        itemId: Long,
        name: String,
        type: String,
        delta: Double,
        before: Double,
        after: Double,
        createdAt: Long
    ) = StockChangeEventEntity(
        inventoryItemId = itemId,
        productName = name,
        changeType = type,
        delta = delta,
        quantityBefore = before,
        quantityAfter = after,
        createdAt = createdAt
    )
}
