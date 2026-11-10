package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.domain.NutritionCalculationEngine
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Everything about one logged meal entry: which food, how much of it was
 * eaten at that moment and what it added up to.
 *
 * The dialog opens in read-only mode so the entry can be read at a glance.
 * The edit action turns the same information into fields for *this entry
 * only* — the food in the inventory is never touched.
 */
@Composable
fun MealEntryDialog(
    log: FoodLogEntity,
    onSave: (FoodLogEntity) -> Unit,
    onDismiss: () -> Unit,
    matchedFood: FoodEntity? = null,
    onDelete: (() -> Unit)? = null
) {
    var editing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var nameText by remember { mutableStateOf(log.foodNameSnapshot) }
    var brandText by remember { mutableStateOf(log.brandSnapshot) }
    var mealType by remember { mutableStateOf(log.mealType) }
    var quantityText by remember { mutableStateOf(formatFoodQuantity(log.quantity)) }
    var unit by remember { mutableStateOf(log.servingUnit) }
    var caloriesText by remember { mutableStateOf(formatDecimalValue(log.calories)) }
    var proteinText by remember { mutableStateOf(formatDecimalValue(log.protein)) }
    var carbsText by remember { mutableStateOf(formatDecimalValue(log.carbohydrates)) }
    var fatText by remember { mutableStateOf(formatDecimalValue(log.fat)) }
    var fiberText by remember { mutableStateOf(log.fiber?.let(::formatDecimalValue) ?: "") }
    var sugarText by remember { mutableStateOf(log.sugar?.let(::formatDecimalValue) ?: "") }

    var mealMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val time = remember(log.timestamp) {
        Instant.ofEpochMilli(log.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val units = listOf("g", "ml", "scoop", "piece", "cup", "tbsp", "tsp", "slice", "serving")

    // Changing the amount updates the numbers from the inventory food, so the
    // entry always matches what was really eaten.
    fun recalculate(newQuantity: String, newUnit: String) {
        val food = matchedFood ?: return
        val quantity = newQuantity.toDoubleOrNull() ?: return
        if (quantity <= 0.0) return
        val calc = NutritionCalculationEngine.calculate(food, quantity, newUnit)
        caloriesText = formatDecimalValue(calc.calories)
        proteinText = formatDecimalValue(calc.protein)
        carbsText = formatDecimalValue(calc.carbohydrates)
        fatText = formatDecimalValue(calc.fat)
        fiberText = calc.fiber?.let(::formatDecimalValue) ?: ""
        sugarText = calc.sugar?.let(::formatDecimalValue) ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.testTag("meal_entry_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ClassicRed.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = ClassicRed,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editing) "Edit meal entry" else "Meal entry",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Logged at $time",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = {
                        if (editing) {
                            editing = false
                            errorMessage = null
                        } else {
                            editing = true
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("meal_entry_edit_button")
                ) {
                    Icon(
                        imageVector = if (editing) Icons.Default.ArrowDropUp else Icons.Default.Edit,
                        contentDescription = if (editing) "Stop editing" else "Edit this entry",
                        tint = if (editing) ClassicRed else TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("meal_entry_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete this entry",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
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

                if (!editing) {
                    // --- Read-only view of this entry ---
                    Text(
                        text = log.foodNameSnapshot,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (log.brandSnapshot.isNotBlank()) {
                        Text(
                            text = log.brandSnapshot,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TrackableDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MetricBlock(
                            label = "Quantity",
                            value = "${formatFoodQuantity(log.quantity)} ${log.servingUnit}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBlock(
                            label = "Meal",
                            value = log.mealType,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TrackableDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${log.calories.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MacroFigure("Protein", log.protein, Modifier.weight(1f))
                        MacroFigure("Carbs", log.carbohydrates, Modifier.weight(1f))
                        MacroFigure("Fat", log.fat, Modifier.weight(1f))
                    }

                    if (log.fiber != null || log.sugar != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (log.fiber != null) {
                                MacroFigure("Fiber", log.fiber, Modifier.weight(1f))
                            }
                            if (log.sugar != null) {
                                MacroFigure("Sugar", log.sugar, Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    // --- Edit fields for this entry ---
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Food item *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_entry_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = entryFieldColors()
                    )

                    OutlinedTextField(
                        value = brandText,
                        onValueChange = { brandText = it },
                        label = { Text("Brand") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_entry_brand_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = entryFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                recalculate(it, unit)
                            },
                            label = { Text("Quantity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meal_entry_quantity_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = entryFieldColors()
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            EntryDropdownButton(
                                label = "Unit",
                                value = unit,
                                expanded = unitMenuExpanded,
                                onClick = { unitMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                units.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = TextPrimary) },
                                        onClick = {
                                            unit = option
                                            unitMenuExpanded = false
                                            recalculate(quantityText, option)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        EntryDropdownButton(
                            label = "Meal",
                            value = mealType,
                            expanded = mealMenuExpanded,
                            onClick = { mealMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meal_entry_meal_input")
                        )
                        DropdownMenu(
                            expanded = mealMenuExpanded,
                            onDismissRequest = { mealMenuExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            mealTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = TextPrimary) },
                                    onClick = {
                                        mealType = option
                                        mealMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Calories *") },
                        suffix = {
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_entry_calories_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = entryFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EntryNumberField("Protein", proteinText, { proteinText = it }, Modifier.weight(1f))
                        EntryNumberField("Carbs", carbsText, { carbsText = it }, Modifier.weight(1f))
                        EntryNumberField("Fat", fatText, { fatText = it }, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EntryNumberField("Fiber", fiberText, { fiberText = it }, Modifier.weight(1f))
                        EntryNumberField("Sugar", sugarText, { sugarText = it }, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Changes apply to this entry only — the food in your inventory stays as it is.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            if (editing) {
                Button(
                    onClick = {
                        val quantity = quantityText.toDoubleOrNull()
                        val calories = caloriesText.toDoubleOrNull()
                        val protein = proteinText.toDoubleOrNull()
                        val carbs = carbsText.toDoubleOrNull()
                        val fat = fatText.toDoubleOrNull()

                        if (nameText.isBlank()) {
                            errorMessage = "Food name is required."
                            return@Button
                        }
                        if (quantity == null || quantity <= 0.0 ||
                            calories == null || protein == null || carbs == null || fat == null
                        ) {
                            errorMessage = "Enter a valid quantity, calories and macros."
                            return@Button
                        }

                        onSave(
                            log.copy(
                                foodNameSnapshot = nameText.trim(),
                                brandSnapshot = brandText.trim(),
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("meal_entry_save_button")
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { editing = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClassicRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("meal_entry_edit_action")
                ) {
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (editing) "Cancel" else "Close", color = TextMuted)
            }
        }
    )
}

@Composable
private fun EntryNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text("g", style = MaterialTheme.typography.labelSmall, color = TextMuted) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = entryFieldColors()
    )
}

@Composable
private fun EntryDropdownButton(
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
private fun entryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = ClassicRed,
    unfocusedBorderColor = com.example.ui.theme.SurfaceBorder,
    focusedLabelColor = ClassicRed,
    unfocusedLabelColor = TextMuted,
    cursorColor = ClassicRed
)

@Composable
private fun MacroFigure(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = "${formatMacroValue(value)}g",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
    }
}

/** Keeps the same numeric formatting the log rows use. */
private fun formatDecimalValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", value)
