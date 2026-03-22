package com.example.fse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Serving
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SearchFoodScreen(
    container: AppContainer,
    navController: NavController,
    initialMealType: MealType = MealType.Breakfast
) {
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<Result<List<Food>>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val recentFoods by container.foodRepository
        .getRecentFoodsForMealType(initialMealType, LocalDate.now())
        .collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val onAddCompleted: () -> Unit = {
        container.foodRepository.forceDiaryRefresh()
        navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SnackbarHost(hostState = snackbarHostState)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search food") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                isSearching = true
                scope.launch {
                    searchResult = withContext(Dispatchers.IO) {
                        container.foodRepository.searchFoodsWithDiaryPriority(query)
                    }
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
                        Text(
                            "Search results (top 10 from your diary first)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(foods, key = { it.id }) { food ->
                                FoodSearchItem(
                                    food = food,
                                    container = container,
                                    onSelect = onAddCompleted,
                                    snackbarHostState = snackbarHostState,
                                    initialMealType = initialMealType
                                )
                            }
                        }
                    }
                },
                onFailure = { Text("Error: ${it.message}", color = MaterialTheme.colorScheme.error) }
            ) ?: run {
                Text("Recent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                if (recentFoods.isEmpty()) {
                    Text("Search for food above or add from diary.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentFoods, key = { it.id }) { food ->
                            FoodSearchItem(
                                food = food,
                                container = container,
                                onSelect = onAddCompleted,
                                snackbarHostState = snackbarHostState,
                                initialMealType = initialMealType
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
    onSelect: () -> Unit,
    snackbarHostState: SnackbarHostState,
    initialMealType: MealType
) {
    var showServingPicker by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(food.id) {
        isFavorite = kotlinx.coroutines.withContext(Dispatchers.IO) { container.foodRepository.isFavorite(food.id) }
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
            IconButton(onClick = {
                scope.launch {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
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

    if (showServingPicker && food.servings.isNotEmpty()) {
        ServingPickerBottomSheet(
            servings = food.servings,
            food = food,
            onAdd = { serving, mult, mealType ->
                scope.launch {
                    val result = runCatching {
                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                            val (finalFood, finalServing) = if (serving.calories == 0.0 && serving.protein == 0.0) {
                                container.foodRepository.getFood(food.id).getOrNull()?.let { fullFood ->
                                    val match = fullFood.servings.find { it.id == serving.id }
                                        ?: fullFood.servings.firstOrNull()
                                    if (match != null && (match.calories > 0 || match.protein > 0)) {
                                        fullFood to match
                                    } else fullFood to serving
                                } ?: (food to serving)
                            } else food to serving
                            container.foodRepository.addToDiary(
                                com.example.fse.data.repository.FoodRepository.AddToDiaryRequest(
                                    date = LocalDate.now(),
                                    mealType = mealType,
                                    food = finalFood,
                                    serving = finalServing,
                                    multiplier = mult
                                )
                            )
                        }
                    }
                    result.onSuccess {
                        showServingPicker = false
                        onSelect()
                    }
                    if (result.isFailure) {
                        val error = result.exceptionOrNull()
                        snackbarHostState.showSnackbar(error?.message ?: "Failed to add food")
                    }
                }
            },
            initialMealType = initialMealType,
            onDismiss = { showServingPicker = false }
        )
    }
}

@Composable
private fun ServingPickerBottomSheet(
    servings: List<Serving>,
    food: Food,
    onAdd: (Serving, Double, MealType) -> Unit,
    initialMealType: MealType,
    onDismiss: () -> Unit
) {
    var selectedMult by remember { mutableStateOf(1.0) }
    var selectedServing by remember { mutableStateOf(servings.first()) }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add to diary", style = MaterialTheme.typography.titleLarge)
        Text("${food.name} ${food.brandName?.let { "($it)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)

        Text("Meal", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { mt ->
                val selected = selectedMealType == mt
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { selectedMealType = mt },
                    label = { Text(mt.displayName) }
                )
            }
        }

        Text("Serving", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        servings.forEach { s ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedServing = s }
                .padding(8.dp)
            ) {
                Text(s.description)
                Text(" - ${s.calories.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(
            value = selectedMult.toString(),
            onValueChange = { selectedMult = it.toDoubleOrNull() ?: 1.0 },
            label = { Text("Multiplier") },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
            androidx.compose.material3.Button(
                onClick = { onAdd(selectedServing, selectedMult, selectedMealType) }
            ) { Text("Add") }
        }
    }
}
