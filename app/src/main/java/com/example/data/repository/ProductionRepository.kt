package com.example.data.repository

import com.example.data.local.OrderDao
import com.example.data.local.ProgressDao
import com.example.data.local.StageConfigDao
import com.example.data.model.Priority
import com.example.data.model.ProductionOrder
import com.example.data.model.ProductionStage
import com.example.data.model.StageConfig
import com.example.data.model.StageProgressRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.roundToInt

class ProductionRepository(
    private val orderDao: OrderDao,
    private val progressDao: ProgressDao,
    private val stageConfigDao: StageConfigDao
) {
    val allOrders: Flow<List<ProductionOrder>> = orderDao.getAllOrders()
    val activeOrders: Flow<List<ProductionOrder>> = orderDao.getActiveOrders()
    val completedOrders: Flow<List<ProductionOrder>> = orderDao.getCompletedOrders()
    val stageConfigs: Flow<List<StageConfig>> = stageConfigDao.getAllConfigs()
    val allProgress: Flow<List<StageProgressRecord>> = progressDao.getAllProgress()

    fun getOrderById(id: Long): Flow<ProductionOrder?> = orderDao.getOrderById(id)

    fun getProgressForOrder(orderId: Long): Flow<List<StageProgressRecord>> =
        progressDao.getProgressForOrder(orderId)

    suspend fun createOrder(order: ProductionOrder): Long {
        val id = orderDao.insert(order)
        // Record initial reception entry
        progressDao.insert(
            StageProgressRecord(
                orderId = id,
                fromStage = order.currentStage,
                toStage = order.currentStage,
                timestamp = order.createdAt,
                operatorName = "Oficina Técnica",
                notes = "Orden creada e ingresada en etapa de ${order.currentStage.title}.",
                hoursInPreviousStage = 0.0,
                wasDelayed = false
            )
        )
        return id
    }

    suspend fun updateOrder(order: ProductionOrder) {
        orderDao.update(order)
    }

    suspend fun deleteOrderById(id: Long) {
        progressDao.deleteByOrderId(id)
        orderDao.deleteById(id)
    }

    suspend fun updateStageConfig(config: StageConfig) {
        stageConfigDao.update(config)
    }

    suspend fun advanceOrderStage(
        orderId: Long,
        newStage: ProductionStage,
        operatorName: String,
        notes: String,
        photoUri: String? = null
    ): Boolean {
        val order = orderDao.getOrderByIdDirect(orderId) ?: return false
        val now = System.currentTimeMillis()
        val previousStage = order.currentStage

        // Calculate hours spent in previous stage
        val durationMillis = (now - order.currentStageStartedAt).coerceAtLeast(0L)
        val hoursSpent = durationMillis.toDouble() / (1000.0 * 60.0 * 60.0)

        // Retrieve config threshold
        val configs = stageConfigDao.getAllConfigsDirect()
        val stageConfig = configs.find { it.stage == previousStage }
        val thresholdHours = stageConfig?.estimatedHours ?: previousStage.defaultHours
        val wasDelayed = hoursSpent > thresholdHours

        // Create advance record
        val record = StageProgressRecord(
            orderId = orderId,
            fromStage = previousStage,
            toStage = newStage,
            timestamp = now,
            operatorName = operatorName.ifBlank { "Tallerista" },
            notes = notes.ifBlank { "Avance registrado hacia ${newStage.title}" },
            photoUri = photoUri,
            hoursInPreviousStage = ((hoursSpent * 10.0).roundToInt()) / 10.0,
            wasDelayed = wasDelayed
        )
        progressDao.insert(record)

        val isFinal = newStage == ProductionStage.LISTO
        val updatedOrder = order.copy(
            currentStage = newStage,
            currentStageStartedAt = now,
            isCompleted = isFinal,
            completedAt = if (isFinal) now else null
        )
        orderDao.update(updatedOrder)
        return true
    }

    suspend fun ensureConfigsInitialized() {
        val current = stageConfigDao.getAllConfigsDirect()
        if (current.isEmpty()) {
            val defaults = ProductionStage.entries.map {
                StageConfig(
                    stage = it,
                    estimatedHours = it.defaultHours,
                    stageLabel = it.title
                )
            }
            stageConfigDao.insertAll(defaults)
        }
    }

    suspend fun seedSampleData() {
        // Clear previous sample data to avoid duplicates if re-seeded
        orderDao.deleteAll()
        progressDao.deleteAll()

        ensureConfigsInitialized()

        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L
        val day = 24 * hour

        // 1. Order in Ensamble - DELAYED (exceeded 48h limit -> 58h in Ensamble)
        val order1 = ProductionOrder(
            orderCode = "ORD-2026-001",
            clientName = "Restaurante Don Quijote",
            clientPhone = "+57 310 456 7890",
            furnitureType = "Mesa de Comedor Rústica (10 puestos)",
            woodType = "Roble Macizo Tratado",
            finishType = "Barniz Poliuretano Mate",
            dimensions = "280 x 110 x 78 cm",
            notes = "Madera curada de poro abierto. Incluye patas cruzadas reforzadas con ensamble cola de milano.",
            currentStage = ProductionStage.ENSAMBLE,
            currentStageStartedAt = now - (58 * hour), // 58 hours spent (threshold is 48h -> DELAY ALERT!)
            createdAt = now - (6 * day),
            targetDeliveryDate = now + (3 * day),
            priority = Priority.ALTA,
            isCompleted = false
        )
        val id1 = orderDao.insert(order1)

        // Progress for order 1
        progressDao.insert(
            StageProgressRecord(
                orderId = id1,
                fromStage = ProductionStage.RECEPCION,
                toStage = ProductionStage.RECEPCION,
                timestamp = now - (6 * day),
                operatorName = "Ing. Manuel Soto",
                notes = "Diseño estructural y plano de despiece aprobados por el cliente.",
                hoursInPreviousStage = 0.0,
                wasDelayed = false
            )
        )
        progressDao.insert(
            StageProgressRecord(
                orderId = id1,
                fromStage = ProductionStage.RECEPCION,
                toStage = ProductionStage.CORTE,
                timestamp = now - (4 * day),
                operatorName = "Carlos Rincón",
                notes = "Dimensionado de tablones principales y cortes a 45 grados ejecutados.",
                hoursInPreviousStage = 22.0,
                wasDelayed = false
            )
        )
        progressDao.insert(
            StageProgressRecord(
                orderId = id1,
                fromStage = ProductionStage.CORTE,
                toStage = ProductionStage.ENSAMBLE,
                timestamp = now - (58 * hour),
                operatorName = "David Morales",
                notes = "Espigado y verificación de escuadras de las patas estructurales.",
                hoursInPreviousStage = 34.0,
                wasDelayed = false
            )
        )

        // 2. Order in Acabado/Pintura - DELAYED (exceeded 48h -> 52h)
        val order2 = ProductionOrder(
            orderCode = "ORD-2026-002",
            clientName = "Arq. Valeria Gómez",
            clientPhone = "+57 315 889 1234",
            furnitureType = "Biblioteca Minimalista con Nichos",
            woodType = "Cedro Rosado y Nogal",
            finishType = "Laca Catalizada Satinada",
            dimensions = "240 x 300 x 35 cm",
            notes = "Módulo desmontable en 3 secciones. Trasera enchapada en nogal americano.",
            currentStage = ProductionStage.ACABADO,
            currentStageStartedAt = now - (52 * hour), // 52h spent (threshold is 48h -> DELAY ALERT!)
            createdAt = now - (7 * day),
            targetDeliveryDate = now + (2 * day),
            priority = Priority.ALTA,
            isCompleted = false
        )
        val id2 = orderDao.insert(order2)
        progressDao.insert(
            StageProgressRecord(
                orderId = id2,
                fromStage = ProductionStage.RECEPCION,
                toStage = ProductionStage.CORTE,
                timestamp = now - (5 * day),
                operatorName = "Carlos Rincón",
                notes = "Corte de parantes verticales y entrepaños con ranurado para tiras LED.",
                hoursInPreviousStage = 18.0,
                wasDelayed = false
            )
        )
        progressDao.insert(
            StageProgressRecord(
                orderId = id2,
                fromStage = ProductionStage.CORTE,
                toStage = ProductionStage.ENSAMBLE,
                timestamp = now - (3 * day),
                operatorName = "David Morales",
                notes = "Armado preliminar de estructura y comprobación de plomos.",
                hoursInPreviousStage = 31.0,
                wasDelayed = false
            )
        )
        progressDao.insert(
            StageProgressRecord(
                orderId = id2,
                fromStage = ProductionStage.ENSAMBLE,
                toStage = ProductionStage.ACABADO,
                timestamp = now - (52 * hour),
                operatorName = "Jorge Méndez",
                notes = "Fondeado inicial y primer lijado al agua. Pendiente secado de segunda mano de tinte.",
                hoursInPreviousStage = 42.0,
                wasDelayed = false
            )
        )

        // 3. Order in Corte - ON TIME
        val order3 = ProductionOrder(
            orderCode = "ORD-2026-003",
            clientName = "Familia Mendoza",
            clientPhone = "+57 320 112 3344",
            furnitureType = "Juego de Dormitorio Queen Size",
            woodType = "Pino Canadiense Seleccionado",
            finishType = "Tinte Miel y Poliuretano",
            dimensions = "Cabecero 165 x 130 cm, Largueros 205 cm",
            notes = "Incluye 2 nocheros flotantes con correderas de extensión total cierre suave.",
            currentStage = ProductionStage.CORTE,
            currentStageStartedAt = now - (14 * hour), // 14 hours (threshold is 36h -> ON TIME)
            createdAt = now - (2 * day),
            targetDeliveryDate = now + (6 * day),
            priority = Priority.MEDIA,
            isCompleted = false
        )
        val id3 = orderDao.insert(order3)
        progressDao.insert(
            StageProgressRecord(
                orderId = id3,
                fromStage = ProductionStage.RECEPCION,
                toStage = ProductionStage.CORTE,
                timestamp = now - (14 * hour),
                operatorName = "Carlos Rincón",
                notes = "Listones cepillados a 4 caras, iniciando corte de molduras de cabecero.",
                hoursInPreviousStage = 20.0,
                wasDelayed = false
            )
        )

        // 4. Order in Calidad - ON TIME
        val order4 = ProductionOrder(
            orderCode = "ORD-2026-004",
            clientName = "Oficinas Nexus Corp",
            clientPhone = "+57 300 777 9900",
            furnitureType = "Escritorio Gerencial en L con Faldón",
            woodType = "MDF Melamínico Roble Santana + Madera Maciza",
            finishType = "Bordes PVC 2mm termofusionado",
            dimensions = "180 x 160 x 75 cm",
            notes = "Caja pasacables en aluminio y cajón con cerradura digital.",
            currentStage = ProductionStage.CALIDAD,
            currentStageStartedAt = now - (5 * hour), // 5 hours (threshold is 12h -> ON TIME)
            createdAt = now - (4 * day),
            targetDeliveryDate = now + (1 * day),
            priority = Priority.MEDIA,
            isCompleted = false
        )
        val id4 = orderDao.insert(order4)
        progressDao.insert(
            StageProgressRecord(
                orderId = id4,
                fromStage = ProductionStage.ACABADO,
                toStage = ProductionStage.CALIDAD,
                timestamp = now - (5 * hour),
                operatorName = "Ing. Manuel Soto",
                notes = "Revisión de apertura de gavetas y firmeza de ensamble de uniones ocultas.",
                hoursInPreviousStage = 26.0,
                wasDelayed = false
            )
        )

        // 5. Order in Recepción - ON TIME
        val order5 = ProductionOrder(
            orderCode = "ORD-2026-005",
            clientName = "Dra. Claudia Lozano",
            clientPhone = "+57 312 990 4455",
            furnitureType = "Mueble Aparador Buffet Escandinavo",
            woodType = "Teca y Enchape Fresno",
            finishType = "Aceite de Teca Natural",
            dimensions = "160 x 45 x 85 cm",
            notes = "Patas cónicas en ángulo de 15 grados. Puertas correderas con ranuras fresadas.",
            currentStage = ProductionStage.RECEPCION,
            currentStageStartedAt = now - (8 * hour), // 8 hours (threshold is 24h -> ON TIME)
            createdAt = now - (8 * hour),
            targetDeliveryDate = now + (8 * day),
            priority = Priority.BAJA,
            isCompleted = false
        )
        orderDao.insert(order5)

        // 6. Completed Order - Delivered
        val order6 = ProductionOrder(
            orderCode = "ORD-2026-006",
            clientName = "Club Campestre La Pradera",
            clientPhone = "+57 318 665 4321",
            furnitureType = "Vitrina Exhibidora de Copas y Vinos",
            woodType = "Caoba y Cedro",
            finishType = "Barniz Marino Brillante",
            dimensions = "120 x 40 x 190 cm",
            notes = "Puertas con vidrios biselados de 5mm y chapa de seguridad.",
            currentStage = ProductionStage.LISTO,
            currentStageStartedAt = now - (1 * day),
            createdAt = now - (10 * day),
            targetDeliveryDate = now - (1 * day),
            priority = Priority.MEDIA,
            isCompleted = true,
            completedAt = now - (1 * day)
        )
        val id6 = orderDao.insert(order6)
        progressDao.insert(
            StageProgressRecord(
                orderId = id6,
                fromStage = ProductionStage.CALIDAD,
                toStage = ProductionStage.LISTO,
                timestamp = now - (1 * day),
                operatorName = "Ing. Manuel Soto",
                notes = "Aprobado 100% en inspección final. Embalado con plástico burbuja y esquineros. Listo para despacho.",
                hoursInPreviousStage = 10.0,
                wasDelayed = false
            )
        )
    }
}
