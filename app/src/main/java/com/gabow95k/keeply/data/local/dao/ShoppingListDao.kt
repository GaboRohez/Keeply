package com.gabow95k.keeply.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabow95k.keeply.data.local.entity.ShoppingListEntity
import com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_lists ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ShoppingListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ShoppingListEntity): Long

    @Update
    suspend fun update(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT COUNT(*) FROM shopping_list_items
        WHERE listId = :listId AND isChecked = 0
        """
    )
    fun observePendingCount(listId: Long): Flow<Int>

    @Query(
        """
        SELECT listId AS listId, COUNT(*) AS totalCount,
               SUM(CASE WHEN isChecked = 0 THEN 1 ELSE 0 END) AS pendingCount
        FROM shopping_list_items
        GROUP BY listId
        """
    )
    fun observeItemCounts(): Flow<List<ShoppingListCountRow>>
}

data class ShoppingListCountRow(
    val listId: Long,
    val totalCount: Int,
    val pendingCount: Int
)

@Dao
interface ShoppingListItemDao {

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY isChecked ASC, sortOrder ASC, id ASC")
    fun observeByList(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByList(listId: Long): List<ShoppingListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingListItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItemEntity>)

    @Update
    suspend fun update(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun deleteByListId(listId: Long)
}
