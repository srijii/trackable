package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ai.NutritionAiParseResult
import com.example.ai.ParsedItemCandidate
import com.example.data.database.FoodEntity
import com.example.domain.NutritionCalculationEngine
import com.example.ui.theme.CarbsOrange
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.FatPink
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.ProteinPurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.theme.WaterCyan

@Composable
fun AiConfirmationDialog(
    parseResult: NutritionAiParseResult,
    inventory: List<FoodEntity>,
    onConfirm: (confirmedItems: List<ParsedItemCandidate>, waterMl: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mutable copy of parsed items so user can edit quantity or resolve ambiguity
    val editableItems = remember {
        mutableStateListOf<ParsedItemCandidate>().apply {
            addAll(parseResult.items)
        }
    }
    var currentWaterMl by remember { mutableStateOf(parseResult.waterMl) }

    val totalCalories = editableItems.sumOf { it.calculatedCalories }
    val totalProtein = editableItems.sumOf { it.calculatedProtein }
    val totalCarbs = editableItems.sumOf { it.calculatedCarbs }
    val totalFat = editableItems.sumOf { it.calculatedFat }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_confirmation_dialog"),
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Parsed",
                        tint = ClassicRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review & confirm",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = parseResult.providerUsed,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Text(
                    text = "Verify the identified foods, quantities, and macronutrient calculations below before saving:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Water notice if detected
                if (currentWaterMl > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Water",
                                tint = WaterCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+$currentWaterMl ml water will be logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = WaterCyan
                            )
                        }
                        IconButton(
                            onClick = { currentWaterMl = 0 },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove water",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (editableItems.isEmpty() && currentWaterMl == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items to log. Try typing what you ate again.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(editableItems) { index, item ->
                            ParsedItemEditorCard(
                                item = item,
                                inventory = inventory,
                                onUpdateItem = { updated ->
                                    editableItems[index] = updated
                                },
                                onRemoveItem = {
                                    editableItems.removeAt(index)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Meal totals
                if (editableItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meal total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "${totalCalories.toInt()} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "P ${Math.round(totalProtein * 10.0) / 10.0}g",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "C ${Math.round(totalCarbs * 10.0) / 10.0}g",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "F ${Math.round(totalFat * 10.0) / 10.0}g",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(editableItems, currentWaterMl)
                },
                enabled = editableItems.any { it.matchedFood != null } || currentWaterMl > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClassicRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_log_all_button")
            ) {
                Text("Confirm & log", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_ai_confirmation")
            ) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun ParsedItemEditorCard(
    item: ParsedItemCandidate,
    inventory: List<FoodEntity>,
    onUpdateItem: (ParsedItemCandidate) -> Unit,
    onRemoveItem: () -> Unit
) {
    var quantityText by remember { mutableStateOf(if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}") }
    var showMatchDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
            // Header: Food name + remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.matchedFood?.name ?: item.rawName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (item.matchedFood != null && item.matchedFood.brand.isNotBlank()) {
                        Text(
                            text = "Brand: ${item.matchedFood.brand}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onRemoveItem,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Ambiguity Resolution Picker
            if (item.isAmbiguous && item.candidateFoods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2C2210))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Ambiguous",
                                tint = WarningYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Which product did you have?",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Candidate choices
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item.candidateFoods.forEach { candidate ->
                                val isSelected = item.matchedFood?.id == candidate.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) ClassicRed else SurfaceCardHigh)
                                        .clickable {
                                            val calc = NutritionCalculationEngine.calculate(
                                                food = candidate,
                                                consumedQuantity = item.quantity,
                                                consumedUnit = item.servingUnit
                                            )
                                            onUpdateItem(
                                                item.copy(
                                                    matchedFood = candidate,
                                                    isAmbiguous = false,
                                                    calculatedCalories = calc.calories,
                                                    calculatedProtein = calc.protein,
                                                    calculatedCarbs = calc.carbohydrates,
                                                    calculatedFat = calc.fat
                                                )
                                            )
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${candidate.name} (${candidate.brand})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Unmatched food resolution: Match from inventory
            if (item.isUnmatched) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2C1616))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Unmatched",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Not found in your inventory",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showMatchDropdown = true },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Select from Inventory (${inventory.size} items)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMatchDropdown,
                                onDismissRequest = { showMatchDropdown = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                inventory.forEach { invFood ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${invFood.name} (${invFood.brand}) - ${invFood.calories.toInt()} kcal/${invFood.servingSize}${invFood.servingUnit}", color = TextPrimary)
                                        },
                                        onClick = {
                                            showMatchDropdown = false
                                            val calc = NutritionCalculationEngine.calculate(
                                                food = invFood,
                                                consumedQuantity = item.quantity,
                                                consumedUnit = item.servingUnit
                                            )
                                            onUpdateItem(
                                                item.copy(
                                                    matchedFood = invFood,
                                                    isUnmatched = false,
                                                    isAmbiguous = false,
                                                    calculatedCalories = calc.calories,
                                                    calculatedProtein = calc.protein,
                                                    calculatedCarbs = calc.carbohydrates,
                                                    calculatedFat = calc.fat
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity & Unit Editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { newVal ->
                        quantityText = newVal
                        val q = newVal.toDoubleOrNull() ?: 1.0
                        if (item.matchedFood != null) {
                            val calc = NutritionCalculationEngine.calculate(
                                food = item.matchedFood,
                                consumedQuantity = q,
                                consumedUnit = item.servingUnit
                            )
                            onUpdateItem(
                                item.copy(
                                    quantity = q,
                                    calculatedCalories = calc.calories,
                                    calculatedProtein = calc.protein,
                                    calculatedCarbs = calc.carbohydrates,
                                    calculatedFat = calc.fat
                                )
                            )
                        } else {
                            onUpdateItem(item.copy(quantity = q))
                        }
                    },
                    label = { Text("Quantity", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = ClassicRed,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardHigh)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.servingUnit,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Calculated macros snapshot for this item
            if (item.matchedFood != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.calculatedCalories.toInt()} kcal",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "P: ${item.calculatedProtein}g • C: ${item.calculatedCarbs}g • F: ${item.calculatedFat}g",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
