package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Person)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.PieChart)
    data object Inventory : Screen("inventory", "Inventory", Icons.Default.Kitchen)
    data object WeightTracker : Screen("weight_tracker", "Weight Tracker", Icons.Default.Scale)
    data object Analytics : Screen("analytics", "Analysis", Icons.Default.BarChart)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items: List<Screen>
            get() = listOf(Dashboard, Inventory, WeightTracker, Analytics, History)

        val drawerTopItems: List<Screen>
            get() = listOf(Dashboard, Inventory, WeightTracker, Analytics, History)
    }
}
