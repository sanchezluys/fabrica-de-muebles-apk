package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.update.PlayStoreUpdateManager
import com.example.update.UpdateUiState
import com.example.viewmodel.ProductionViewModel
import com.example.viewmodel.ProductionViewModelFactory
import com.google.android.play.core.appupdate.AppUpdateInfo

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

    private lateinit var playStoreUpdateManager: PlayStoreUpdateManager

    private val updateFlowLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            playStoreUpdateManager.checkForUpdates(isManualCheck = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playStoreUpdateManager = PlayStoreUpdateManager(this)

        setContent {
            MyApplicationTheme {
                val updateState by playStoreUpdateManager.updateState.collectAsState()
                val isBannerDismissed by playStoreUpdateManager.isBannerDismissed.collectAsState()

                FurnitureFactoryApp(
                    viewModel = viewModel,
                    updateState = updateState,
                    isUpdateBannerDismissed = isBannerDismissed,
                    onStartUpdate = { appUpdateInfo, preferImmediate ->
                        playStoreUpdateManager.startUpdate(
                            appUpdateInfo = appUpdateInfo,
                            launcher = updateFlowLauncher,
                            preferImmediate = preferImmediate
                        )
                    },
                    onCompleteUpdate = {
                        playStoreUpdateManager.completeUpdate()
                    },
                    onDismissUpdateBanner = {
                        playStoreUpdateManager.dismissBanner()
                    },
                    onManualCheckUpdates = {
                        playStoreUpdateManager.checkForUpdates(
                            isManualCheck = true,
                            onResumeImmediateIfNeeded = { info ->
                                playStoreUpdateManager.startUpdate(
                                    appUpdateInfo = info,
                                    launcher = updateFlowLauncher,
                                    preferImmediate = true
                                )
                            }
                        )
                    },
                    onClearManualUpdateStatus = {
                        playStoreUpdateManager.clearManualStatus()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playStoreUpdateManager.registerListener()
    }

    override fun onResume() {
        super.onResume()
        playStoreUpdateManager.checkForUpdates(
            isManualCheck = false,
            onResumeImmediateIfNeeded = { appUpdateInfo ->
                playStoreUpdateManager.startUpdate(
                    appUpdateInfo = appUpdateInfo,
                    launcher = updateFlowLauncher,
                    preferImmediate = true
                )
            }
        )
    }

    override fun onStop() {
        playStoreUpdateManager.unregisterListener()
        super.onStop()
    }
}

@Composable
fun FurnitureFactoryApp(
    viewModel: ProductionViewModel,
    updateState: UpdateUiState = UpdateUiState.Idle,
    isUpdateBannerDismissed: Boolean = false,
    onStartUpdate: (AppUpdateInfo, Boolean) -> Unit = { _, _ -> },
    onCompleteUpdate: () -> Unit = {},
    onDismissUpdateBanner: () -> Unit = {},
    onManualCheckUpdates: () -> Unit = {},
    onClearManualUpdateStatus: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedDetailOrderId by remember { mutableStateOf<Long?>(null) }
    var selectedAdvanceOrderId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(updateState) {
        if (updateState is UpdateUiState.Downloaded) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.update_downloaded_message),
                actionLabel = context.getString(R.string.update_action_restart),
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                onCompleteUpdate()
            }
        }
    }

    val showBottomNav = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.OrderList ||
            currentScreen is Screen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
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
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        updateState = updateState,
                        isUpdateBannerDismissed = isUpdateBannerDismissed,
                        onStartUpdate = onStartUpdate,
                        onCompleteUpdate = onCompleteUpdate,
                        onDismissUpdateBanner = onDismissUpdateBanner,
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
                    SettingsScreen(
                        viewModel = viewModel,
                        updateState = updateState,
                        onStartUpdate = onStartUpdate,
                        onCompleteUpdate = onCompleteUpdate,
                        onCheckForUpdates = onManualCheckUpdates,
                        onClearUpdateFeedback = onClearManualUpdateStatus
                    )
                }
            }
        }
    }
}
