package com.gabow95k.keeply.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockChangeEventDao {

    @Insert
    suspend fun insert(event: StockChangeEventEntity): Long

    @Query(
        """
        SELECT * FROM stock_change_events
        WHERE createdAt >= :since
        ORDER BY createdAt DESC
        """
    )
    fun observeSince(since: Long): Flow<List<StockChangeEventEntity>>

    @Query(
        """
        SELECT * FROM stock_change_events
        WHERE createdAt >= :since
        ORDER BY createdAt DESC
        """
    )
    suspend fun getSince(since: Long): List<StockChangeEventEntity>
}
