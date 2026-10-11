package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "production_orders")
data class ProductionOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderCode: String,
    val clientName: String,
    val clientPhone: String = "",
    val furnitureType: String,
    val woodType: String,
    val finishType: String,
    val dimensions: String,
    val notes: String = "",
    val currentStage: ProductionStage = ProductionStage.RECEPCION,
    val currentStageStartedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val targetDeliveryDate: Long = System.currentTimeMillis() + (7L * 24 * 3600 * 1000),
    val priority: Priority = Priority.MEDIA,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
