package com.example.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CarbsOrange
import com.example.ui.theme.FatPink
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProteinPurple
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

    val snackbarHostState = remember { SnackbarHostState() }
    var foodToEdit by remember { mutableStateOf<FoodEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.Black,
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FOOD INVENTORY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = "My Available Foods",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Scan Nutrition Label Button
                OutlinedButton(
                    onClick = { showScanDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("scan_label_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan label",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Label", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search by food name or brand...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_food_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    singleLine = true
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryGreen else SurfaceDark)
                            .border(1.dp, if (isSelected) PrimaryGreen else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.onCategorySelected(cat) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Food Inventory List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (foods.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No foods found matching \"$searchQuery\"",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(foods, key = { it.id }) { food ->
                        InventoryFoodItemCard(
                            food = food,
                            onEdit = { foodToEdit = food },
                            onDelete = { viewModel.deleteFood(food) },
                            onLogToday = { qty -> viewModel.logFoodToToday(food, qty) }
                        )
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
                foodToEdit = extractedFood
            }
        )
    }
}

@Composable
fun InventoryFoodItemCard(
    food: FoodEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogToday: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_item_${food.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (food.brand.isNotBlank()) {
                            Text(
                                text = food.brand,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(" • ", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "${food.category} • Serving: ${if (food.servingSize % 1.0 == 0.0) food.servingSize.toInt() else food.servingSize} ${food.servingUnit}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLogToday(food.servingSize) },
                        modifier = Modifier.size(36.dp).testTag("quick_log_inventory_${food.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log to today",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_inventory_${food.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit food",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_inventory_${food.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete food",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nutritional Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniNutrientBadge(
                    label = "Calories",
                    value = "${food.calories.toInt()} kcal",
                    color = PrimaryGreen,
                    modifier = Modifier.weight(1.2f)
                )
                MiniNutrientBadge(
                    label = "Protein",
                    value = "${food.protein}g",
                    color = ProteinPurple,
                    modifier = Modifier.weight(1f)
                )
                MiniNutrientBadge(
                    label = "Carbs",
                    value = "${food.carbohydrates}g",
                    color = CarbsOrange,
                    modifier = Modifier.weight(1f)
                )
                MiniNutrientBadge(
                    label = "Fat",
                    value = "${food.fat}g",
                    color = FatPink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniNutrientBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222732))
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 11.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
fun FoodEditDialog(
    food: FoodEntity?,
    onDismiss: () -> Unit,
    onSave: (FoodEntity) -> Unit
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
    val commonUnits = listOf("g", "ml", "scoop", "piece", "cup", "tbsp", "slice")

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = if (food == null) "Add Food to Inventory" else "Edit Food",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let {
                    Text(it, color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    placeholder = { Text("e.g. Whey Protein, Paneer, Rice") },
                    modifier = Modifier.fillMaxWidth().testTag("food_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Product (e.g. Brand A, Fresh Dairy)") },
                    modifier = Modifier.fillMaxWidth().testTag("food_brand_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Dairy, Supplements, Grains)") },
                    modifier = Modifier.fillMaxWidth().testTag("food_category_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = servingSizeText,
                        onValueChange = { servingSizeText = it },
                        label = { Text("Serving Size *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f).testTag("food_serving_size_input"),
                        singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        OutlinedButton(
                            onClick = { unitMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(servingUnit, style = MaterialTheme.typography.bodyMedium)
                        }

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

                Text(
                    text = "NUTRITION PER SERVING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories (kcal) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("food_calories_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_protein_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_carbs_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("food_fat_input"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sugarText,
                        onValueChange = { sugarText = it },
                        label = { Text("Sugar (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                modifier = Modifier.testTag("save_food_button")
            ) {
                Text("Save to Inventory", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun NutritionLabelScannerDialog(
    onDismiss: () -> Unit,
    onExtracted: (FoodEntity) -> Unit
) {
    // Label scanner with presets or manual snapshot
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nutrition Label Scanner", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Position camera at the nutrition facts table. You can also select a common scanned product template below:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                // Presets from scanning
                val scanPresets = listOf(
                    FoodEntity(
                        name = "Optimum Nutrition Whey",
                        brand = "ON Gold Standard",
                        category = "Supplements",
                        servingSize = 1.0,
                        servingUnit = "scoop",
                        calories = 120.0,
                        protein = 24.0,
                        carbohydrates = 3.0,
                        fat = 1.5,
                        fiber = 0.0,
                        sugar = 1.0
                    ),
                    FoodEntity(
                        name = "Amul Malai Paneer",
                        brand = "Amul",
                        category = "Dairy",
                        servingSize = 100.0,
                        servingUnit = "g",
                        calories = 295.0,
                        protein = 18.0,
                        carbohydrates = 4.0,
                        fat = 23.0
                    ),
                    FoodEntity(
                        name = "Almond Butter",
                        brand = "Kirkland",
                        category = "Spreads",
                        servingSize = 32.0,
                        servingUnit = "g",
                        calories = 210.0,
                        protein = 6.0,
                        carbohydrates = 6.0,
                        fat = 18.0,
                        fiber = 3.0,
                        sugar = 1.0
                    )
                )

                scanPresets.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExtracted(preset) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13161C)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(preset.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${preset.brand} • ${preset.calories.toInt()} kcal (${preset.protein}g P)", color = TextSecondary, fontSize = 11.sp)
                            }
                            Text("Use Data", color = PrimaryGreen, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Default scan template
                    onExtracted(
                        FoodEntity(
                            name = "Scanned Nutrition Product",
                            brand = "Product Label",
                            category = "Scanned",
                            servingSize = 100.0,
                            servingUnit = "g",
                            calories = 250.0,
                            protein = 15.0,
                            carbohydrates = 20.0,
                            fat = 10.0
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black)
            ) {
                Text("Simulate Camera OCR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
