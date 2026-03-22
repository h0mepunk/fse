package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CookbookScreen(container: AppContainer) {
    var query by remember { mutableStateOf("") }
    val cachedRecipes by container.recipeRepository.getCachedRecipes().collectAsState(initial = emptyList())
    val lastQuery by container.recipeRepository.getLastQuery().collectAsState(initial = "")
    val myRecipes by container.recipeRepository.getUserFavoriteRecipes().collectAsState(initial = emptyList())
    val hasOAuth by container.oauth1TokenStore.hasTokens.collectAsState(initial = false)
    var searchResults by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lastQuery) {
        query = lastQuery
    }

    LaunchedEffect(Unit) {
        if (myRecipes.isEmpty() && cachedRecipes.isEmpty() && searchResults.isEmpty() && !isSearching) {
            isSearching = true
            withContext(Dispatchers.IO) {
                container.recipeRepository.searchRecipes("recipe")
                    .onSuccess { searchResults = it }
                    .onFailure { error = it.message }
            }
            isSearching = false
        }
    }

    var userSearched by remember { mutableStateOf(false) }

    val displayRecipes = when {
        userSearched -> searchResults
        myRecipes.isNotEmpty() -> myRecipes
        else -> if (searchResults.isNotEmpty()) searchResults else cachedRecipes
    }
    val showMyRecipes = !userSearched && myRecipes.isNotEmpty()
    val showSearchNoResults = userSearched && searchResults.isEmpty() && !isSearching
    val myRecipeIds = myRecipes.map { it.id }.toSet()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("My Cookbook", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))
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
            label = { Text("Search recipes") },
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

        when {
            isSearching -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            showSearchNoResults -> Text(
                "No results for \"$query\"",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
            displayRecipes.isEmpty() -> Text(
                "Search for recipes above or connect FatSecret in Account to see your saved recipes",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
            else -> {
                if (showMyRecipes) {
                    Text("Saved recipes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(displayRecipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            isInFavorites = recipe.id in myRecipeIds,
                            canAddToFavorites = hasOAuth && recipe.id !in myRecipeIds,
                            onAddToFavorites = {
                                scope.launch {
                                    container.recipeRepository.addRecipeToFavorites(recipe.id)
                                        .onSuccess { /* will refresh via Flow */ }
                                        .onFailure { error = it.message }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

        if (hasOAuth) {
            FloatingActionButton(
                onClick = { scope.launch { focusRequester.requestFocus() } },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add recipe")
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    isInFavorites: Boolean,
    canAddToFavorites: Boolean,
    onAddToFavorites: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
