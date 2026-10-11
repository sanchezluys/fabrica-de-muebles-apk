package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.ProductionStage
import com.example.ui.components.OfflineIndicatorBanner
import com.example.viewmodel.ProductionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    viewModel: ProductionViewModel,
    onBack: () -> Unit,
    onOrderCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val randomSuffix = remember { (100..999).random() }
    var orderCode by remember { mutableStateOf("ORD-2026-$randomSuffix") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var furnitureType by remember { mutableStateOf("") }
    var woodType by remember { mutableStateOf("Roble Macizo") }
    var finishType by remember { mutableStateOf("Poliuretano Mate") }
    var dimensions by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIA) }
    var deliveryDays by remember { mutableStateOf("12") }

    var clientError by remember { mutableStateOf(false) }
    var furnitureError by remember { mutableStateOf(false) }

    val popularFurniture = listOf(
        "Mesa de Comedor (8 Puestos)",
        "Armario Vestidor 3 Puertas",
        "Juego de Alcoba King Size",
        "Mueble de Cocina Integral",
        "Escritorio Ejecutivo en L",
        "Biblioteca Modular con Nichos"
    )

    val popularWoods = listOf(
        "Roble Macizo",
        "Cedro Rosado",
        "Pino Insigne",
        "Nogal Americano",
        "Teca",
        "MDF Melamínico"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nueva Orden de Fabricación",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_create_order")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OfflineIndicatorBanner()
            }

            // Datos de la orden y cliente
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Datos de Orden y Cliente",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = orderCode,
                            onValueChange = { orderCode = it },
                            label = { Text("Código de Orden *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_order_code"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = clientName,
                            onValueChange = {
                                clientName = it
                                if (it.isNotBlank()) clientError = false
                            },
                            label = { Text("Nombre del Cliente *") },
                            isError = clientError,
                            supportingText = { if (clientError) Text("El cliente es requerido", color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_client_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it },
                            label = { Text("Teléfono de Contacto") },
                            placeholder = { Text("+57 300 000 0000") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_client_phone"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Especificaciones del Mueble
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Especificaciones del Mueble",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = furnitureType,
                            onValueChange = {
                                furnitureType = it
                                if (it.isNotBlank()) furnitureError = false
                            },
                            label = { Text("Tipo de Mueble / Descripción *") },
                            placeholder = { Text("Ej: Mesa de Comedor 8 Puestos") },
                            isError = furnitureError,
                            supportingText = { if (furnitureError) Text("El mueble es requerido", color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_furniture_type"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick suggestion chips
                        Text(
                            text = "Sugerencias frecuentes:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(popularFurniture) { item ->
                                FilterChip(
                                    selected = furnitureType == item,
                                    onClick = { furnitureType = item },
                                    label = { Text(item, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Wood type
                        OutlinedTextField(
                            value = woodType,
                            onValueChange = { woodType = it },
                            label = { Text("Tipo de Madera / Tablero *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_wood_type"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(popularWoods) { wood ->
                                FilterChip(
                                    selected = woodType == wood,
                                    onClick = { woodType = wood },
                                    label = { Text(wood, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Dimensions
                        OutlinedTextField(
                            value = dimensions,
                            onValueChange = { dimensions = it },
                            label = { Text("Medidas (Alto x Ancho x Profundidad)") },
                            placeholder = { Text("Ej: 200 x 100 x 78 cm") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_dimensions"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Finish
                        OutlinedTextField(
                            value = finishType,
                            onValueChange = { finishType = it },
                            label = { Text("Acabado y Tono") },
                            placeholder = { Text("Ej: Poliuretano Mate, Tinte Nogal") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_finish_type"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Tiempos y Prioridad
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tiempos y Prioridad",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = deliveryDays,
                                onValueChange = { deliveryDays = it },
                                label = { Text("Días para entrega") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_delivery_days"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Text(
                            text = "Nivel de Prioridad:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Priority.entries.forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = { Text(p.label, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notas Técnicas / Observaciones") },
                            placeholder = { Text("Detalles de herrajes, uniones o especificaciones especiales...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("input_order_notes"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3
                        )
                    }
                }
            }

            // Action Button
            item {
                Button(
                    onClick = {
                        if (clientName.isBlank()) {
                            clientError = true
                            return@Button
                        }
                        if (furnitureType.isBlank()) {
                            furnitureError = true
                            return@Button
                        }

                        val days = deliveryDays.toIntOrNull() ?: 10
                        viewModel.createOrder(
                            orderCode = orderCode,
                            clientName = clientName,
                            clientPhone = clientPhone,
                            furnitureType = furnitureType,
                            woodType = woodType,
                            finishType = finishType,
                            dimensions = dimensions,
                            notes = notes,
                            initialStage = ProductionStage.RECEPCION,
                            priority = priority,
                            targetDeliveryDays = days,
                            onSuccess = { newId ->
                                onOrderCreated(newId)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_submit_create_order"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Registrar Orden en Producción",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
