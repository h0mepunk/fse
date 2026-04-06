package com.example.fse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.data.repository.FoodRepository
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Serving
import com.example.fse.ui.AppDestination
import com.example.fse.ui.format.extractGrams
import com.example.fse.ui.format.formatAmountNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private fun closeSearch(navController: NavController, templateMode: Boolean, savedMealId: String? = null) {
    if (!navController.popBackStack()) {
        when {
            templateMode -> navController.navigate(AppDestination.Cookbook.route) {
                launchSingleTop = true
            }
            savedMealId != null -> navController.navigate(AppDestination.Meals.route) {
                launchSingleTop = true
            }
            else -> navController.navigate(AppDestination.Diary.route) {
                launchSingleTop = true
                popUpTo(AppDestination.Diary.route) { inclusive = false }
            }
        }
    }
}

@Composable
fun SearchFoodScreen(
    container: AppContainer,
    navController: NavController,
    diaryDate: LocalDate,
    templateId: Long? = null,
    /** When set (e.g. opened from diary meal +), serving picker pre-selects this meal type. */
    presetMealType: MealType? = null,
    /** Добавление строки в сохранённый приём FatSecret (вкладка Meals). */
    savedMealId: String? = null
) {
    val templateMode = templateId != null
    val savedMealMode = savedMealId != null
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<Result<List<Food>>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val recentFoods by container.foodRepository.getRecentFoods().collectAsState(initial = emptyList())
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            when {
                templateMode -> "Добавить в шаблон"
                savedMealMode -> "Добавить в сохранённый приём"
                else -> "Поиск еды"
            },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                isSearching = true
                scope.launch {
                    searchResult = withContext(Dispatchers.IO) { container.foodRepository.searchFoods(query) }
                    isSearching = false
                }
            })
        )

        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            searchResult?.fold(
                onSuccess = { foods ->
                    if (foods.isNotEmpty()) {
                        Text("Результаты", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(foods, key = { it.id }) { food ->
                                FoodSearchItem(
                                    food = food,
                                    container = container,
                                    diaryDate = diaryDate,
                                    templateId = templateId,
                                    presetMealType = presetMealType,
                                    savedMealId = savedMealId,
                                    onSelect = { closeSearch(navController, templateMode, savedMealId) }
                                )
                            }
                        }
                    }
                },
                onFailure = { Text("Ошибка: ${it.message}", color = MaterialTheme.colorScheme.error) }
            ) ?: run {
                Text("Недавние", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                if (recentFoods.isEmpty()) {
                    Text("Введите запрос выше.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentFoods, key = { it.id }) { food ->
                            FoodSearchItem(
                                food = food,
                                container = container,
                                diaryDate = diaryDate,
                                templateId = templateId,
                                presetMealType = presetMealType,
                                savedMealId = savedMealId,
                                onSelect = { closeSearch(navController, templateMode, savedMealId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodSearchItem(
    food: Food,
    container: AppContainer,
    diaryDate: LocalDate,
    templateId: Long?,
    presetMealType: MealType? = null,
    savedMealId: String? = null,
    onSelect: () -> Unit
) {
    var showServingPicker by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val forTemplate = templateId != null
    val savedMealMode = savedMealId != null

    LaunchedEffect(food.id) {
        isFavorite = withContext(Dispatchers.IO) { container.foodRepository.isFavorite(food.id) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showServingPicker = true }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                food.brandName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (food.servings.isNotEmpty()) {
                    val s = food.servings.first()
                    Text(
                        "${s.calories.toInt()} kcal • P:${s.protein.toInt()}g C:${s.carbs.toInt()}g F:${s.fat.toInt()}g / ${s.description}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!savedMealMode) {
                IconButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            if (container.foodRepository.isFavorite(food.id)) {
                                container.foodRepository.removeFavorite(food.id)
                            } else {
                                container.foodRepository.addFavorite(food)
                            }
                        }
                        isFavorite = !isFavorite
                    }
                }) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                }
            }
        }
    }

    if (showServingPicker && food.servings.isNotEmpty()) {
        ServingPickerBottomSheet(
            servings = food.servings,
            food = food,
            forTemplate = forTemplate,
            savedMealMode = savedMealMode,
            presetMealType = if (forTemplate || savedMealMode) null else presetMealType,
            onAdd = { serving, mult, mealType ->
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            val (finalFood, finalServing) = if (serving.calories == 0.0 && serving.protein == 0.0) {
                                container.foodRepository.getFood(food.id).getOrNull()?.let { fullFood ->
                                    val match = fullFood.servings.find { it.id == serving.id }
                                        ?: fullFood.servings.firstOrNull()
                                    if (match != null && (match.calories > 0 || match.protein > 0)) {
                                        fullFood to match
                                    } else fullFood to serving
                                } ?: (food to serving)
                            } else food to serving
                            when {
                                savedMealId != null -> container.savedMealsRepository.addFoodToSavedMeal(
                                    savedMealId,
                                    finalFood,
                                    finalServing,
                                    mult
                                )
                                templateId != null -> container.savedMealTemplateRepository.addLineFromSearch(
                                    templateId,
                                    finalFood,
                                    finalServing,
                                    mult
                                )
                                else -> container.foodRepository.addToDiary(
                                    FoodRepository.AddToDiaryRequest(
                                        date = diaryDate,
                                        mealType = mealType,
                                        food = finalFood,
                                        serving = finalServing,
                                        multiplier = mult
                                    )
                                )
                            }
                        }
                    }
                    if (result.isSuccess) {
                        showServingPicker = false
                        onSelect()
                    }
                }
            },
            onDismiss = { showServingPicker = false }
        )
    }
}

@Composable
private fun ServingPickerBottomSheet(
    servings: List<Serving>,
    food: Food,
    forTemplate: Boolean,
    savedMealMode: Boolean = false,
    presetMealType: MealType? = null,
    onAdd: (Serving, Double, MealType) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultServing = remember(servings) {
        servings.firstOrNull { extractGrams(it.description) != null } ?: servings.first()
    }
    var selectedServing by remember { mutableStateOf(defaultServing) }
    var selectedMealType by remember(forTemplate, savedMealMode, presetMealType) {
        val initial = if (forTemplate || savedMealMode) MealType.Breakfast else (presetMealType ?: MealType.Breakfast)
        mutableStateOf(initial)
    }
    var selectedMultText by remember { mutableStateOf("1") }
    val servingGrams = extractGrams(selectedServing.description)
    var gramsText by remember(selectedServing.id) {
        mutableStateOf(servingGrams?.let { formatAmountNumber(it) } ?: "")
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            when {
                forTemplate -> "В шаблон"
                savedMealMode -> "В сохранённый приём"
                else -> "В дневник"
            },
            style = MaterialTheme.typography.titleLarge
        )
        Text("${food.name} ${food.brandName?.let { "($it)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)

        if (!forTemplate && !savedMealMode) {
            Text("Приём пищи", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { mt ->
                    val selected = selectedMealType == mt
                    FilterChip(
                        selected = selected,
                        onClick = { selectedMealType = mt },
                        label = { Text(mt.displayName) }
                    )
                }
            }
        }

        Text("Порция", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        servings.forEach { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedServing = s }
                    .padding(8.dp)
            ) {
                Text(s.description)
                Text(" - ${s.calories.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(
            value = if (servingGrams != null) gramsText else selectedMultText,
            onValueChange = {
                if (servingGrams != null) {
                    gramsText = it
                } else {
                    selectedMultText = it
                }
            },
            label = { Text(if (servingGrams != null) "Граммы" else "Множитель") },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row {
            TextButton(onClick = onDismiss) { Text("Отмена") }
            Button(
                onClick = {
                    val multiplier = if (servingGrams != null && servingGrams > 0) {
                        val grams = gramsText.replace(',', '.').toDoubleOrNull() ?: servingGrams
                        (grams / servingGrams).coerceAtLeast(0.01)
                    } else {
                        selectedMultText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.01) ?: 1.0
                    }
                    onAdd(selectedServing, multiplier, selectedMealType)
                }
            ) { Text("Добавить") }
        }
    }
}
