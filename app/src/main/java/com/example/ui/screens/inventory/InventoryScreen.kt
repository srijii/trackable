package com.example.ui.screens.inventory

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.FoodEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.InventorySkeleton
import com.example.ui.components.ScannedFoodDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackableDivider
import com.example.ui.components.formatFoodQuantity
import com.example.ui.components.formatMacroValue
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var foodToEdit by remember { mutableStateOf<FoodEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var scannedFood by remember { mutableStateOf<FoodEntity?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen"),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ClassicRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_food")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Food")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search + scan. The page title itself comes from the shared header,
            // so there is no second oversized heading competing with it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            "Search foods or brands",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_food_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = ClassicRed.copy(alpha = 0.7f),
                        unfocusedBorderColor = SurfaceBorder.copy(alpha = 0.6f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .clickable { showScanDialog = true }
                        .testTag("scan_label_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan nutrition label",
                        tint = ClassicRed,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Category filters: quiet text tabs instead of a row of pills.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onCategorySelected(cat) }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) TextPrimary else TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isSelected) ClassicRed else Color.Transparent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "All foods",
                    supportingText = if (searchQuery.isBlank()) {
                        "${foods.size} item${if (foods.size == 1) "" else "s"}"
                    } else {
                        "${foods.size} result${if (foods.size == 1) "" else "s"}"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Crossfade(
                targetState = isLoaded,
                animationSpec = tween(220),
                label = "inventory_load",
                modifier = Modifier.weight(1f)
            ) { loaded ->
                if (!loaded) {
                    InventorySkeleton(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 88.dp
                        )
                    ) {
                        if (foods.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Restaurant,
                                    title = if (searchQuery.isBlank()) "No foods in your inventory" else "No matches",
                                    message = if (searchQuery.isBlank()) {
                                        "Add a food manually or scan a nutrition label to get started."
                                    } else {
                                        "Try a different name or category."
                                    }
                                )
                            }
                        } else {
                            items(foods, key = { it.id }) { food ->
                                InventoryFoodRow(
                                    food = food,
                                    onEdit = { foodToEdit = food },
                                    onDelete = { viewModel.deleteFood(food) },
                                    onLogToday = { qty -> viewModel.logFoodToToday(food, qty) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Food Dialog
    if (showAddDialog || foodToEdit != null) {
        val initialFood = foodToEdit
        FoodEditDialog(
            food = initialFood,
            onDismiss = {
                showAddDialog = false
                foodToEdit = null
            },
            onSave = { savedFood ->
                viewModel.saveFood(savedFood)
                showAddDialog = false
                foodToEdit = null
            }
        )
    }

    // Label Scanner Dialog
    if (showScanDialog) {
        NutritionLabelScannerDialog(
            onDismiss = { showScanDialog = false },
            onExtracted = { extractedFood ->
                showScanDialog = false
                scannedFood = extractedFood
            },
            scanLabel = viewModel::scanNutritionLabel
        )
    }

    // What the label scan found, shown as read-only information first.
    scannedFood?.let { extractedFood ->
        ScannedFoodDialog(
            food = extractedFood,
            onEdit = {
                scannedFood = null
                foodToEdit = extractedFood
            },
            onSave = {
                viewModel.saveFood(extractedFood)
                scannedFood = null
            },
            onDismiss = { scannedFood = null }
        )
    }
}

@Composable
fun InventoryFoodRow(
    food: FoodEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogToday: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = buildString {
                        if (food.brand.isNotBlank()) {
                            append(food.brand)
                            append(" · ")
                        }
                        append(food.category)
                        append(" · ")
                        append(formatFoodQuantity(food.servingSize))
                        append(" ")
                        append(food.servingUnit)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions for ${food.name}",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Log one serving", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = ClassicRed,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onLogToday(food.servingSize)
                            menuExpanded = false
                        },
                        modifier = Modifier.testTag("quick_log_inventory_${food.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Edit food", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        modifier = Modifier.testTag("edit_inventory_${food.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF4444)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        modifier = Modifier.testTag("delete_inventory_${food.id}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutrientCell(
                label = "Calories",
                value = "${food.calories.toInt()} kcal",
                modifier = Modifier.weight(1.25f)
            )
            NutrientCell(
                label = "Protein",
                value = "${formatMacroValue(food.protein)}g",
                modifier = Modifier.weight(1f)
            )
            NutrientCell(
                label = "Carbs",
                value = "${formatMacroValue(food.carbohydrates)}g",
                modifier = Modifier.weight(1f)
            )
            NutrientCell(
                label = "Fat",
                value = "${formatMacroValue(food.fat)}g",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        TrackableDivider(color = SurfaceBorder.copy(alpha = 0.45f))
    }
}

@Composable
private fun NutrientCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
    }
}

@Composable
fun FoodEditDialog(
    food: FoodEntity?,
    onDismiss: () -> Unit,
    onSave: (FoodEntity) -> Unit,
    titleOverride: String? = null,
    saveLabel: String = "Save food"
) {
    var name by remember { mutableStateOf(food?.name ?: "") }
    var brand by remember { mutableStateOf(food?.brand ?: "") }
    var category by remember { mutableStateOf(food?.category ?: "General") }
    var servingSizeText by remember { mutableStateOf(if (food != null) "${food.servingSize}" else "100") }
    var servingUnit by remember { mutableStateOf(food?.servingUnit ?: "g") }
    var caloriesText by remember { mutableStateOf(if (food != null) "${food.calories}" else "") }
    var proteinText by remember { mutableStateOf(if (food != null) "${food.protein}" else "") }
    var carbsText by remember { mutableStateOf(if (food != null) "${food.carbohydrates}" else "") }
    var fatText by remember { mutableStateOf(if (food != null) "${food.fat}" else "") }
    var fiberText by remember { mutableStateOf(food?.fiber?.toString() ?: "") }
    var sugarText by remember { mutableStateOf(food?.sugar?.toString() ?: "") }

    var unitMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val commonUnits = listOf("g", "ml", "scoop", "piece", "cup", "tbsp", "tsp", "slice", "serving")
    val categoryOptions = remember(category) {
        (FoodCategories.ALL + category).filter { it.isNotBlank() }.distinct()
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = titleOverride ?: if (food == null) "Add food" else "Edit food",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FormSectionLabel("Food")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    placeholder = { Text("e.g. Paneer, Whey protein", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth().testTag("food_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = foodFieldColors()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    placeholder = { Text("Optional", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth().testTag("food_brand_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = foodFieldColors()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    CompactDropdownButton(
                        label = "Category",
                        value = category,
                        expanded = categoryMenuExpanded,
                        onClick = { categoryMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_category_input")
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .heightIn(max = 360.dp)
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = if (option == category) ClassicRed else TextPrimary
                                    )
                                },
                                onClick = {
                                    category = option
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = servingSizeText,
                        onValueChange = { servingSizeText = it },
                        label = { Text("Serving *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_serving_size_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        CompactDropdownButton(
                            label = "Unit",
                            value = servingUnit,
                            expanded = unitMenuExpanded,
                            onClick = { unitMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            commonUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u, color = TextPrimary) },
                                    onClick = {
                                        servingUnit = u
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                FormSectionLabel("Nutrition per serving")

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories *") },
                    suffix = { Text("kcal", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("food_calories_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = foodFieldColors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein *") },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_protein_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs *") },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_carbs_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat *") },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_fat_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it },
                        label = { Text("Fiber") },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )
                    OutlinedTextField(
                        value = sugarText,
                        onValueChange = { sugarText = it },
                        label = { Text("Sugar") },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = foodFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Food name is required"
                        return@Button
                    }
                    val servSize = servingSizeText.toDoubleOrNull()
                    val cal = caloriesText.toDoubleOrNull()
                    val p = proteinText.toDoubleOrNull()
                    val c = carbsText.toDoubleOrNull()
                    val f = fatText.toDoubleOrNull()

                    if (servSize == null || cal == null || p == null || c == null || f == null) {
                        errorMessage = "Please enter valid numeric values for serving size, calories, and macros"
                        return@Button
                    }

                    val updated = (food ?: FoodEntity(name = name, calories = cal, protein = p, carbohydrates = c, fat = f)).copy(
                        name = name.trim(),
                        brand = brand.trim(),
                        category = if (category.isBlank()) "General" else category.trim(),
                        servingSize = servSize,
                        servingUnit = servingUnit,
                        calories = cal,
                        protein = p,
                        carbohydrates = c,
                        fat = f,
                        fiber = fiberText.toDoubleOrNull(),
                        sugar = sugarText.toDoubleOrNull()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ClassicRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_food_button")
            ) {
                Text(saveLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

/** Small uppercase-free label that separates the parts of the food form. */
@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/** A dropdown that matches the flat look of the food form. */
@Composable
private fun CompactDropdownButton(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCardHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = "Choose $label",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun foodFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = ClassicRed,
    unfocusedBorderColor = SurfaceBorder,
    focusedLabelColor = ClassicRed,
    unfocusedLabelColor = TextMuted,
    cursorColor = ClassicRed
)

/** Categories offered by the inventory food form. "Other" keeps custom items possible. */
object FoodCategories {
    val ALL = listOf(
        "Dairy",
        "Meat & Poultry",
        "Fish & Seafood",
        "Eggs",
        "Grains & Cereals",
        "Legumes & Pulses",
        "Vegetables",
        "Fruits",
        "Nuts & Seeds",
        "Oils & Fats",
        "Bakery",
        "Snacks",
        "Beverages",
        "Supplements",
        "Frozen Foods",
        "Condiments & Sauces",
        "Sweets & Desserts",
        "Fast Food",
        "General",
        "Other"
    )
}
