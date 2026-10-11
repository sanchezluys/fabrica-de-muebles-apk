package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductionStage
import com.example.ui.components.OfflineIndicatorBanner
import com.example.ui.components.OrderCard
import com.example.viewmodel.ProductionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    viewModel: ProductionViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToAdvance: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.allOrders.collectAsState()
    val configs by viewModel.stageConfigs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStage by viewModel.selectedStageFilter.collectAsState()
    val onlyDelayed by viewModel.onlyDelayedFilter.collectAsState()

    val configMap = configs.associateBy { it.stage }

    // Filter logic
    val filteredOrders = orders.filter { order ->
        val matchesQuery = if (searchQuery.isBlank()) true else {
            val q = searchQuery.lowercase()
            order.orderCode.lowercase().contains(q) ||
                    order.clientName.lowercase().contains(q) ||
                    order.furnitureType.lowercase().contains(q) ||
                    order.woodType.lowercase().contains(q)
        }

        val matchesStage = selectedStage == null || order.currentStage == selectedStage

        val isDelayed = if (!order.isCompleted) {
            val (delayed, _) = viewModel.isOrderDelayed(order)
            delayed
        } else false

        val matchesDelay = !onlyDelayed || isDelayed

        matchesQuery && matchesStage && matchesDelay
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Órdenes de Fabricación",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_order_list")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva Orden"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Chip
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OfflineIndicatorBanner()
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_order_input"),
                placeholder = { Text("Buscar por código, cliente o mueble...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Solo retrasos" filter chip
                item {
                    FilterChip(
                        selected = onlyDelayed,
                        onClick = { viewModel.setOnlyDelayedFilter(!onlyDelayed) },
                        label = { Text("⚠️ Con Retraso", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier.testTag("filter_delayed_chip")
                    )
                }

                // "Todas"
                item {
                    FilterChip(
                        selected = selectedStage == null,
                        onClick = { viewModel.setStageFilter(null) },
                        label = { Text("Todas", fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                }

                // Individual stage chips
                items(ProductionStage.entries) { stage ->
                    FilterChip(
                        selected = selectedStage == stage,
                        onClick = {
                            viewModel.setStageFilter(if (selectedStage == stage) null else stage)
                        },
                        label = { Text(stage.shortName, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_stage_${stage.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Order List
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No se encontraron órdenes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prueba cambiando los filtros o crea una nueva orden de fabricación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        val (isDelayed, hoursInStage) = viewModel.isOrderDelayed(order)
                        val threshold = configMap[order.currentStage]?.estimatedHours ?: order.currentStage.defaultHours

                        OrderCard(
                            order = order,
                            isDelayed = isDelayed,
                            hoursInStage = hoursInStage,
                            thresholdHours = threshold,
                            onClick = { onNavigateToDetail(order.id) },
                            onAdvanceClick = { onNavigateToAdvance(order.id) }
                        )
                    }
                }
            }
        }
    }
}
