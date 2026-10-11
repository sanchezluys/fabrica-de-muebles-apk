package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Priority
import com.example.data.model.ProductionStage

class Converters {
    @TypeConverter
    fun fromProductionStage(stage: ProductionStage?): String? = stage?.name

    @TypeConverter
    fun toProductionStage(value: String?): ProductionStage? {
        return value?.let {
            try {
                ProductionStage.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ProductionStage.RECEPCION
            }
        }
    }

    @TypeConverter
    fun fromPriority(priority: Priority?): String? = priority?.name

    @TypeConverter
    fun toPriority(value: String?): Priority? {
        return value?.let {
            try {
                Priority.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Priority.MEDIA
            }
        }
    }
}
