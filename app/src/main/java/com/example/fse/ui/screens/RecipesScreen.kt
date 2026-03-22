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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Recipe

@Composable
fun CookbookScreen(container: AppContainer) {
    var query by remember { mutableStateOf("") }
    val myRecipes by container.recipeRepository.getUserFavoriteRecipes().collectAsState(initial = emptyList())
    val hasOAuth by container.oauth1TokenStore.hasTokens.collectAsState(initial = false)
    val displayRecipes = myRecipes.filter { recipe ->
        query.isBlank() || recipe.name.contains(query.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Cookbook", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search my recipes") },
            singleLine = true
        )

        when {
            !hasOAuth -> Text(
                "Connect FatSecret in Account to see your recipes",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
            myRecipes.isEmpty() -> Text(
                "You don't have saved recipes in FatSecret yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
            displayRecipes.isEmpty() -> Text(
                "No results for \"$query\" in your recipes",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
            else -> {
                Text("My recipes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(displayRecipes, key = { it.id }) { recipe ->
                        RecipeCard(recipe = recipe)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe
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
            androidx.compose.material3.Icon(
                Icons.Default.Favorite,
                contentDescription = "My recipe",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}
