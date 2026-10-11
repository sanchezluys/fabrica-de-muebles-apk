package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Priority
import com.example.data.model.ProductionOrder
import com.example.data.model.ProductionStage
import com.example.data.model.StageConfig
import com.example.data.repository.ProductionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProductionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProductionRepository(db.orderDao(), db.progressDao(), db.stageConfigDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gestión Fábrica Muebles", appName)
    }

    @Test
    fun `production stage ordering is correct`() {
        assertEquals(ProductionStage.CORTE, ProductionStage.RECEPCION.next())
        assertEquals(ProductionStage.ENSAMBLE, ProductionStage.CORTE.next())
        assertEquals(ProductionStage.ACABADO, ProductionStage.ENSAMBLE.next())
        assertEquals(ProductionStage.CALIDAD, ProductionStage.ACABADO.next())
        assertEquals(ProductionStage.LISTO, ProductionStage.CALIDAD.next())
        assertEquals(null, ProductionStage.LISTO.next())
    }

    @Test
    fun `create order and advance stage with delay check`() = runBlocking {
        repository.ensureConfigsInitialized()

        val order = ProductionOrder(
            orderCode = "ORD-TEST-1",
            clientName = "Cliente Prueba",
            furnitureType = "Mesa Rústica",
            woodType = "Roble",
            finishType = "Mate",
            dimensions = "180x90x75 cm",
            currentStage = ProductionStage.RECEPCION,
            currentStageStartedAt = System.currentTimeMillis() - (30L * 3600 * 1000) // 30h ago (default is 24h)
        )
        val id = repository.createOrder(order)
        assertTrue(id > 0)

        // Advance stage to CORTE
        val success = repository.advanceOrderStage(
            orderId = id,
            newStage = ProductionStage.CORTE,
            operatorName = "Maestro Carpintero",
            notes = "Corte aprobado",
            photoUri = null
        )
        assertTrue(success)

        val updated = db.orderDao().getOrderByIdDirect(id)
        assertNotNull(updated)
        assertEquals(ProductionStage.CORTE, updated?.currentStage)
        assertFalse(updated?.isCompleted ?: true)

        val progressList = db.progressDao().getProgressForOrderDirect(id)
        assertTrue(progressList.size >= 2)
        val lastAdvance = progressList.last()
        assertEquals(ProductionStage.CORTE, lastAdvance.toStage)
        assertTrue("Debe registrar retraso al superar 24h", lastAdvance.wasDelayed)
    }
}
