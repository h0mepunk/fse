package com.example.fse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Nutrient
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.navigation.NavController

private val ProteinColor = Color(0xFF4CAF50)
private val CarbsColor = Color(0xFFFF9800)
private val FatColor = Color(0xFF2196F3)

@Composable
fun DiaryScreen(
    container: AppContainer,
    navController: NavController
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val entries by container.foodRepository.getDiary(selectedDate).collectAsState(initial = emptyList())
    val dailyTotals by container.foodRepository.getDiaryNutrientTotals(selectedDate).collectAsState(initial = emptyMap())
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    fun norm(n: Nutrient): Double = customNorms[n] ?: n.dailyRecommended

    val kcalConsumed = dailyTotals[Nutrient.Calories] ?: 0.0
    val proteinConsumed = dailyTotals[Nutrient.Protein] ?: 0.0
    val carbsConsumed = dailyTotals[Nutrient.Carbs] ?: 0.0
    val fatConsumed = dailyTotals[Nutrient.Fat] ?: 0.0

    val kcalGoal = norm(Nutrient.Calories)
    val proteinGoal = norm(Nutrient.Protein)
    val carbsGoal = norm(Nutrient.Carbs)
    val fatGoal = norm(Nutrient.Fat)

    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Food diary",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                selectedDate.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            KbjuChart(
                protein = proteinConsumed,
                carbs = carbsConsumed,
                fat = fatConsumed
            )

            Text(
                "Remaining today",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RemainingChip("${(kcalGoal - kcalConsumed).toInt()}", "kcal")
                RemainingChip("${(proteinGoal - proteinConsumed).toInt()}", "P")
                RemainingChip("${(carbsGoal - carbsConsumed).toInt()}", "C")
                RemainingChip("${(fatGoal - fatConsumed).toInt()}", "F")
            }

            Text(
                "Consumed today",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConsumedChip(kcalConsumed.toInt(), "kcal")
                ConsumedChip(proteinConsumed.toInt(), "P")
                ConsumedChip(carbsConsumed.toInt(), "C")
                ConsumedChip(fatConsumed.toInt(), "F")
            }

            Text(
                "Entries",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            if (entries.isEmpty()) {
                Text(
                    "No entries for today. Tap + to add food.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                val mealOrder = listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack, MealType.Other)
                val grouped = entries.groupBy { it.mealType }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    mealOrder.forEach { mealType ->
                        grouped[mealType]?.let { mealEntries ->
                            val mealKcal = mealEntries.sumOf { (it.serving.calories * it.multiplier).toInt() }.toDouble()
                            val mealP = mealEntries.sumOf { (it.serving.protein * it.multiplier).toInt() }.toDouble()
                            val mealC = mealEntries.sumOf { (it.serving.carbs * it.multiplier).toInt() }.toDouble()
                            val mealF = mealEntries.sumOf { (it.serving.fat * it.multiplier).toInt() }.toDouble()
                            item(key = "header_$mealType") {
                                MealGroupCard(
                                    mealType = mealType,
                                    entries = mealEntries,
                                    mealKcal = mealKcal,
                                    mealP = mealP,
                                    mealC = mealC,
                                    mealF = mealF,
                                    onDelete = { scope.launch { container.foodRepository.removeFromDiary(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KbjuChart(protein: Double, carbs: Double, fat: Double) {
    val total = protein + carbs + fat
    val hasData = total > 0
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2 * 0.85f
                val innerRadius = radius * 0.55f

                if (hasData) {
                    val pRatio = (protein / total).toFloat()
                    val cRatio = (carbs / total).toFloat()
                    val fRatio = (fat / total).toFloat()
                    val sweepAngles = listOf(pRatio * 360f, cRatio * 360f, fRatio * 360f)
                    val colors = listOf(ProteinColor, CarbsColor, FatColor)
                    var startAngle = -90f

                    sweepAngles.zip(colors).forEach { (angle, color) ->
                        if (angle > 0.5f) {
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = angle,
                                useCenter = true,
                                topLeft = Offset(cx - radius, cy - radius),
                                size = Size(radius * 2, radius * 2)
                            )
                            startAngle += angle
                        }
                    }
                    drawCircle(color = surfaceColor, radius = innerRadius)
                } else {
                    drawCircle(color = surfaceVariantColor, radius = radius)
                    drawCircle(color = surfaceColor, radius = innerRadius)
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                if (hasData) {
                    LegendItem("P", protein.toInt(), ProteinColor)
                    LegendItem("C", carbs.toInt(), CarbsColor)
                    LegendItem("F", fat.toInt(), FatColor)
                } else {
                    Text("No data yet", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text("$label: ${value}g", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RemainingChip(value: String, unit: String) {
    val num = value.toIntOrNull() ?: 0
    Card(
        modifier = Modifier.padding(0.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (num >= 0) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            "$value $unit",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ConsumedChip(value: Int, unit: String) {
    Card(
        modifier = Modifier.padding(0.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            "$value $unit",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MealGroupCard(
    mealType: MealType,
    entries: List<DiaryEntry>,
    mealKcal: Double,
    mealP: Double,
    mealC: Double,
    mealF: Double,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    mealType.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${mealKcal.toInt()} kcal • P:${mealP.toInt()}g C:${mealC.toInt()}g F:${mealF.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEach { entry ->
                    DiaryEntryRow(
                        entry = entry,
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryRow(
    entry: DiaryEntry,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.food.name + (entry.food.brandName?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${entry.serving.description} ×${entry.multiplier}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${(entry.serving.calories * entry.multiplier).toInt()} kcal • P:${(entry.serving.protein * entry.multiplier).toInt()}g C:${(entry.serving.carbs * entry.multiplier).toInt()}g F:${(entry.serving.fat * entry.multiplier).toInt()}g",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
    }
}
