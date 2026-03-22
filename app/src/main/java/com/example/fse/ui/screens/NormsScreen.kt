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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.ui.AppDestination
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.Nutrient
import java.time.LocalDate

@Composable
fun NormsScreen(
    container: AppContainer,
    navController: NavController
) {
    var selectedNutrient by remember { mutableStateOf<Nutrient?>(null) }
    var isWeekly by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val favorites by container.foodRepository.getFavorites().collectAsState(initial = emptyList())
    val dailyTotals by container.foodRepository.getDiaryNutrientTotals(today).collectAsState(initial = emptyMap())
    val weeklyTotals by container.foodRepository.getWeeklyNutrientTotals(today).collectAsState(initial = emptyMap())
    val nutrientTotals = if (isWeekly) weeklyTotals else dailyTotals
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())

    fun effectiveNorm(n: Nutrient): Double = customNorms[n] ?: n.dailyRecommended

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                if (isWeekly) "Weekly norms" else "Daily norms",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = { navController.navigate(AppDestination.NormsEdit.route) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit norms")
            }
        }
        Text("${today}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.Switch(checked = isWeekly, onCheckedChange = { isWeekly = it })
            Text(if (isWeekly) "Weekly" else "Daily", style = MaterialTheme.typography.bodyMedium)
        }
        Text("Tap a nutrient to see favorite foods that help reach your goal.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

        if (selectedNutrient == null) {
            val micronutrients = Nutrient.entries.filter {
                it !in listOf(Nutrient.Calories, Nutrient.Protein, Nutrient.Carbs, Nutrient.Fat)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(micronutrients) { nutrient ->
                    val recommended = if (isWeekly) effectiveNorm(nutrient) * 7 else effectiveNorm(nutrient)
                    val consumed = nutrientTotals[nutrient] ?: 0.0
                    val progress = (consumed / recommended).coerceIn(0.0, 2.0)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNutrient = nutrient }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(nutrient.displayName, style = MaterialTheme.typography.titleMedium)
                                Text("${consumed.toInt()}/${recommended.toInt()} ${nutrient.unit} (${(progress * 100).toInt().coerceAtMost(100)}%)", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = progress.toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Goal: ${recommended.toInt()} ${nutrient.unit}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            val nutrient = selectedNutrient!!
            Text("Foods rich in ${nutrient.displayName}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Text("From your favorites (tap to add to diary)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            val minAmount = effectiveNorm(nutrient) * 0.1
            val richFoods by container.foodRepository.getFavoritesRichIn(nutrient, minAmount).collectAsState(initial = emptyList())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(richFoods, key = { it.id }) { food ->
                    NutrientFoodCard(food = food, nutrient = nutrient)
                }
            }
            androidx.compose.material3.TextButton(onClick = { selectedNutrient = null }) {
                Text("Back to norms")
            }
        }
    }
}

@Composable
private fun NutrientFoodCard(food: Food, nutrient: Nutrient) {
    val value = food.servings.maxOfOrNull { serving ->
        when (nutrient) {
            Nutrient.Calories -> serving.calories
            Nutrient.Protein -> serving.protein
            Nutrient.Carbs -> serving.carbs
            Nutrient.Fat -> serving.fat
            Nutrient.Fiber -> serving.fiber
            Nutrient.Sodium -> serving.sodium
            Nutrient.Calcium -> serving.calcium
            Nutrient.Iron -> serving.iron
            Nutrient.VitaminA -> serving.vitaminA
            Nutrient.VitaminC -> serving.vitaminC
            Nutrient.VitaminD -> serving.vitaminD
            Nutrient.Potassium -> serving.potassium
        }
    } ?: 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleSmall)
                food.brandName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text("${value.toInt()} ${nutrient.unit}", style = MaterialTheme.typography.titleSmall)
        }
    }
}
