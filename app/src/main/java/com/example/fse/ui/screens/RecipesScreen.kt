package com.example.fse.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.ProfileSavedMeal
import com.example.fse.domain.model.ProfileSavedMealItem
import com.example.fse.domain.model.Recipe
import com.example.fse.domain.model.Serving
import com.example.fse.ui.AppDestination
import com.example.fse.ui.format.extractGrams
import com.example.fse.ui.format.formatAmountNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsTabScreen(container: AppContainer, navController: NavController) {
    var query by remember { mutableStateOf("") }
    val lastQuery by container.recipeRepository.getLastQuery().collectAsState(initial = "")
    val savedMeals by container.savedMealsRepository.getSavedMeals().collectAsState(initial = emptyList())
    val favoriteRecipes by container.recipeRepository.getUserFavoriteRecipes().collectAsState(initial = emptyList())
    val favoriteRecipeIds = remember(favoriteRecipes) { favoriteRecipes.map { it.id }.toSet() }
    val hasOAuth by container.oauth1TokenStore.hasTokens.collectAsState(initial = false)
    var globalSearchOpen by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var userSearched by remember { mutableStateOf(false) }
    val savedMealsRefreshing by container.savedMealsRepository.isRefreshing.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    var recipeDetailId by remember { mutableStateOf<Long?>(null) }
    var loadedDetail by remember { mutableStateOf<Recipe?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    val recipeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var mealPickForDiary by remember { mutableStateOf<ProfileSavedMeal?>(null) }

    LaunchedEffect(hasOAuth) {
        if (hasOAuth) {
            container.savedMealsRepository.triggerRefresh()
        }
    }

    LaunchedEffect(globalSearchOpen) {
        if (globalSearchOpen) {
            query = lastQuery
            if (lastQuery.isNotBlank()) {
                isSearching = true
                error = null
                withContext(Dispatchers.IO) {
                    container.recipeRepository.searchRecipes(lastQuery)
                        .onSuccess {
                            searchResults = it
                            userSearched = true
                        }
                        .onFailure { error = it.message }
                }
                isSearching = false
            }
        } else {
            userSearched = false
            searchResults = emptyList()
            query = ""
            error = null
        }
    }

    LaunchedEffect(recipeDetailId) {
        val id = recipeDetailId ?: run {
            loadedDetail = null
            detailError = null
            detailLoading = false
            return@LaunchedEffect
        }
        detailLoading = true
        detailError = null
        loadedDetail = null
        container.recipeRepository.getRecipeById(id)
            .onSuccess { loadedDetail = it }
            .onFailure { detailError = it.message }
        detailLoading = false
    }

    val displaySearchRecipes = if (globalSearchOpen) {
        when {
            userSearched -> searchResults
            else -> emptyList()
        }
    } else {
        emptyList()
    }
    val showSearchNoResults = globalSearchOpen && userSearched && searchResults.isEmpty() && !isSearching

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = !globalSearchOpen && savedMealsRefreshing,
            onRefresh = {
                scope.launch {
                    if (!globalSearchOpen) {
                        container.savedMealsRepository.refreshSavedMealsAndAwait()
                    }
                }
            },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize(),
            enabled = !globalSearchOpen
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (globalSearchOpen) "Поиск рецептов" else "Сохранённые приёмы",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (globalSearchOpen) {
                            IconButton(onClick = { globalSearchOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                            }
                        } else {
                            IconButton(onClick = { globalSearchOpen = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск в каталоге")
                            }
                        }
                    }
                }

                if (globalSearchOpen) {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                if (it.isBlank()) {
                                    userSearched = false
                                    searchResults = emptyList()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            label = { Text("Поиск рецептов") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                if (query.isBlank()) {
                                    userSearched = false
                                    searchResults = emptyList()
                                    return@KeyboardActions
                                }
                                userSearched = true
                                isSearching = true
                                error = null
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        container.recipeRepository.searchRecipes(query)
                                            .onSuccess { searchResults = it }
                                            .onFailure { error = it.message }
                                    }
                                    isSearching = false
                                }
                            })
                        )
                    }
                }

                when {
                    isSearching -> item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    error != null -> item {
                        Text(
                            "Ошибка: $error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    showSearchNoResults -> item {
                        Text(
                            "Нет результатов для «$query»",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    !globalSearchOpen && !hasOAuth -> item {
                        Text(
                            "Подключите FatSecret в Account — подтянем сохранённые приёмы пищи из профиля. Потяните вниз для обновления после входа.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    !globalSearchOpen && hasOAuth && savedMeals.isEmpty() && !isSearching -> item {
                        Text(
                            "В профиле FatSecret нет сохранённых приёмов. Создайте их на fatsecret.com или в приложении FatSecret, затем обновите список.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    globalSearchOpen && !userSearched && !isSearching -> item {
                        Text(
                            "Введите запрос и нажмите поиск на клавиатуре.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    globalSearchOpen -> items(displaySearchRecipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            isInFavorites = recipe.id in favoriteRecipeIds,
                            canAddToFavorites = hasOAuth && recipe.id !in favoriteRecipeIds,
                            onAddToFavorites = {
                                scope.launch {
                                    container.recipeRepository.addRecipeToFavorites(recipe.id)
                                        .onFailure { error = it.message }
                                }
                            },
                            onOpenDetail = { recipeDetailId = recipe.id }
                        )
                    }
                    else -> items(savedMeals, key = { it.id }) { meal ->
                        ProfileSavedMealCard(
                            meal = meal,
                            onAddToDiary = { mealPickForDiary = meal }
                        )
                    }
                }
            }
        }
    }

    mealPickForDiary?.let { meal ->
        Dialog(onDismissRequest = { mealPickForDiary = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Добавить «${meal.name}» в дневник",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Выберите приём дня (как в FatSecret API: перекус идёт в «другое»).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val labels = listOf(
                        MealType.Breakfast to "Завтрак",
                        MealType.Lunch to "Обед",
                        MealType.Dinner to "Ужин",
                        MealType.Snack to "Перекус",
                        MealType.Other to "Другое"
                    )
                    labels.forEach { (mt, label) ->
                        TextButton(
                            onClick = {
                                val id = meal.id.toLongOrNull()
                                if (id == null) {
                                    error = "Некорректный id приёма"
                                    mealPickForDiary = null
                                    return@TextButton
                                }
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            container.foodRepository.copySavedMealToDiary(
                                                savedMealId = id,
                                                mealType = mt
                                            )
                                        }
                                    }.onFailure { err -> error = err.message }
                                    mealPickForDiary = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                    TextButton(
                        onClick = { mealPickForDiary = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена") }
                }
            }
        }
    }

    if (recipeDetailId != null) {
        ModalBottomSheet(
            onDismissRequest = { recipeDetailId = null },
            sheetState = recipeSheetState
        ) {
            RecipeDetailSheet(
                recipeId = recipeDetailId!!,
                loaded = loadedDetail,
                loading = detailLoading,
                error = detailError,
                onOpenOnFatSecret = { url ->
                    runCatching {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    }
                }
            )
        }
    }
}

@Composable
private fun RecipeDetailSheet(
    recipeId: Long,
    loaded: Recipe?,
    loading: Boolean,
    error: String?,
    onOpenOnFatSecret: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when {
            loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                Text("Загрузка рецепта…", style = MaterialTheme.typography.bodyMedium)
            }
            error != null -> Text(error, color = MaterialTheme.colorScheme.error)
            loaded != null -> {
                loaded.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
                Text(loaded.name, style = MaterialTheme.typography.titleLarge)
                loaded.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
                Text(
                    "${loaded.calories.toInt()} ккал • Б:${loaded.protein.toInt()} У:${loaded.carbs.toInt()} Ж:${loaded.fat.toInt()} г",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (loaded.ingredients.isNotEmpty()) {
                    Text("Ингредиенты", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    loaded.ingredients.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (loaded.directions.isNotEmpty()) {
                    Text("Приготовление", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    loaded.directions.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                loaded.recipeUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    TextButton(
                        onClick = { onOpenOnFatSecret(url) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Редактировать на FatSecret")
                    }
                } ?: Text(
                    "Изменить рецепт в приложении нельзя — откройте его на сайте FatSecret, если он есть в избранном.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> Text("Нет данных для рецепта $recipeId")
        }
    }
}

@Composable
private fun ProfileSavedMealCard(
    meal: ProfileSavedMeal,
    onAddToDiary: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(meal.name, style = MaterialTheme.typography.titleMedium)
            meal.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            meal.suitableMeals?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                "${meal.items.size} позиций",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            meal.items.take(6).forEach { line ->
                Text(
                    "• ${line.name}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (meal.items.size > 6) {
                Text(
                    "…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            TextButton(
                onClick = onAddToDiary,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Добавить в дневник")
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    isInFavorites: Boolean,
    canAddToFavorites: Boolean,
    onAddToFavorites: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                recipe.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                recipe.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "${recipe.calories.toInt()} kcal • P:${recipe.protein.toInt()}g C:${recipe.carbs.toInt()}g F:${recipe.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (canAddToFavorites) {
                IconButton(onClick = onAddToFavorites) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Add to favorites")
                }
            } else if (isInFavorites) {
                Icon(Icons.Default.Favorite, contentDescription = "In favorites", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
