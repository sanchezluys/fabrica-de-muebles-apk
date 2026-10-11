package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.ProductionRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdvanceStageScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderFormScreen
import com.example.ui.screens.OrderListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ProductionViewModel
import com.example.viewmodel.ProductionViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: ProductionViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProductionRepository(
            orderDao = database.orderDao(),
            progressDao = database.progressDao(),
            stageConfigDao = database.stageConfigDao()
        )
        ProductionViewModelFactory(repository, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FurnitureFactoryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FurnitureFactoryApp(viewModel: ProductionViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedDetailOrderId by remember { mutableStateOf<Long?>(null) }
    var selectedAdvanceOrderId by remember { mutableStateOf<Long?>(null) }

    val showBottomNav = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.OrderList ||
            currentScreen is Screen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav")
                ) {
                    // Dashboard
                    NavigationBarItem(
                        selected = currentScreen is Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Dashboard) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Panel", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    // Order List
                    NavigationBarItem(
                        selected = currentScreen is Screen.OrderList,
                        onClick = {
                            viewModel.setOnlyDelayedFilter(false)
                            viewModel.setStageFilter(null)
                            currentScreen = Screen.OrderList
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.OrderList) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                                contentDescription = "Órdenes"
                            )
                        },
                        label = { Text("Órdenes", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_orders")
                    )

                    // Settings
                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Settings) Icons.Default.Tune else Icons.Outlined.Tune,
                                contentDescription = "Ajustes"
                            )
                        },
                        label = { Text("Ajustes", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToOrderList = { onlyDelayed ->
                            viewModel.setOnlyDelayedFilter(onlyDelayed)
                            currentScreen = Screen.OrderList
                        },
                        onNavigateToDetail = { orderId ->
                            selectedDetailOrderId = orderId
                            currentScreen = Screen.OrderDetail
                        },
                        onNavigateToCreate = {
                            currentScreen = Screen.CreateOrder
                        },
                        onNavigateToAdvance = { orderId ->
                            selectedAdvanceOrderId = orderId
                            currentScreen = Screen.AdvanceStage
                        }
                    )
                }

                is Screen.OrderList -> {
                    BackHandler {
                        currentScreen = Screen.Dashboard
                    }
                    OrderListScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { orderId ->
                            selectedDetailOrderId = orderId
                            currentScreen = Screen.OrderDetail
                        },
                        onNavigateToCreate = {
                            currentScreen = Screen.CreateOrder
                        },
                        onNavigateToAdvance = { orderId ->
                            selectedAdvanceOrderId = orderId
                            currentScreen = Screen.AdvanceStage
                        }
                    )
                }

                is Screen.OrderDetail -> {
                    val orderId = selectedDetailOrderId ?: 0L
                    OrderDetailScreen(
                        orderId = orderId,
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.OrderList },
                        onNavigateToAdvance = { id ->
                            selectedAdvanceOrderId = id
                            currentScreen = Screen.AdvanceStage
                        }
                    )
                }

                is Screen.CreateOrder -> {
                    OrderFormScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard },
                        onOrderCreated = { newId ->
                            selectedDetailOrderId = newId
                            currentScreen = Screen.OrderDetail
                        }
                    )
                }

                is Screen.AdvanceStage -> {
                    val orderId = selectedAdvanceOrderId ?: 0L
                    AdvanceStageScreen(
                        orderId = orderId,
                        viewModel = viewModel,
                        onBack = {
                            if (selectedDetailOrderId != null) {
                                currentScreen = Screen.OrderDetail
                            } else {
                                currentScreen = Screen.Dashboard
                            }
                        }
                    )
                }

                is Screen.Settings -> {
                    BackHandler {
                        currentScreen = Screen.Dashboard
                    }
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
