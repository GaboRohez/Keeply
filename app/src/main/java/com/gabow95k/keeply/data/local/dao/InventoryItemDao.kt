package com.gabow95k.keeply.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE categoryId = :categoryId ORDER BY name ASC")
    fun observeByCategory(categoryId: Long): Flow<List<InventoryItemEntity>>

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE name LIKE '%' || :query || '%'
           OR IFNULL(brand, '') LIKE '%' || :query || '%'
           OR IFNULL(barcode, '') LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun observeSearch(query: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InventoryItemEntity?

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE expirationDate IS NOT NULL
          AND expirationDate <= :beforeTimestamp
        ORDER BY expirationDate ASC
        """
    )
    fun observeExpiringBefore(beforeTimestamp: Long): Flow<List<InventoryItemEntity>>

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE minQuantity IS NOT NULL
          AND quantity <= minQuantity
        ORDER BY name ASC
        """
    )
    fun observeLowStock(): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Delete
    suspend fun delete(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
