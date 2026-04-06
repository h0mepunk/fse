package com.example.fse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Food
import com.example.fse.domain.model.Nutrient
import com.example.fse.ui.AppDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Bottom panel on the diary: day/week micronutrient norms, expandable list, collapse control.
 */
@Composable
fun DiaryNormsPanel(
    selectedDate: LocalDate,
    container: AppContainer,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isWeekly by remember { mutableStateOf(false) }
    var selectedNutrient by remember { mutableStateOf<Nutrient?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val dailyTotals by container.foodRepository.getDiaryNutrientTotals(selectedDate).collectAsState(initial = emptyMap())
    val weeklyTotals by container.foodRepository.getWeeklyNutrientTotals(selectedDate).collectAsState(initial = emptyMap())
    val nutrientTotals = if (isWeekly) weeklyTotals else dailyTotals
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())

    fun effectiveNorm(n: Nutrient): Double = customNorms[n] ?: n.dailyRecommended

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isWeekly) "Нормы · 7 дней до ${selectedDate.format(dateFormatter)}"
                            else "Нормы · ${selectedDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (expanded) "Нажмите, чтобы свернуть список" else "Нажмите, чтобы развернуть",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("День", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = isWeekly,
                        onCheckedChange = { isWeekly = it }
                    )
                    Text("Неделя", style = MaterialTheme.typography.labelSmall)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.navigate(AppDestination.NormsEdit.route) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Изменить нормы")
                        }
                    }

                    if (selectedNutrient == null) {
                        val micronutrients = Nutrient.entries.filter {
                            it !in listOf(Nutrient.Calories, Nutrient.Protein, Nutrient.Carbs, Nutrient.Fat)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            items(micronutrients, key = { it.name }) { nutrient ->
                                val recommended =
                                    if (isWeekly) effectiveNorm(nutrient) * 7 else effectiveNorm(nutrient)
                                val consumed = nutrientTotals[nutrient] ?: 0.0
                                val progress = (consumed / recommended).coerceIn(0.0, 2.0)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedNutrient = nutrient }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(nutrient.displayName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${consumed.toInt()}/${recommended.toInt()} ${nutrient.unit} (${(progress * 100).toInt().coerceAtMost(100)}%)",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress.toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            item {
                                TextButton(
                                    onClick = { expanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Свернуть")
                                }
                            }
                        }
                    } else {
                        val nutrient = selectedNutrient!!
                        val minAmount = effectiveNorm(nutrient) * 0.1
                        val richFoods by container.foodRepository.getFavoritesRichIn(nutrient, minAmount)
                            .collectAsState(initial = emptyList())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Продукты с ${nutrient.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(richFoods, key = { it.id }) { food ->
                                    NormsPanelFoodCard(food = food, nutrient = nutrient)
                                }
                            }
                            TextButton(
                                onClick = { selectedNutrient = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Назад к нормам")
                            }
                            TextButton(
                                onClick = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Свернуть")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NormsPanelFoodCard(food: Food, nutrient: Nutrient) {
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
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleSmall)
                food.brandName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text("${value.toInt()} ${nutrient.unit}", style = MaterialTheme.typography.titleSmall)
        }
    }
}
