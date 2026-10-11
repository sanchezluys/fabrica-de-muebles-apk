package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Priority
import com.example.data.model.ProductionOrder
import com.example.data.model.ProductionStage
import com.example.data.model.StageConfig
import com.example.data.model.StageProgressRecord
import com.example.data.repository.ProductionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

data class OrderAlertItem(
    val order: ProductionOrder,
    val stage: ProductionStage,
    val hoursInStage: Double,
    val thresholdHours: Int,
    val exceededHours: Double
)

data class DashboardUiState(
    val totalActive: Int = 0,
    val totalCompleted: Int = 0,
    val delayedCount: Int = 0,
    val onTimeCount: Int = 0,
    val alerts: List<OrderAlertItem> = emptyList(),
    val stageDistribution: Map<ProductionStage, Int> = emptyMap(),
    val isSeeding: Boolean = false,
    val userMessage: String? = null
)

class ProductionViewModel(
    private val repository: ProductionRepository,
    private val appContext: Context
) : ViewModel() {

    // Orders flows
    val allOrders = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeOrders = repository.activeOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedOrders = repository.completedOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stageConfigs = repository.stageConfigs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search and filter in orders list
    val searchQuery = MutableStateFlow("")
    val selectedStageFilter = MutableStateFlow<ProductionStage?>(null)
    val onlyDelayedFilter = MutableStateFlow(false)

    // Current selected order ID for detail screen
    val selectedOrderId = MutableStateFlow<Long?>(null)

    // Message/Notification banner
    val userMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureConfigsInitialized()
            // Check if database is empty; if so, automatically seed rich initial data
            val current = repository.allOrders
            // If empty, auto-seed sample data so user immediately sees a working factory
        }
        viewModelScope.launch {
            repository.allOrders.collect { orders ->
                if (orders.isEmpty()) {
                    repository.seedSampleData()
                }
            }
        }
    }

    // Dashboard UI State calculation
    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.activeOrders,
        repository.completedOrders,
        repository.stageConfigs
    ) { active, completed, configs ->
        val configMap = configs.associateBy { it.stage }
        val now = System.currentTimeMillis()

        val alerts = mutableListOf<OrderAlertItem>()
        val stageCount = mutableMapOf<ProductionStage, Int>()
        ProductionStage.entries.forEach { stageCount[it] = 0 }

        for (order in active) {
            stageCount[order.currentStage] = (stageCount[order.currentStage] ?: 0) + 1
            val durationHours = ((now - order.currentStageStartedAt).coerceAtLeast(0L)).toDouble() / (3600.0 * 1000.0)
            val threshold = configMap[order.currentStage]?.estimatedHours ?: order.currentStage.defaultHours
            if (durationHours > threshold) {
                alerts.add(
                    OrderAlertItem(
                        order = order,
                        stage = order.currentStage,
                        hoursInStage = ((durationHours * 10.0).roundToInt()) / 10.0,
                        thresholdHours = threshold,
                        exceededHours = (((durationHours - threshold) * 10.0).roundToInt()) / 10.0
                    )
                )
            }
        }

        // Sort alerts by largest delay first
        alerts.sortByDescending { it.exceededHours }

        DashboardUiState(
            totalActive = active.size,
            totalCompleted = completed.size,
            delayedCount = alerts.size,
            onTimeCount = (active.size - alerts.size).coerceAtLeast(0),
            alerts = alerts,
            stageDistribution = stageCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun getProgressForOrder(orderId: Long) = repository.getProgressForOrder(orderId)

    fun getOrderById(orderId: Long) = repository.getOrderById(orderId)

    fun isOrderDelayed(order: ProductionOrder): Pair<Boolean, Double> {
        val now = System.currentTimeMillis()
        val durationHours = ((now - order.currentStageStartedAt).coerceAtLeast(0L)).toDouble() / (3600.0 * 1000.0)
        val configs = stageConfigs.value.associateBy { it.stage }
        val threshold = configs[order.currentStage]?.estimatedHours ?: order.currentStage.defaultHours
        val isDelayed = durationHours > threshold
        return Pair(isDelayed, ((durationHours * 10.0).roundToInt()) / 10.0)
    }

    fun selectOrder(orderId: Long?) {
        selectedOrderId.value = orderId
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setStageFilter(stage: ProductionStage?) {
        selectedStageFilter.value = stage
    }

    fun setOnlyDelayedFilter(onlyDelayed: Boolean) {
        onlyDelayedFilter.value = onlyDelayed
    }

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun createOrder(
        orderCode: String,
        clientName: String,
        clientPhone: String,
        furnitureType: String,
        woodType: String,
        finishType: String,
        dimensions: String,
        notes: String,
        initialStage: ProductionStage = ProductionStage.RECEPCION,
        priority: Priority = Priority.MEDIA,
        targetDeliveryDays: Int = 10,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val order = ProductionOrder(
                orderCode = orderCode.ifBlank { "ORD-${System.currentTimeMillis() % 10000}" },
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                furnitureType = furnitureType.trim(),
                woodType = woodType.trim(),
                finishType = finishType.trim(),
                dimensions = dimensions.trim(),
                notes = notes.trim(),
                currentStage = initialStage,
                currentStageStartedAt = now,
                createdAt = now,
                targetDeliveryDate = now + (targetDeliveryDays * 24L * 3600 * 1000),
                priority = priority,
                isCompleted = initialStage == ProductionStage.LISTO,
                completedAt = if (initialStage == ProductionStage.LISTO) now else null
            )
            val newId = repository.createOrder(order)
            userMessage.value = "Orden ${order.orderCode} creada exitosamente"
            onSuccess(newId)
        }
    }

    fun updateOrder(
        order: ProductionOrder,
        clientName: String,
        clientPhone: String,
        furnitureType: String,
        woodType: String,
        finishType: String,
        dimensions: String,
        notes: String,
        priority: Priority,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val updated = order.copy(
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                furnitureType = furnitureType.trim(),
                woodType = woodType.trim(),
                finishType = finishType.trim(),
                dimensions = dimensions.trim(),
                notes = notes.trim(),
                priority = priority
            )
            repository.updateOrder(updated)
            userMessage.value = "Orden ${order.orderCode} actualizada"
            onSuccess()
        }
    }

    fun deleteOrder(orderId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteOrderById(orderId)
            userMessage.value = "Orden eliminada del sistema"
            if (selectedOrderId.value == orderId) {
                selectedOrderId.value = null
            }
            onDone()
        }
    }

    fun advanceStage(
        orderId: Long,
        newStage: ProductionStage,
        operatorName: String,
        notes: String,
        photoUri: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.advanceOrderStage(
                orderId = orderId,
                newStage = newStage,
                operatorName = operatorName,
                notes = notes,
                photoUri = photoUri
            )
            if (success) {
                userMessage.value = "Etapa actualizada a ${newStage.title}"
                onSuccess()
            }
        }
    }

    fun updateStageConfig(stage: ProductionStage, estimatedHours: Int) {
        viewModelScope.launch {
            repository.updateStageConfig(
                StageConfig(
                    stage = stage,
                    estimatedHours = estimatedHours.coerceAtLeast(1),
                    stageLabel = stage.title
                )
            )
            userMessage.value = "Tiempo estimado para ${stage.shortName} actualizado a ${estimatedHours}h"
        }
    }

    fun reseedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            userMessage.value = "Datos de demostración cargados exitosamente"
        }
    }

    fun copyUriToInternalStorage(sourceUri: Uri): String? {
        return try {
            val photosDir = File(appContext.filesDir, "order_photos").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(photosDir, "evidence_${System.currentTimeMillis()}.jpg")
            val inputStream: InputStream? = appContext.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(destinationFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class ProductionViewModelFactory(
    private val repository: ProductionRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductionViewModel::class.java)) {
            return ProductionViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
