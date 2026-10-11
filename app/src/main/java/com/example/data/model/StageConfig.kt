package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_configs")
data class StageConfig(
    @PrimaryKey val stage: ProductionStage,
    val estimatedHours: Int,
    val stageLabel: String
)
