package com.example.fse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Food
import com.example.fse.ui.AppDestination
import java.time.LocalDate

@Composable
fun FavoritesScreen(
    container: AppContainer,
    navController: NavController
) {
    val favorites by container.foodRepository.getFavorites().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        if (favorites.isEmpty()) {
            Text("No favorites yet. Add foods from search and tap the heart icon.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(favorites, key = { it.id }) { food ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(AppDestination.searchFoodPath(LocalDate.now())) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(food.name, style = MaterialTheme.typography.titleMedium)
                            food.brandName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (food.servings.isNotEmpty()) {
                                Text("${food.servings.first().calories.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
