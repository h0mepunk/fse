package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.SavedMeal
import com.example.fse.ui.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun MealsScreen(container: AppContainer, navController: androidx.navigation.NavController) {
    val meals by container.mealsRepository.getSavedMeals().collectAsState(initial = emptyList())
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMeal by remember { mutableStateOf<SavedMeal?>(null) }
    var selectedQuantityMode by remember { mutableStateOf(QuantityMode.Weight) }
    var quantityInput by remember { mutableStateOf("100") }
    var selectedMealType by remember { mutableStateOf(MealType.Other) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SnackbarHost(hostState = snackbarHostState)
        Text(
            "Приёмы пищи",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        errorMessage?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        if (meals.isEmpty()) {
            Text(
                "Подключите FatSecret в разделе Account, чтобы загрузить сохранённые приёмы пищи.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(meals, key = { it.id }) { meal ->
                    SavedMealCard(
                        meal = meal,
                        onAdd = { selectedMeal = meal }
                    )
                }
            }
        }
    }

    selectedMeal?.let { meal ->
        MealAddDialog(
            meal = meal,
            quantityMode = selectedQuantityMode,
            quantityInput = quantityInput,
            selectedMealType = selectedMealType,
            onQuantityModeChange = { mode ->
                selectedQuantityMode = mode
                quantityInput = if (mode == QuantityMode.Weight) "100" else "1"
            },
            onQuantityInputChange = { quantityInput = it },
            onMealTypeChange = { selectedMealType = it },
            onDismiss = { selectedMeal = null },
            onConfirm = {
                scope.launch {
                    errorMessage = null
                    val value = quantityInput.toDoubleOrNull()
                    if (value == null || value <= 0.0) {
                        errorMessage = "Введите корректное количество"
                        return@launch
                    }
                    val multiplier = if (selectedQuantityMode == QuantityMode.Weight) value / 100.0 else value
                    val result = withContext(Dispatchers.IO) {
                        container.foodRepository.addSavedMealToDiary(
                            meal = meal,
                            mealType = selectedMealType,
                            quantityMultiplier = multiplier
                        )
                    }
                    result.onSuccess {
                        selectedMeal = null
                        container.foodRepository.forceDiaryRefresh()
                        snackbarHostState.showSnackbar("Добавлено")
                        navController.navigate(AppDestination.Diary.route)
                    }.onFailure { errorMessage = it.message }
                }
            }
        )
    }
}

@Composable
private fun SavedMealCard(meal: SavedMeal, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                meal.name,
                style = MaterialTheme.typography.titleMedium
            )
            meal.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (meal.mealTypes.isNotEmpty()) {
                Text(
                    meal.mealTypes.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (meal.items.isNotEmpty()) {
                Text(
                    "${meal.items.size} продуктов",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                meal.items.take(3).forEach { item ->
                    Text(
                        "• ${item.name} (×${item.numberOfUnits})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
                if (meal.items.size > 3) {
                    Text(
                        "  ...и ещё ${meal.items.size - 3}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Добавить")
            }
        }
    }
}

private enum class QuantityMode(val title: String) {
    Weight("Вес, г"),
    Portions("Порции")
}

@Composable
private fun MealAddDialog(
    meal: SavedMeal,
    quantityMode: QuantityMode,
    quantityInput: String,
    selectedMealType: MealType,
    onQuantityModeChange: (QuantityMode) -> Unit,
    onQuantityInputChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ${meal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Количество")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuantityMode.entries.forEach { mode ->
                        FilterChip(
                            selected = quantityMode == mode,
                            onClick = { onQuantityModeChange(mode) },
                            label = { Text(mode.title) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityInputChange,
                    label = { Text(if (quantityMode == QuantityMode.Weight) "Вес (г)" else "Порции") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Прием пищи")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { mt ->
                        FilterChip(
                            selected = selectedMealType == mt,
                            onClick = { onMealTypeChange(mt) },
                            label = { Text(mt.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
