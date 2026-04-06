package com.example.fse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.fse.data.local.db.entity.UserProfileEntity
import com.example.fse.data.repository.FoodRepository
import com.example.fse.di.AppContainer
import com.example.fse.domain.cycle.CyclePhaseCalculator
import com.example.fse.domain.model.DiaryEntry
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.Serving
import com.example.fse.domain.model.PendingMealTemplateFromDiary
import com.example.fse.domain.model.Nutrient
import com.example.fse.domain.model.UserSex
import com.example.fse.ui.format.extractGrams
import com.example.fse.ui.format.formatAmountNumber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import com.example.fse.ui.AppDestination

private val ProteinColor = Color(0xFF4CAF50)
private val CarbsColor = Color(0xFFFF9800)
private val FatColor = Color(0xFF2196F3)

/** Приглушённый красный под кнопку удаления при свайпе. */
private val DiarySwipeMutedDeleteRed = Color(0xFF9E5C5C)
/** Сиреневая зона «в избранное» слева. */
private val DiarySwipeLilac = Color(0xFF8B6CBC)
private val DiarySwipeActionOnTint = Color.White.copy(alpha = 0.95f)

private val DiaryFoodItemCornerRadius = 12.dp

private val DiaryMealOrder = listOf(
    MealType.Breakfast,
    MealType.Lunch,
    MealType.Dinner,
    MealType.Snack
)

private fun mealTypeDiaryTitle(mealType: MealType): String = when (mealType) {
    MealType.Breakfast -> "Завтрак"
    MealType.Lunch -> "Обед"
    MealType.Dinner -> "Ужин"
    MealType.Snack -> "Перекус"
    MealType.Other -> "Другое"
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDateInSystemDefault(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiaryScreen(
    container: AppContainer,
    navController: NavController
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val entries by container.foodRepository.getDiary(selectedDate).collectAsState(initial = emptyList())
    val dailyTotals by container.foodRepository.getDiaryNutrientTotals(selectedDate).collectAsState(initial = emptyMap())
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())
    val userProfile by container.userProfileRepository.observeProfile().collectAsState(initial = UserProfileEntity())
    val periodEntities by container.userProfileRepository.observePeriodEntities().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    var expandedMealTypes by remember { mutableStateOf(DiaryMealOrder.toSet()) }
    var saveMealTemplateTarget by remember { mutableStateOf<Pair<MealType, List<DiaryEntry>>?>(null) }
    var portionEditTarget by remember { mutableStateOf<DiaryEntry?>(null) }
    val portionEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        container.foodRepository.diarySyncErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun norm(n: Nutrient): Double = customNorms[n] ?: n.dailyRecommended

    val kcalConsumed = dailyTotals[Nutrient.Calories] ?: 0.0
    val proteinConsumed = dailyTotals[Nutrient.Protein] ?: 0.0
    val carbsConsumed = dailyTotals[Nutrient.Carbs] ?: 0.0
    val fatConsumed = dailyTotals[Nutrient.Fat] ?: 0.0

    val kcalGoal = norm(Nutrient.Calories)
    val proteinGoal = norm(Nutrient.Protein)
    val carbsGoal = norm(Nutrient.Carbs)
    val fatGoal = norm(Nutrient.Fat)

    val cyclePhaseLabel = remember(selectedDate, userProfile, periodEntities) {
        val sex = runCatching { UserSex.valueOf(userProfile.sex) }.getOrNull() ?: UserSex.UNSPECIFIED
        if (sex != UserSex.FEMALE) {
            null
        } else {
            val starts = periodEntities.map { LocalDate.parse(it.startDate) }.sorted()
            CyclePhaseCalculator.phaseForDate(selectedDate, starts)?.labelRu ?: "Нет данных"
        }
    }

    saveMealTemplateTarget?.let { (mealType, mealEntries) ->
        AlertDialog(
            onDismissRequest = { saveMealTemplateTarget = null },
            title = { Text("Сохранить приём пищи?") },
            text = {
                Text("Сохранить продукты из «${mealTypeDiaryTitle(mealType)}» как шаблон? Откроется вкладка Cookbook, чтобы ввести название.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        container.offerMealTemplateFromDiary(
                            PendingMealTemplateFromDiary(
                                date = selectedDate,
                                mealType = mealType,
                                entries = mealEntries
                            )
                        )
                        saveMealTemplateTarget = null
                        navController.navigate(AppDestination.Cookbook.route) {
                            launchSingleTop = true
                        }
                    }
                ) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { saveMealTemplateTarget = null }) { Text("Нет") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            selectedDate = ms.toLocalDateInSystemDefault()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
               // .padding(horizontal = 16.dp, top = 16.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Choose date",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (cyclePhaseLabel != null) {
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .widthIn(min = 88.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Цикл",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                cyclePhaseLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            KbjuChart(
                protein = proteinConsumed,
                carbs = carbsConsumed,
                fat = fatConsumed
            )

            DiaryConsumedRemainingCard(
                proteinConsumed = proteinConsumed.toInt(),
                fatConsumed = fatConsumed.toInt(),
                carbsConsumed = carbsConsumed.toInt(),
                kcalConsumed = kcalConsumed.toInt(),
                proteinLeft = (proteinGoal - proteinConsumed).toInt(),
                fatLeft = (fatGoal - fatConsumed).toInt(),
                carbsLeft = (carbsGoal - carbsConsumed).toInt(),
                kcalLeft = (kcalGoal - kcalConsumed).toInt(),
                modifier = Modifier.padding(top = 16.dp)
            )

            val grouped = entries.groupBy { it.mealType }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 4.dp)
                ) {
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                if (selectedDate == LocalDate.now()) {
                                    "Нажмите «+» у нужного приёма пищи, чтобы добавить еду."
                                } else {
                                    "За этот день нет записей. Добавьте еду через «+» у приёма пищи."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    DiaryMealOrder.forEach { mealType ->
                        val mealEntries = grouped[mealType] ?: emptyList()
                        val mealKcal =
                            mealEntries.sumOf { (it.serving.calories * it.multiplier).toInt() }.toDouble()
                        val mealP =
                            mealEntries.sumOf { (it.serving.protein * it.multiplier).toInt() }.toDouble()
                        val mealF = mealEntries.sumOf { (it.serving.fat * it.multiplier).toInt() }.toDouble()
                        val mealC =
                            mealEntries.sumOf { (it.serving.carbs * it.multiplier).toInt() }.toDouble()
                        item(key = "header_$mealType") {
                            val expanded = mealType in expandedMealTypes
                            MealGroupCard(
                                mealTitle = mealTypeDiaryTitle(mealType),
                                entries = mealEntries,
                                mealKcal = mealKcal,
                                mealP = mealP,
                                mealC = mealC,
                                mealF = mealF,
                                expanded = expanded,
                                foodRepository = container.foodRepository,
                                scope = scope,
                                onToggleExpand = {
                                    expandedMealTypes =
                                        if (mealType in expandedMealTypes) expandedMealTypes - mealType
                                        else expandedMealTypes + mealType
                                },
                                onLongClick = {
                                    saveMealTemplateTarget = mealType to mealEntries
                                },
                                onAddFood = {
                                    navController.navigate(AppDestination.searchFoodPath(selectedDate, mealType))
                                },
                                onDelete = { scope.launch { container.foodRepository.removeFromDiary(it) } },
                                onEditPortion = { portionEditTarget = it }
                            )
                        }
                    }
                    grouped[MealType.Other]?.takeIf { it.isNotEmpty() }?.let { otherEntries ->
                        val mealKcal =
                            otherEntries.sumOf { (it.serving.calories * it.multiplier).toInt() }.toDouble()
                        val mealP =
                            otherEntries.sumOf { (it.serving.protein * it.multiplier).toInt() }.toDouble()
                        val mealF = otherEntries.sumOf { (it.serving.fat * it.multiplier).toInt() }.toDouble()
                        val mealC =
                            otherEntries.sumOf { (it.serving.carbs * it.multiplier).toInt() }.toDouble()
                        item(key = "header_other") {
                            val mt = MealType.Other
                            val expanded = mt in expandedMealTypes
                            MealGroupCard(
                                mealTitle = mealTypeDiaryTitle(mt),
                                entries = otherEntries,
                                mealKcal = mealKcal,
                                mealP = mealP,
                                mealC = mealC,
                                mealF = mealF,
                                expanded = expanded,
                                foodRepository = container.foodRepository,
                                scope = scope,
                                onToggleExpand = {
                                    expandedMealTypes =
                                        if (mt in expandedMealTypes) expandedMealTypes - mt
                                        else expandedMealTypes + mt
                                },
                                onLongClick = {
                                    saveMealTemplateTarget = mt to otherEntries
                                },
                                onAddFood = {
                                    navController.navigate(AppDestination.searchFoodPath(selectedDate, mt))
                                },
                                onDelete = { scope.launch { container.foodRepository.removeFromDiary(it) } },
                                onEditPortion = { portionEditTarget = it }
                            )
                        }
                    }
                }
            }

            DiaryNormsPanel(
                selectedDate = selectedDate,
                container = container,
                navController = navController,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (portionEditTarget != null) {
        val entryToEdit = portionEditTarget!!
        ModalBottomSheet(
            onDismissRequest = { portionEditTarget = null },
            sheetState = portionEditSheetState
        ) {
            DiaryEntryPortionEditContent(
                entry = entryToEdit,
                onDismiss = { portionEditTarget = null },
                onSave = { serving, mult ->
                    portionEditTarget?.let { e ->
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    container.foodRepository.updateDiaryEntryPortion(e, serving, mult)
                                }
                            }
                            portionEditTarget = null
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun KbjuChart(protein: Double, carbs: Double, fat: Double) {
    val total = protein + carbs + fat
    val hasData = total > 0
    val surfaceColor = MaterialTheme.colorScheme.surface
    val trackColor = MaterialTheme.colorScheme.outlineVariant

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
                val ringStroke = (radius - innerRadius).coerceAtLeast(6.dp.toPx())

                if (hasData) {
                    val pRatio = (protein / total).toFloat()
                    val fRatio = (fat / total).toFloat()
                    val cRatio = (carbs / total).toFloat()
                    val sweepAngles = listOf(pRatio * 360f, fRatio * 360f, cRatio * 360f)
                    val colors = listOf(ProteinColor, FatColor, CarbsColor)
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
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = ringStroke)
                    )
                    drawCircle(color = surfaceColor, radius = innerRadius)
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                if (hasData) {
                    LegendItem("P", protein.toInt(), ProteinColor)
                    LegendItem("F", fat.toInt(), FatColor)
                    LegendItem("C", carbs.toInt(), CarbsColor)
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
private fun DiaryConsumedRemainingCard(
    proteinConsumed: Int,
    fatConsumed: Int,
    carbsConsumed: Int,
    kcalConsumed: Int,
    proteinLeft: Int,
    fatLeft: Int,
    carbsLeft: Int,
    kcalLeft: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConsumedRemainingColumn(
                label = "P",
                consumedText = "${proteinConsumed}g",
                remainingText = "${proteinLeft}g",
                remainingOk = proteinLeft >= 0,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            ConsumedRemainingColumn(
                label = "F",
                consumedText = "${fatConsumed}g",
                remainingText = "${fatLeft}g",
                remainingOk = fatLeft >= 0,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            ConsumedRemainingColumn(
                label = "C",
                consumedText = "${carbsConsumed}g",
                remainingText = "${carbsLeft}g",
                remainingOk = carbsLeft >= 0,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            ConsumedRemainingColumn(
                label = "kcal",
                consumedText = "$kcalConsumed",
                remainingText = "$kcalLeft",
                remainingOk = kcalLeft >= 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConsumedRemainingColumn(
    label: String,
    consumedText: String,
    remainingText: String,
    remainingOk: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = consumedText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
        Text(
            text = remainingText,
            style = MaterialTheme.typography.bodySmall,
            color = if (remainingOk) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiaryMacroValuesRow(
    p: Int,
    fat: Int,
    carbs: Int,
    kcal: Int,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    Row(modifier.fillMaxWidth()) {
        MacroValueCell("P", "${p}g", valueStyle, Modifier.weight(1f))
        MacroValueCell("F", "${fat}g", valueStyle, Modifier.weight(1f))
        MacroValueCell("C", "${carbs}g", valueStyle, Modifier.weight(1f))
        MacroValueCell("kcal", "$kcal", valueStyle, Modifier.weight(1f))
    }
}

@Composable
private fun MacroValueCell(
    label: String,
    value: String,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            value,
            style = valueStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MealGroupCard(
    mealTitle: String,
    entries: List<DiaryEntry>,
    mealKcal: Double,
    mealP: Double,
    mealC: Double,
    mealF: Double,
    expanded: Boolean,
    foodRepository: FoodRepository,
    scope: CoroutineScope,
    onToggleExpand: () -> Unit,
    onLongClick: () -> Unit,
    onAddFood: () -> Unit,
    onDelete: (String) -> Unit,
    onEditPortion: (DiaryEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = onToggleExpand,
                            onLongClick = if (entries.isNotEmpty()) {
                                { onLongClick() }
                            } else {
                                null
                            }
                        )
                        .padding(vertical = 4.dp, horizontal = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mealTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onAddFood) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить еду")
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }
            if (!expanded) {
                DiaryMacroValuesRow(
                    p = mealP.toInt(),
                    fat = mealF.toInt(),
                    carbs = mealC.toInt(),
                    kcal = mealKcal.toInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entries.isEmpty()) {
                        Text(
                            "Пока нет продуктов",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        entries.forEach { entry ->
                            DiaryEntryRow(
                                entry = entry,
                                foodRepository = foodRepository,
                                scope = scope,
                                onDelete = { onDelete(entry.id) },
                                onEditPortion = { onEditPortion(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryRow(
    entry: DiaryEntry,
    foodRepository: FoodRepository,
    scope: CoroutineScope,
    onDelete: () -> Unit,
    onEditPortion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        DiarySwipeableEntryRow(
            entry = entry,
            foodRepository = foodRepository,
            scope = scope,
            onDelete = onDelete,
            onEditPortion = onEditPortion
        )
    }
}

@Composable
private fun DiarySwipeableEntryRow(
    entry: DiaryEntry,
    foodRepository: FoodRepository,
    scope: CoroutineScope,
    onDelete: () -> Unit,
    onEditPortion: () -> Unit
) {
    val density = LocalDensity.current
    val actionZoneWidth = (200f / density.density).dp
    val maxOffsetPx = with(density) { actionZoneWidth.toPx() }
    var offsetPx by remember(entry.id) { mutableFloatStateOf(0f) }

    var isFavorite by remember(entry.food.id) { mutableStateOf(false) }
    LaunchedEffect(entry.food.id) {
        isFavorite = withContext(Dispatchers.IO) { foodRepository.isFavorite(entry.food.id) }
    }

    val cardShapeLeftFlat = RoundedCornerShape(
        topStart = DiaryFoodItemCornerRadius,
        bottomStart = DiaryFoodItemCornerRadius
    )
    val cardShapeRightFlat = RoundedCornerShape(
        topEnd = DiaryFoodItemCornerRadius,
        bottomEnd = DiaryFoodItemCornerRadius
    )
    val cardShape = if (offsetPx >= 0f) cardShapeRightFlat else cardShapeLeftFlat

    val favStartShape = RoundedCornerShape(
        topStart = DiaryFoodItemCornerRadius,
        bottomStart = DiaryFoodItemCornerRadius
    )
    val deleteEndShape = RoundedCornerShape(
        topEnd = DiaryFoodItemCornerRadius,
        bottomEnd = DiaryFoodItemCornerRadius
    )

    val draggableState = rememberDraggableState { delta ->
        offsetPx = (offsetPx + delta).coerceIn(-maxOffsetPx, maxOffsetPx)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        offsetPx = when {
                            offsetPx < -maxOffsetPx / 2f -> -maxOffsetPx
                            offsetPx > maxOffsetPx / 2f -> maxOffsetPx
                            else -> 0f
                        }
                    }
                )
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, cardShape)
                .combinedClickable(
                    onClick = {
                        if (offsetPx != 0f) offsetPx = 0f
                    },
                    onLongClick = onEditPortion
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        entry.food.name + (entry.food.brandName?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    extractGrams(entry.serving.description)?.let { baseGrams ->
                        Text(
                            text = "${formatAmountNumber(baseGrams * entry.multiplier)} g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                DiaryMacroValuesRow(
                    p = (entry.serving.protein * entry.multiplier).toInt(),
                    fat = (entry.serving.fat * entry.multiplier).toInt(),
                    carbs = (entry.serving.carbs * entry.multiplier).toInt(),
                    kcal = (entry.serving.calories * entry.multiplier).toInt(),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(-1f)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(actionZoneWidth)
                    .fillMaxHeight()
                    .clip(favStartShape)
                    .background(DiarySwipeLilac, favStartShape)
                    .clickable {
                        val target = !isFavorite
                        isFavorite = target
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    if (target) foodRepository.addFavorite(entry.food)
                                    else foodRepository.removeFavorite(entry.food.id)
                                }
                            }.onFailure {
                                isFavorite = !target
                            }
                            offsetPx = 0f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = DiarySwipeActionOnTint
                    )
                    Text(
                        text = if (isFavorite) "В избранном" else "В избранное",
                        color = DiarySwipeActionOnTint,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionZoneWidth)
                    .fillMaxHeight()
                    .clip(deleteEndShape)
                    .background(DiarySwipeMutedDeleteRed, deleteEndShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = DiarySwipeActionOnTint
                    )
                    Text(
                        text = "Удалить",
                        color = DiarySwipeActionOnTint,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryPortionEditContent(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onSave: (Serving, Double) -> Unit
) {
    val servings = entry.food.servings
    if (servings.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Нет данных о порциях для этого продукта.")
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
        return
    }
    var selectedServing by remember(entry.id) { mutableStateOf(entry.serving) }
    var selectedMultText by remember(entry.id) {
        val g = extractGrams(entry.serving.description)
        mutableStateOf(
            if (g == null) formatAmountNumber(entry.multiplier) else "1"
        )
    }
    var gramsText by remember(entry.id) {
        val g = extractGrams(entry.serving.description)
        mutableStateOf(
            if (g != null) formatAmountNumber(g * entry.multiplier) else ""
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Граммовка", style = MaterialTheme.typography.titleLarge)
        Text(
            entry.food.name + (entry.food.brandName?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium
        )
        Text("Порция", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        servings.forEach { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedServing = s
                        val ng = extractGrams(s.description)
                        if (ng != null) {
                            gramsText = formatAmountNumber(ng)
                            selectedMultText = "1"
                        } else {
                            selectedMultText = "1"
                            gramsText = ""
                        }
                    }
                    .padding(8.dp)
            ) {
                Text(s.description)
                Text(" - ${s.calories.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
        val servingGrams = extractGrams(selectedServing.description)
        OutlinedTextField(
            value = if (servingGrams != null) gramsText else selectedMultText,
            onValueChange = {
                if (servingGrams != null) gramsText = it
                else selectedMultText = it
            },
            label = { Text(if (servingGrams != null) "Граммы" else "Множитель") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Row {
            TextButton(onClick = onDismiss) { Text("Отмена") }
            Button(
                onClick = {
                    val multiplier = if (servingGrams != null && servingGrams > 0) {
                        val grams = gramsText.replace(',', '.').toDoubleOrNull() ?: servingGrams
                        (grams / servingGrams).coerceAtLeast(0.01)
                    } else {
                        selectedMultText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.01) ?: 1.0
                    }
                    onSave(selectedServing, multiplier)
                }
            ) { Text("Сохранить") }
        }
    }
}
