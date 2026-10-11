package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stage_progress_records",
    foreignKeys = [
        ForeignKey(
            entity = ProductionOrder::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class StageProgressRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val fromStage: ProductionStage,
    val toStage: ProductionStage,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val hoursInPreviousStage: Double = 0.0,
    val wasDelayed: Boolean = false
)
