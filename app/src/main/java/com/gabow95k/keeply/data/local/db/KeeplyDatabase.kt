package com.gabow95k.keeply.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabow95k.keeply.data.local.dao.CategoryDao
import com.gabow95k.keeply.data.local.dao.InventoryItemDao
import com.gabow95k.keeply.data.local.dao.UserProfileDao
import com.gabow95k.keeply.data.local.entity.CategoryEntity
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        InventoryItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KeeplyDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun inventoryItemDao(): InventoryItemDao

    companion object {
        private const val DATABASE_NAME = "keeply.db"

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
