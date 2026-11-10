package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.screens.backup.BackupRestoreViewModel
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

    // Handler provided by SettingsScreen to guard navigation away when unsaved changes exist
    var settingsNavigationHandler by remember { mutableStateOf<((() -> Unit) -> Unit)?>(null) }

    val onboardingViewModel = remember { OnboardingViewModel(repository) }
    // The settings flow always emits a value; null here only means the first
    // emission has not arrived yet.
    val appSettingsState by repository.settingsFlow.collectAsState(
        initial = null as com.example.data.database.AppSettingsEntity?
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Initialize database settings before determining start destination
    LaunchedEffect(Unit) {
        repository.initialize()
    }

    // Wait until settings are loaded before deciding where to start, so an
    // incomplete onboarding can never briefly land on the dashboard.
    val loadedSettings = appSettingsState
    if (loadedSettings == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ClassicRed)
        }
        return
    }

    // Retain ViewModels across recompositions
    val dashboardViewModel = remember { DashboardViewModel(repository, aiCoordinator) }
    val inventoryViewModel = remember { InventoryViewModel(repository, aiCoordinator) }
    val weightTrackerViewModel = remember { WeightTrackerViewModel(repository) }
    val analyticsViewModel = remember { AnalyticsViewModel(repository) }
    val historyViewModel = remember { HistoryViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }
    val backupRestoreViewModel = remember { BackupRestoreViewModel(repository) }

    // If onboarding hasn't been completed yet, always start there.
    val startDestination = if (loadedSettings.hasCompletedOnboarding) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    // The navigation drawer must never be usable during onboarding.
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Onboarding.route) {
            drawerState.close()
        }
    }

    // Intercept back button if drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    fun navigateTo(screen: Screen) {
        coroutineScope.launch { drawerState.close() }
        if (currentRoute != screen.route) {
            // If leaving Settings with unsaved changes, let the SettingsScreen guard
            // the navigation (shows Save / Discard changes dialog).
            if (currentRoute == Screen.Settings.route && settingsNavigationHandler != null) {
                settingsNavigationHandler?.let { handler ->
                    handler {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            } else {
                navController.navigate(screen.route) {
                    popUpTo(Screen.Dashboard.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != Screen.Onboarding.route,
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

                    // Backup & Restore sits next to Settings at the very bottom
                    val isBackupSelected = currentRoute == Screen.BackupRestore.route
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = "Backup & Restore",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isBackupSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isBackupSelected) Color.White else TextSecondary
                                    )
                                )
                                Text(
                                    text = "Export & import your data",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Screen.BackupRestore.icon,
                                contentDescription = "Backup & Restore",
                                tint = if (isBackupSelected) ClassicRed else TextMuted
                            )
                        },
                        selected = isBackupSelected,
                        onClick = { navigateTo(Screen.BackupRestore) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF222630),
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("drawer_item_backup_restore")
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
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        fadeIn(animationSpec = tween(240)) +
                            slideInVertically(
                                animationSpec = tween(280),
                                initialOffsetY = { it / 24 }
                            )
                    },
                    exitTransition = { fadeOut(animationSpec = tween(180)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(240)) },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(180)) +
                            slideOutVertically(
                                animationSpec = tween(260),
                                targetOffsetY = { it / 24 }
                            )
                    }
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
                                supportingText = "Saved foods, brands and labels",
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
                                supportingText = "Weigh-ins and progress",
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
                                supportingText = "Trends across your logged days",
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
                                supportingText = "Every day, one page",
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
                    composable(Screen.BackupRestore.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Backup & Restore",
                                supportingText = "Export and import your data",
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            BackupRestoreScreen(
                                viewModel = backupRestoreViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    composable(Screen.Settings.route) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PageHeader(
                                title = "Settings",
                                supportingText = "AI model, targets and profile",
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
                                setNavigationHandler = { handler ->
                                    settingsNavigationHandler = handler
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
