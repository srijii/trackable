package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.domain.NutritionCalculationEngine
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Full editor for a dashboard food-log entry. Unlike the old grams-only dialog
 * this can change the food name, brand, meal, quantity, unit and every macro.
 */
@Composable
fun EditFoodLogDialog(
    log: FoodLogEntity,
    matchedFood: FoodEntity?,
    onDismiss: () -> Unit,
    onSave: (FoodLogEntity) -> Unit
) {
    var name by remember(log.id) { mutableStateOf(log.foodNameSnapshot) }
    var brand by remember(log.id) { mutableStateOf(log.brandSnapshot) }
    var mealType by remember(log.id) { mutableStateOf(log.mealType) }
    var quantityText by remember(log.id) { mutableStateOf(formatNumber(log.quantity)) }
    var unit by remember(log.id) { mutableStateOf(log.servingUnit) }
    var caloriesText by remember(log.id) { mutableStateOf(formatDecimal(log.calories)) }
    var proteinText by remember(log.id) { mutableStateOf(formatDecimal(log.protein)) }
    var carbsText by remember(log.id) { mutableStateOf(formatDecimal(log.carbohydrates)) }
    var fatText by remember(log.id) { mutableStateOf(formatDecimal(log.fat)) }
    var fiberText by remember(log.id) { mutableStateOf(log.fiber?.let(::formatDecimal) ?: "") }
    var sugarText by remember(log.id) { mutableStateOf(log.sugar?.let(::formatDecimal) ?: "") }

    var mealMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val units = listOf("g", "ml", "scoop", "piece", "cup", "tbsp", "tsp", "slice", "serving")

    fun recalculateMacros(newQuantity: String, newUnit: String) {
        val food = matchedFood ?: return
        val quantity = newQuantity.toDoubleOrNull() ?: return
        if (quantity <= 0.0) return
        val calc = NutritionCalculationEngine.calculate(food, quantity, newUnit)
        caloriesText = formatDecimal(calc.calories)
        proteinText = formatDecimal(calc.protein)
        carbsText = formatDecimal(calc.carbohydrates)
        fatText = formatDecimal(calc.fat)
        fiberText = calc.fiber?.let(::formatDecimal) ?: ""
        sugarText = calc.sugar?.let(::formatDecimal) ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Edit log entry",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food item *") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_log_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_log_brand_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledDropdown(
                        label = "Meal",
                        value = mealType,
                        options = mealTypes,
                        expanded = mealMenuExpanded,
                        onExpandedChange = { mealMenuExpanded = it },
                        onSelect = { mealType = it; mealMenuExpanded = false },
                        modifier = Modifier.weight(1f)
                    )

                    LabeledDropdown(
                        label = "Unit",
                        value = unit,
                        options = units,
                        expanded = unitMenuExpanded,
                        onExpandedChange = { unitMenuExpanded = it },
                        onSelect = {
                            unit = it
                            unitMenuExpanded = false
                            recalculateMacros(quantityText, it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        recalculateMacros(it, unit)
                    },
                    label = { Text("Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_log_quantity_input"),
                    singleLine = true
                )

                Text(
                    text = "Nutrition for this entry",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories (kcal) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_log_calories_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroField("Protein", proteinText, { proteinText = it }, Modifier.weight(1f))
                    MacroField("Carbs", carbsText, { carbsText = it }, Modifier.weight(1f))
                    MacroField("Fat", fatText, { fatText = it }, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroField("Fiber", fiberText, { fiberText = it }, Modifier.weight(1f))
                    MacroField("Sugar", sugarText, { sugarText = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Food name is required."
                        return@Button
                    }
                    val quantity = quantityText.toDoubleOrNull()
                    val calories = caloriesText.toDoubleOrNull()
                    val protein = proteinText.toDoubleOrNull()
                    val carbs = carbsText.toDoubleOrNull()
                    val fat = fatText.toDoubleOrNull()

                    if (quantity == null || quantity <= 0.0 ||
                        calories == null || protein == null || carbs == null || fat == null
                    ) {
                        errorMessage = "Enter a valid quantity, calories and macros."
                        return@Button
                    }

                    onSave(
                        log.copy(
                            foodNameSnapshot = name.trim(),
                            brandSnapshot = brand.trim(),
                            mealType = mealType,
                            quantity = quantity,
                            servingUnit = unit,
                            calories = calories,
                            protein = protein,
                            carbohydrates = carbs,
                            fat = fat,
                            fiber = fiberText.toDoubleOrNull(),
                            sugar = sugarText.toDoubleOrNull()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClassicRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("save_edited_log_button")
            ) {
                Text("Save changes", fontWeight = FontWeight.SemiBold)
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
private fun MacroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true
    )
}

@Composable
private fun LabeledDropdown(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box {
            OutlinedButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = value,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(SurfaceDark).heightIn(max = 320.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (option == value) ClassicRed else TextPrimary
                            )
                        },
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)
}

private fun formatDecimal(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}
