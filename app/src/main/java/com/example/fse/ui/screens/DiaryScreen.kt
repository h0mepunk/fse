package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Nutrient
import com.example.fse.ui.AppDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BlueAccent = Color(0xFF6B83FF)
private val MacroProtein = Color(0xFF26C281)
private val SwipeDeleteColor = Color(0xFFE53935)

@Composable
fun DiaryScreen(
    container: AppContainer,
    navController: NavController
) {
    val selectedDate = LocalDate.now()
    val entries by container.foodRepository.getDiary(selectedDate).collectAsState(initial = emptyList())
    val dailyTotals by container.foodRepository.getDiaryNutrientTotals(selectedDate).collectAsState(initial = emptyMap())
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    fun norm(n: Nutrient): Double = customNorms[n] ?: n.dailyRecommended
    fun toRouteMealType(mealType: MealType): String = mealType.name.lowercase(Locale.US)

    val kcalConsumed = dailyTotals[Nutrient.Calories] ?: 0.0
    val proteinConsumed = dailyTotals[Nutrient.Protein] ?: 0.0
    val carbsConsumed = dailyTotals[Nutrient.Carbs] ?: 0.0
    val fatConsumed = dailyTotals[Nutrient.Fat] ?: 0.0

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Today, ${
                        selectedDate.format(
                            DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
                        )
                    }",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BlueAccent,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroCard("Protein", proteinConsumed, norm(Nutrient.Protein), MacroProtein, Modifier.weight(1f))
                    MacroCard("Fats", fatConsumed, norm(Nutrient.Fat), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    MacroCard("Carbs", carbsConsumed, norm(Nutrient.Carbs), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    MacroCard("Kcal", kcalConsumed, norm(Nutrient.Calories), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                }
            }

            mealTypesInOrder().forEach { mealType ->
                val mealEntries = entries.filter { it.mealType == mealType }
                item(key = "meal_$mealType") {
                    MealCard(
                        mealType = mealType,
                        entries = mealEntries,
                        onAddProduct = {
                            navController.navigate("${AppDestination.SearchFood.route}?mealType=${toRouteMealType(mealType)}")
                        },
                        onDelete = { id ->
                            scope.launch { container.foodRepository.removeFromDiary(id) }
                        }
                    )
                }
            }
        }
    }
}

private fun mealTypesInOrder(): List<MealType> = listOf(
    MealType.Breakfast,
    MealType.Lunch,
    MealType.Dinner,
    MealType.Snack
)

@Composable
private fun MacroCard(
    label: String,
    consumed: Double,
    target: Double,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(consumed.toInt().toString(), style = MaterialTheme.typography.titleLarge, color = valueColor)
            Text(target.toInt().toString(), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
private fun MealCard(
    mealType: MealType,
    entries: List<DiaryEntry>,
    onAddProduct: () -> Unit,
    onDelete: (String) -> Unit
) {
    val kcal = entries.sumOf { it.serving.calories * it.multiplier }
    val p = entries.sumOf { it.serving.protein * it.multiplier }
    val f = entries.sumOf { it.serving.fat * it.multiplier }
    val c = entries.sumOf { it.serving.carbs * it.multiplier }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconForMealType(mealType), contentDescription = null, tint = BlueAccent)
                    Text(
                        mealType.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${kcal.toInt()} Kcal", style = MaterialTheme.typography.titleLarge, color = Color.DarkGray)
                    IconButton(onClick = onAddProduct) {
                        Icon(Icons.Default.Add, contentDescription = "Add food", tint = BlueAccent)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (entries.isEmpty()) 0.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("P ${formatDecimal(p)}", color = if (p > 0.0) MacroProtein else Color.Gray)
                Text("F ${formatDecimal(f)}", color = Color.DarkGray)
                Text("C ${formatDecimal(c)}", color = Color.DarkGray)
            }

            if (entries.isEmpty()) {
                Text("No products yet", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                entries.forEach { entry ->
                    DiaryEntryRow(entry = entry, onDelete = { onDelete(entry.id) })
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
    var offsetX by remember(entry.id) { mutableFloatStateOf(0f) }
    val revealWidth = 72f
    val dragThreshold = 28f
    val contentModifier = Modifier
        .fillMaxWidth()
        .pointerInput(entry.id) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    offsetX = (offsetX + dragAmount).coerceIn(-revealWidth, 0f)
                },
                onDragEnd = {
                    offsetX = if (offsetX <= -dragThreshold) -revealWidth else 0f
                },
                onDragCancel = {
                    offsetX = if (offsetX <= -dragThreshold) -revealWidth else 0f
                }
            )
        }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth()
                    .padding(end = 0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SwipeDeleteColor),
                        modifier = Modifier
                            .pointerInput(entry.id) {
                                detectTapGestures(onTap = { onDelete() })
                            }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete food",
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = contentModifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.food.name + (entry.food.brandName?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${entry.multiplier} × ${entry.serving.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueAccent
                    )
                    Text(
                        "P ${formatDecimal(entry.serving.protein * entry.multiplier)}  F ${formatDecimal(entry.serving.fat * entry.multiplier)}  C ${formatDecimal(entry.serving.carbs * entry.multiplier)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text("${(entry.serving.calories * entry.multiplier).toInt()}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun iconForMealType(mealType: MealType): ImageVector = when (mealType) {
    MealType.Breakfast -> Icons.Default.FreeBreakfast
    MealType.Lunch -> Icons.Default.LunchDining
    MealType.Dinner -> Icons.Default.Nightlight
    MealType.Snack -> Icons.Default.Fastfood
    MealType.Other -> Icons.Default.Fastfood
}

private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.2f", value)
