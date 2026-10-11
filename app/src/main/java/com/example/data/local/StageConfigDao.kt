package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StageConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface StageConfigDao {
    @Query("SELECT * FROM stage_configs")
    fun getAllConfigs(): Flow<List<StageConfig>>

    @Query("SELECT * FROM stage_configs")
    suspend fun getAllConfigsDirect(): List<StageConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<StageConfig>)

    @Update
    suspend fun update(config: StageConfig)
}
