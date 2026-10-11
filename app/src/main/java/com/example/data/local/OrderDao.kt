package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProductionOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM production_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ProductionOrder>>

    @Query("SELECT * FROM production_orders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveOrders(): Flow<List<ProductionOrder>>

    @Query("SELECT * FROM production_orders WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedOrders(): Flow<List<ProductionOrder>>

    @Query("SELECT * FROM production_orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: Long): Flow<ProductionOrder?>

    @Query("SELECT * FROM production_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdDirect(id: Long): ProductionOrder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: ProductionOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<ProductionOrder>): List<Long>

    @Update
    suspend fun update(order: ProductionOrder)

    @Delete
    suspend fun delete(order: ProductionOrder)

    @Query("DELETE FROM production_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM production_orders")
    suspend fun deleteAll()
}
