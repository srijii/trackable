package com.example.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FoodEntity
import com.example.data.database.FoodLogEntity
import com.example.data.repository.NutritionRepository
import com.example.domain.NutritionCalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val foods: StateFlow<List<FoodEntity>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchFoods(query)
        }
        .combine(_selectedCategory) { list, cat ->
            if (cat == null || cat == "All") {
                list
            } else {
                list.filter { it.category.equals(cat, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<String>> = repository.allFoodsFlow
        .combine(MutableStateFlow(Unit)) { foodsList, _ ->
            val set = foodsList.map { it.category }.filter { it.isNotBlank() }.toSortedSet()
            listOf("All") + set
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("All")
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(cat: String) {
        _selectedCategory.value = cat
    }

    fun saveFood(food: FoodEntity) {
        viewModelScope.launch {
            if (food.id == 0L) {
                repository.insertFood(food)
                _message.value = "Added '${food.name}' to inventory"
            } else {
                repository.updateFood(food)
                _message.value = "Updated '${food.name}'"
            }
        }
    }

    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            repository.deleteFood(food)
            _message.value = "Deleted '${food.name}'"
        }
    }

    fun logFoodToToday(food: FoodEntity, quantity: Double) {
        viewModelScope.launch {
            val calc = NutritionCalculationEngine.calculate(food, quantity, food.servingUnit)
            val log = FoodLogEntity(
                foodId = food.id,
                foodNameSnapshot = food.name,
                brandSnapshot = food.brand,
                quantity = quantity,
                servingUnit = food.servingUnit,
                calories = calc.calories,
                protein = calc.protein,
                carbohydrates = calc.carbohydrates,
                fat = calc.fat,
                fiber = calc.fiber,
                sugar = calc.sugar,
                dateString = LocalDate.now().toString(),
                mealType = "Snack"
            )
            repository.insertFoodLog(log)
            _message.value = "Logged ${food.name} to Today!"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
