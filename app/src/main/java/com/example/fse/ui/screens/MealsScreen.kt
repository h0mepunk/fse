package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.fse.domain.model.SavedMeal
import com.example.fse.ui.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MealsScreen(container: AppContainer, navController: androidx.navigation.NavController) {
    val meals by container.mealsRepository.getSavedMeals().collectAsState(initial = emptyList())
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                        onAddToToday = {
                            scope.launch {
                                errorMessage = null
                                val result = withContext(Dispatchers.IO) {
                                    container.foodRepository.addSavedMealToDiary(meal)
                                }
                                result.onSuccess { navController.navigate(AppDestination.Diary.route) }
                                    .onFailure { errorMessage = it.message }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedMealCard(meal: SavedMeal, onAddToToday: () -> Unit) {
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
                onClick = onAddToToday,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Добавить на сегодня")
            }
        }
    }
}
