package com.gabow95k.keeply.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabow95k.keeply.data.local.dao.CategoryDao
import com.gabow95k.keeply.data.local.dao.InventoryItemDao
import com.gabow95k.keeply.data.local.dao.ShoppingListDao
import com.gabow95k.keeply.data.local.dao.ShoppingListItemDao
import com.gabow95k.keeply.data.local.dao.StockChangeEventDao
import com.gabow95k.keeply.data.local.dao.UserProfileDao
import com.gabow95k.keeply.data.local.entity.CategoryEntity
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.ShoppingListEntity
import com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        InventoryItemEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        StockChangeEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KeeplyDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun stockChangeEventDao(): StockChangeEventDao

    companion object {
        private const val DATABASE_NAME = "keeply.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        note TEXT,
                        inventoryItemId INTEGER,
                        isChecked INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_shopping_list_items_listId ON shopping_list_items(listId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_shopping_list_items_inventoryItemId ON shopping_list_items(inventoryItemId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stock_change_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        inventoryItemId INTEGER,
                        productName TEXT NOT NULL,
                        changeType TEXT NOT NULL,
                        delta REAL NOT NULL,
                        quantityBefore REAL NOT NULL,
                        quantityAfter REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_stock_change_events_createdAt ON stock_change_events(createdAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_stock_change_events_inventoryItemId ON stock_change_events(inventoryItemId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_stock_change_events_changeType ON stock_change_events(changeType)"
                )
            }
        }

        @Volatile
        private var instance: KeeplyDatabase? = null

        fun getInstance(context: Context): KeeplyDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): KeeplyDatabase {
            lateinit var database: KeeplyDatabase
            database = Room.databaseBuilder(context, KeeplyDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            database.userProfileDao().upsert(UserProfileEntity())
                            database.categoryDao().insertAll(CategoryDefaults.seedCategories())
                        }
                    }
                })
                .build()
            return database
        }
    }
}
