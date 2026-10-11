package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object OrderList : Screen("order_list")
    data object OrderDetail : Screen("order_detail")
    data object CreateOrder : Screen("create_order")
    data object AdvanceStage : Screen("advance_stage")
    data object Settings : Screen("settings")
}
