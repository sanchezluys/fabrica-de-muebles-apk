package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ProductionOrder
import com.example.data.model.ProductionStage
import com.example.data.model.StageConfig
import com.example.data.model.StageProgressRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductionOrder::class,
        StageProgressRecord::class,
        StageConfig::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun progressDao(): ProgressDao
    abstract fun stageConfigDao(): StageConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "furniture_factory_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialStageConfigs(database.stageConfigDao())
                    }
                }
            }

            private suspend fun populateInitialStageConfigs(dao: StageConfigDao) {
                val defaults = ProductionStage.entries.map { stage ->
                    StageConfig(
                        stage = stage,
                        estimatedHours = stage.defaultHours,
                        stageLabel = stage.title
                    )
                }
                dao.insertAll(defaults)
            }
        }
    }
}
