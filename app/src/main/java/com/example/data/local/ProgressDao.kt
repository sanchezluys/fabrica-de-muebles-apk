package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StageProgressRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM stage_progress_records WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getProgressForOrder(orderId: Long): Flow<List<StageProgressRecord>>

    @Query("SELECT * FROM stage_progress_records ORDER BY timestamp DESC")
    fun getAllProgress(): Flow<List<StageProgressRecord>>

    @Query("SELECT * FROM stage_progress_records WHERE orderId = :orderId ORDER BY timestamp ASC")
    suspend fun getProgressForOrderDirect(orderId: Long): List<StageProgressRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StageProgressRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StageProgressRecord>)

    @Delete
    suspend fun delete(record: StageProgressRecord)

    @Query("DELETE FROM stage_progress_records WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: Long)

    @Query("DELETE FROM stage_progress_records")
    suspend fun deleteAll()
}
