package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.database.AppSettingsEntity
import com.example.data.database.NutritionGoalEntity
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProgressCyanBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterCyan
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class AiModelItem(
    val id: String,
    val provider: String, // "GEMINI", "NVIDIA_NIM", "OPENAI", "LOCAL"
    val modelName: String,
    val title: String,
    val subtitle: String,
    val requiresApiKey: Boolean,
    val badgeLabel: String,
    val badgeColor: Color
)

val AVAILABLE_AI_MODELS = listOf(
    AiModelItem(
        id = "gemini_3_6_flash",
        provider = "GEMINI",
        modelName = "gemini-3.6-flash",
        title = "Google Gemini — Flash 3.6",
        subtitle = "Confirmed working model",
        requiresApiKey = true,
        badgeLabel = "Google",
        badgeColor = Color(0xFF4285F4)
    ),
    AiModelItem(
        id = "nvidia_minimax_m3_1_flash",
        provider = "NVIDIA_NIM",
        modelName = "minimaxai/minimax-m3.1-flash",
        title = "NVIDIA NIM — Minimax M3.1 Flash",
        subtitle = "Confirmed working model on NVIDIA NIM",
        requiresApiKey = true,
        badgeLabel = "NVIDIA",
        badgeColor = Color(0xFF76B900)
    ),
    AiModelItem(
        id = "local_offline",
        provider = "LOCAL",
        modelName = "local-smart",
        title = "Local Smart Engine (Offline)",
        subtitle = "100% on-device & private. No API key required",
        requiresApiKey = false,
        badgeLabel = "Offline",
        badgeColor = PrimaryGreen
    )
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSaveComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentGoals by viewModel.goals.collectAsStateWithLifecycle()
    val currentAppSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val testError by viewModel.testError.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val goals = currentGoals ?: NutritionGoalEntity()
    val appSettings = currentAppSettings ?: AppSettingsEntity()

    // Initialize originals for unsaved changes tracking
    LaunchedEffect(goals, appSettings) {
        viewModel.initializeOriginals(goals, appSettings)
    }

    // Determine currently active AI model option
    val initialModelOption = remember(appSettings) {
        when (appSettings.aiProvider) {
            "GEMINI" -> AVAILABLE_AI_MODELS.find { it.provider == "GEMINI" && it.modelName == appSettings.geminiModel }
                ?: AVAILABLE_AI_MODELS[0]
            "NVIDIA_NIM" -> AVAILABLE_AI_MODELS.find { it.provider == "NVIDIA_NIM" && it.modelName == appSettings.nvidiaModel }
                ?: AVAILABLE_AI_MODELS.find { it.provider == "NVIDIA_NIM" } ?: AVAILABLE_AI_MODELS[0]
            "OPENAI" -> AVAILABLE_AI_MODELS.find { it.provider == "OPENAI" && it.modelName == appSettings.openaiModel }
                ?: AVAILABLE_AI_MODELS.find { it.provider == "OPENAI" } ?: AVAILABLE_AI_MODELS[0]
            "LOCAL" -> AVAILABLE_AI_MODELS.find { it.provider == "LOCAL" } ?: AVAILABLE_AI_MODELS[0]
            else -> AVAILABLE_AI_MODELS[0]
        }
    }

    var selectedModelOption by remember(initialModelOption) { mutableStateOf(initialModelOption) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    // API Keys state
    var geminiApiKey by remember(appSettings) { mutableStateOf(appSettings.geminiApiKey.trim()) }
    var nvidiaApiKey by remember(appSettings) { mutableStateOf(appSettings.nvidiaApiKey.trim()) }
    var nvidiaBaseUrl by remember(appSettings) { mutableStateOf(appSettings.nvidiaBaseUrl.trim()) }
    var openaiApiKey by remember(appSettings) { mutableStateOf(appSettings.openaiApiKey.trim()) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showNvidiaKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }

    // Goal targets
    var calTarget by remember(goals) { mutableStateOf("${goals.caloriesGoal.toInt()}") }
    var proteinTarget by remember(goals) { mutableStateOf("${goals.proteinGoal.toInt()}") }
    var carbsTarget by remember(goals) { mutableStateOf("${goals.carbsGoal.toInt()}") }
    var fatTarget by remember(goals) { mutableStateOf("${goals.fatGoal.toInt()}") }
    var waterTarget by remember(goals) { mutableStateOf("${goals.waterGoalMl}") }

    // Water cup sizes
    var cupSize1Target by remember(appSettings) { mutableStateOf("${appSettings.cupSize1Ml}") }
    var cupSize2Target by remember(appSettings) { mutableStateOf("${appSettings.cupSize2Ml}") }

    // Profile
    var weightText by remember(goals) { mutableStateOf(goals.currentWeightKg?.toString() ?: "") }
    var targetWeightText by remember(goals) { mutableStateOf(goals.targetWeightKg?.toString() ?: "") }
    var heightText by remember(goals) { mutableStateOf(goals.heightCm?.toString() ?: "") }
    var ageText by remember(goals) { mutableStateOf(goals.age?.toString() ?: "") }
    var activityLevel by remember(goals) { mutableStateOf(goals.activityLevel) }

    // Start date
    var useStartDateLocal by remember(appSettings) { mutableStateOf(appSettings.useStartDate) }
    var startDateLocal by remember(appSettings) { mutableStateOf(appSettings.startDate) }
    var blockBeforeStartLocal by remember(appSettings) { mutableStateOf(appSettings.blockBeforeStartDate) }
    var blockAfterTodayLocal by remember(appSettings) { mutableStateOf(appSettings.blockAfterToday) }

    var activityMenuExpanded by remember { mutableStateOf(false) }
    val activityLevels = listOf("Sedentary", "Light", "Moderate", "Very Active", "Extra Active")

    // Track selected AI model
    LaunchedEffect(selectedModelOption) {
        viewModel.setSelectedAiModel(selectedModelOption.provider, selectedModelOption.modelName)
    }

    // Unsaved changes tracking
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    LaunchedEffect(
        geminiApiKey, nvidiaApiKey, openaiApiKey, nvidiaBaseUrl,
        calTarget, proteinTarget, carbsTarget, fatTarget, waterTarget,
        cupSize1Target, cupSize2Target,
        weightText, targetWeightText, heightText, ageText, activityLevel,
        useStartDateLocal, startDateLocal, blockBeforeStartLocal, blockAfterTodayLocal
    ) {
        if (viewModel.hasUnsavedChanges(
            currentGoals, currentAppSettings,
            geminiApiKey, nvidiaApiKey, openaiApiKey, nvidiaBaseUrl,
            calTarget, proteinTarget, carbsTarget, fatTarget, waterTarget,
            cupSize1Target, cupSize2Target,
            weightText, targetWeightText, heightText, ageText, activityLevel,
            useStartDateLocal, startDateLocal, blockBeforeStartLocal, blockAfterTodayLocal
        )) {
            hasUnsavedChanges = true
        }
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var showTestErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(testError) {
        if (testError != null) {
            showTestErrorDialog = true
        }
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Handle save result - navigate to dashboard on success
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.success && result.navigateToDashboard) {
                viewModel.clearSaveResult()
                onSaveComplete()
            } else if (result.success) {
                viewModel.clearSaveResult()
                hasUnsavedChanges = false
                viewModel.clearMessage()
            }
        }
    }

    // Back handler for unsaved changes
    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedDialog = true
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "SETTINGS & INTEGRATIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "AI Model & General Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // =====================================================================
            // 1. AI MODEL & API KEY CARD
            // =====================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Model Details",
                                    tint = ProgressCyanBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Model & API Key",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(selectedModelOption.badgeColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedModelOption.badgeLabel,
                                    color = selectedModelOption.badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "Select your AI engine and enter your API key. When you log meals, the AI extracts the exact proportional multiplier and matches inventory foods.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )

                        // AI MODEL DROPDOWN
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SELECT AI MODEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF13171F))
                                        .border(1.dp, ProgressCyanBlue.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .clickable { isModelDropdownExpanded = true }
                                        .padding(14.dp)
                                        .testTag("ai_model_dropdown_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = selectedModelOption.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(selectedModelOption.badgeColor.copy(alpha = 0.25f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = selectedModelOption.badgeLabel,
                                                    color = selectedModelOption.badgeColor,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedModelOption.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isModelDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = ProgressCyanBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isModelDropdownExpanded,
                                    onDismissRequest = { isModelDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SurfaceDark)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                ) {
                                    AVAILABLE_AI_MODELS.forEach { modelItem ->
                                        val isCurrent = modelItem.id == selectedModelOption.id
                                        DropdownMenuItem(
                                            text = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = modelItem.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = if (isCurrent) ProgressCyanBlue else TextPrimary,
                                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(modelItem.badgeColor.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = modelItem.badgeLabel,
                                                                color = modelItem.badgeColor,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = modelItem.subtitle,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = TextSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedModelOption = modelItem
                                                isModelDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("ai_option_${modelItem.id}")
                                        )
                                    }
                                }
                            }
                        }

                        // API KEY INPUT
                        if (selectedModelOption.requiresApiKey) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "API Key",
                                        tint = FlameOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedModelOption.badgeLabel} API Key Required",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = FlameOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                when (selectedModelOption.provider) {
                                    "GEMINI" -> {
                                        OutlinedTextField(
                                            value = geminiApiKey,
                                            onValueChange = { geminiApiKey = it },
                                            label = { Text("Gemini API Key") },
                                            placeholder = {
                                                val defaultKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
                                                Text(if (defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY") "Using injected key from Secrets" else "Paste your AI Studio Gemini API key")
                                            },
                                            visualTransformation = if (false) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { /* visibility toggle removed for simplicity */ }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Visibility,
                                                        contentDescription = "Toggle key visibility",
                                                        tint = PrimaryGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("gemini_api_key_input"),
                                            colors = customTextFieldColors(),
                                            singleLine = true
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF13171F))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Google AI Studio API key. Model: ${selectedModelOption.modelName}.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    "NVIDIA_NIM" -> {
                                        OutlinedTextField(
                                            value = nvidiaApiKey,
                                            onValueChange = { nvidiaApiKey = it },
                                            label = { Text("NVIDIA NIM API Key") },
                                            placeholder = { Text("nvapi-...") },
                                            visualTransformation = if (false) VisualTransformation.None else PasswordVisualTransformation(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("nvidia_api_key_input"),
                                            colors = customTextFieldColors(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = nvidiaBaseUrl,
                                            onValueChange = { nvidiaBaseUrl = it },
                                            label = { Text("NVIDIA NIM Base URL") },
                                            placeholder = { Text("https://integrate.api.nvidia.com/v1") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = customTextFieldColors(),
                                            singleLine = true
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF13171F))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "NVIDIA NIM API key from build.nvidia.com. Model: ${selectedModelOption.modelName}.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    "OPENAI" -> {
                                        OutlinedTextField(
                                            value = openaiApiKey,
                                            onValueChange = { openaiApiKey = it },
                                            label = { Text("OpenAI API Key") },
                                            placeholder = { Text("sk-...") },
                                            visualTransformation = if (false) VisualTransformation.None else PasswordVisualTransformation(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("openai_api_key_input"),
                                            colors = customTextFieldColors(),
                                            singleLine = true
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF13171F))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "OpenAI API key from platform.openai.com. Model: ${selectedModelOption.modelName}.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SAVE API BUTTON
                        Button(
                            onClick = {
                                val updatedSettings = appSettings.copy(
                                    aiProvider = selectedModelOption.provider,
                                    geminiApiKey = if (selectedModelOption.provider == "GEMINI") geminiApiKey.trim() else appSettings.geminiApiKey,
                                    geminiModel = if (selectedModelOption.provider == "GEMINI") selectedModelOption.modelName else appSettings.geminiModel,
                                    nvidiaApiKey = if (selectedModelOption.provider == "NVIDIA_NIM") nvidiaApiKey.trim() else appSettings.nvidiaApiKey,
                                    nvidiaModel = if (selectedModelOption.provider == "NVIDIA_NIM") selectedModelOption.modelName else appSettings.nvidiaModel,
                                    nvidiaBaseUrl = if (selectedModelOption.provider == "NVIDIA_NIM") nvidiaBaseUrl.trim().ifBlank { "https://integrate.api.nvidia.com/v1" } else appSettings.nvidiaBaseUrl,
                                    openaiApiKey = if (selectedModelOption.provider == "OPENAI") openaiApiKey.trim() else appSettings.openaiApiKey,
                                    openaiModel = if (selectedModelOption.provider == "OPENAI") selectedModelOption.modelName else appSettings.openaiModel
                                )
                                viewModel.saveApiSettings(updatedSettings)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_api_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProgressCyanBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTesting) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save API Key",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 2. DAILY NUTRITIONAL TARGETS
            // =====================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Nutritional Targets",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            // Autofill button
                            Button(
                                onClick = {
                                    val age = ageText.toIntOrNull()
                                    val weightKg = weightText.toDoubleOrNull()
                                    val targetWeightKg = targetWeightText.toDoubleOrNull()
                                    val heightCm = heightText.toDoubleOrNull()
                                    val result = viewModel.calculateNutrition(
                                        age = age,
                                        sex = goals.sex ?: "",
                                        currentWeightKg = weightKg,
                                        targetWeightKg = targetWeightKg,
                                        heightCm = heightCm,
                                        activityLevel = activityLevel
                                    )
                                    if (result != null) {
                                        calTarget = result.calories.toString()
                                        proteinTarget = result.protein.toString()
                                        carbsTarget = result.carbs.toString()
                                        fatTarget = result.fat.toString()
                                    }
                                },
                                modifier = Modifier.height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceBorder,
                                    contentColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Autofill",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        OutlinedTextField(
                            value = calTarget,
                            onValueChange = { calTarget = it.filter { c -> c.isDigit() } },
                            label = { Text("Daily Calorie Target (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("calories_target_input"),
                            colors = customTextFieldColors(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = proteinTarget,
                                onValueChange = { proteinTarget = it.filter { c -> c.isDigit() } },
                                label = { Text("Protein (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("protein_target_input"),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = carbsTarget,
                                onValueChange = { carbsTarget = it.filter { c -> c.isDigit() } },
                                label = { Text("Carbs (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("carbs_target_input"),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fatTarget,
                                onValueChange = { fatTarget = it.filter { c -> c.isDigit() } },
                                label = { Text("Fat (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("fat_target_input"),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = waterTarget,
                            onValueChange = { waterTarget = it.filter { c -> c.isDigit() } },
                            label = { Text("Daily Water Target (ml)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("water_target_input"),
                            colors = customTextFieldColors(),
                            singleLine = true
                        )
                    }
                }
            }

            // =====================================================================
            // 3. WATER CUP SIZES
            // =====================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = WaterCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Water Cup Sizes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Text(
                            text = "Set your two preferred cup sizes. These appear as quick-add buttons in Water Intake.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = cupSize1Target,
                                onValueChange = { cupSize1Target = it.filter { c -> c.isDigit() } },
                                label = { Text("Cup 1 (ml)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("cup_size_1_input"),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cupSize2Target,
                                onValueChange = { cupSize2Target = it.filter { c -> c.isDigit() } },
                                label = { Text("Cup 2 (ml)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("cup_size_2_input"),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // 4. TRACKING START DATE
            // =====================================================================
            item {
                val startDate = appSettings.startDate

                var showStartDatePicker by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Start date",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use Start Date",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Switch(
                            checked = useStartDateLocal,
                            onCheckedChange = { checked ->
                                useStartDateLocal = checked
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedTrackColor = PrimaryGreen,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = SurfaceBorder,
                                uncheckedThumbColor = TextMuted
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (!useStartDateLocal) {
                            "Start date tracking is disabled — all days are trackable."
                        } else if (startDateLocal == null) {
                            "Start date enabled but not set — set a date below."
                        } else {
                            "Tracking starts from $startDateLocal. Days before this date are untrackable."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (useStartDateLocal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProgressCyanBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (startDateLocal == null) "Set Start Date" else "Change Start Date")
                            }

                            if (startDateLocal != null) {
                                TextButton(
                                    onClick = {
                                        startDateLocal = null
                                        showStartDatePicker = false
                                    }
                                ) {
                                    Text("Clear", color = ClassicRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = blockBeforeStartLocal,
                                onCheckedChange = { checked ->
                                    blockBeforeStartLocal = checked
                                },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = PrimaryGreen,
                                    uncheckedColor = SurfaceBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Block adding before start date",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = blockAfterTodayLocal,
                            onCheckedChange = { checked ->
                                blockAfterTodayLocal = checked
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = PrimaryGreen,
                                uncheckedColor = SurfaceBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Block adding after today",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        )
                    }
                }

                if (showStartDatePicker && useStartDateLocal) {
                    StartDatePickerDialog(
                        initialDate = startDateLocal?.let { LocalDate.parse(it) },
                        onConfirm = { selected ->
                            startDateLocal = selected.toString()
                            showStartDatePicker = false
                        },
                        onDismiss = { showStartDatePicker = false }
                    )
                }
            }

            // =====================================================================
            // 5. PROFILE SECTION
            // =====================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ProgressCyanBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Profile (Optional)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = targetWeightText,
                                onValueChange = { targetWeightText = it },
                                label = { Text("Target Wt (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                label = { Text("Height (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                                label = { Text("Age (yrs)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = customTextFieldColors(),
                                singleLine = true
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = { activityMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Activity Level:", color = TextSecondary)
                                    Text(activityLevel, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                }
                            }

                            DropdownMenu(
                                expanded = activityMenuExpanded,
                                onDismissRequest = { activityMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                activityLevels.forEach { lvl ->
                                    DropdownMenuItem(
                                        text = { Text(lvl, color = TextPrimary) },
                                        onClick = {
                                            activityLevel = lvl
                                            activityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // SAVE BUTTONS
            // =====================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val updatedSettings = appSettings.copy(
                                aiProvider = selectedModelOption.provider,
                                geminiApiKey = geminiApiKey.trim(),
                                geminiModel = selectedModelOption.modelName,
                                nvidiaApiKey = appSettings.nvidiaApiKey,
                                nvidiaModel = appSettings.nvidiaModel,
                                nvidiaBaseUrl = appSettings.nvidiaBaseUrl,
                                openaiApiKey = appSettings.openaiApiKey,
                                openaiModel = appSettings.openaiModel
                            )
                            viewModel.saveApiSettings(updatedSettings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_api_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProgressCyanBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isTesting) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save API",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cal = calTarget.toDoubleOrNull() ?: 2200.0
                            val prot = proteinTarget.toDoubleOrNull() ?: 140.0
                            val carbs = carbsTarget.toDoubleOrNull() ?: 220.0
                            val fat = fatTarget.toDoubleOrNull() ?: 65.0
                            val water = waterTarget.toIntOrNull() ?: 3000
                            val cup1 = cupSize1Target.toIntOrNull() ?: 250
                            val cup2 = cupSize2Target.toIntOrNull() ?: 500

                            val updatedGoals = goals.copy(
                                caloriesGoal = cal,
                                proteinGoal = prot,
                                carbsGoal = carbs,
                                fatGoal = fat,
                                waterGoalMl = water,
                                currentWeightKg = weightText.toDoubleOrNull(),
                                targetWeightKg = targetWeightText.toDoubleOrNull(),
                                heightCm = heightText.toDoubleOrNull(),
                                age = ageText.toIntOrNull(),
                                activityLevel = activityLevel,
                                sex = goals.sex
                            )

                            val updatedSettings = appSettings.copy(
                                cupSize1Ml = cup1,
                                cupSize2Ml = cup2,
                                useStartDate = useStartDateLocal,
                                startDate = if (useStartDateLocal && startDateLocal != null) startDateLocal else null,
                                blockBeforeStartDate = blockBeforeStartLocal,
                                blockAfterToday = blockAfterTodayLocal
                            )

                            viewModel.saveGeneralSettings(updatedGoals, updatedSettings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_general_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save General",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // API Key test error dialog
    if (showTestErrorDialog && testError != null) {
        AlertDialog(
            onDismissRequest = {
                showTestErrorDialog = false
                viewModel.clearTestError()
            },
            title = { Text("API Key Test Failed", color = TextPrimary) },
            text = {
                Text(testError ?: "", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTestErrorDialog = false
                        viewModel.clearTestError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClassicRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTestErrorDialog = false
                    viewModel.clearTestError()
                }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = {
                showUnsavedDialog = false
            },
            title = { Text("Unsaved Changes", color = TextPrimary) },
            text = {
                Text("You have unsaved changes. Do you want to keep them or revert?", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        // Navigate away (keep changes)
                        onSaveComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Keep Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    hasUnsavedChanges = false
                    // Navigate away (revert changes)
                    onSaveComplete()
                }) {
                    Text("Revert", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun StartDatePickerDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    var year by remember(initialDate ?: now) { mutableStateOf((initialDate ?: now).year) }
    var month by remember(initialDate ?: now) { mutableStateOf((initialDate ?: now).monthValue) }
    var selectedDay by remember(initialDate ?: now) { mutableStateOf((initialDate ?: now).dayOfMonth) }

    val yearMonth = YearMonth.of(year, month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = (LocalDate.of(year, month, 1).dayOfWeek.value % 7)
    val totalCells = firstDayOffset + daysInMonth

    val monthName = LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Start Date",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.width(260.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (month == 1) {
                                month = 12
                                year -= 1
                            } else {
                                month -= 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "$monthName $year",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (month == 12) {
                                month = 1
                                year += 1
                            } else {
                                month += 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next month",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { wd ->
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = wd,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                )
                            }
                        }
                    }

                    items(totalCells, key = { it.toString() }) { idx ->
                        val day = idx - firstDayOffset + 1
                        val isValid = day in 1..daysInMonth
                        val isSelected = isValid && day == selectedDay

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ClassicRed else Color.Transparent)
                                .clickable(enabled = isValid) { selectedDay = day }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isValid) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LocalDate.of(year, month, selectedDay.coerceIn(1, daysInMonth)))
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProgressCyanBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color(0xFF13171F),
    unfocusedContainerColor = Color(0xFF13171F),
    focusedBorderColor = ProgressCyanBlue,
    unfocusedBorderColor = SurfaceBorder
)
