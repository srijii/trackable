package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.NutritionAIServiceCoordinator
import com.example.data.repository.NutritionRepository
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.analytics.AnalyticsViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.inventory.InventoryViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.weight.WeightTrackerScreen
import com.example.ui.screens.weight.WeightTrackerViewModel
import com.example.ui.components.PageHeader
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ClassicRedGlow
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@Composable
fun NutriTrackApp(
    repository: NutritionRepository,
    aiCoordinator: NutritionAIServiceCoordinator,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val onboardingViewModel = remember { OnboardingViewModel(repository) }
    val appSettingsState by repository.settingsFlow.collectAsState(initial = null)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Initialize database settings before determining start destination
    LaunchedEffect(Unit) {
        repository.initialize()
    }

    // Retain ViewModels across recompositions
    val dashboardViewModel = remember { DashboardViewModel(repository, aiCoordinator) }
    val inventoryViewModel = remember { InventoryViewModel(repository) }
    val weightTrackerViewModel = remember { WeightTrackerViewModel(repository) }
    val analyticsViewModel = remember { AnalyticsViewModel(repository) }
    val historyViewModel = remember { HistoryViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    // Determine start destination: show dashboard if onboarded or still loading,
    // show onboarding only if we know onboarding is not completed
    val hasCompletedOnboarding = appSettingsState?.hasCompletedOnboarding == true
    val startDestination = if (appSettingsState == null || hasCompletedOnboarding) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    // Intercept back button if drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    fun navigateTo(screen: Screen) {
        coroutineScope.launch { drawerState.close() }
        if (currentRoute != screen.route) {
            navController.navigate(screen.route) {
                popUpTo(Screen.Dashboard.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF14161B),
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .testTag("left_sidebar_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Drawer Top Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ClassicRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TRACKABLE",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ClassicRed)
                                    )
                                }
                                Text(
                                    text = "CLASSIC ATHLETIC EDITION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = SurfaceBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Navigation Options from the top
                    Text(
                        text = "NAVIGATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )

                    val topScreens = listOf(
                        Screen.Dashboard,
                        Screen.Inventory,
                        Screen.WeightTracker,
                        Screen.Analytics,
                        Screen.History
                    )

                    topScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (isSelected) ClassicRed else TextMuted
                                )
                            },
                            selected = isSelected,
                            onClick = { navigateTo(screen) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF222630),
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .testTag("drawer_item_${screen.route}")
                        )
                    }

                    // Spacer pushes Settings to the VERY BOTTOM
                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(
                        color = SurfaceBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Settings at the most bottom
                    val isSettingsSelected = currentRoute == Screen.Settings.route
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSettingsSelected) Color.White else TextSecondary
                                    )
                                )
                                Text(
                                    text = "AI Models & Nutritional Targets",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isSettingsSelected) ClassicRed else TextMuted
                            )
                        },
                        selected = isSettingsSelected,
                        onClick = { navigateTo(Screen.Settings) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF222630),
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("drawer_item_settings")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trackable v1.2.0 • Offline Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = BackgroundDark,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            onFinish = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Onboarding.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onNavigateToSettings = {
                                navigateTo(Screen.Settings)
                            }
                        )
                    }
                    composable(Screen.Inventory.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Food Inventory",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            InventoryScreen(
                                viewModel = inventoryViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    composable(Screen.WeightTracker.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Weight Tracker",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            WeightTrackerScreen(
                                viewModel = weightTrackerViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    composable(Screen.Analytics.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Analysis",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            AnalyticsScreen(
                                viewModel = analyticsViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    composable(Screen.History.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "History",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            HistoryScreen(
                                viewModel = historyViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    composable(Screen.Settings.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Settings",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onSaveComplete = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
