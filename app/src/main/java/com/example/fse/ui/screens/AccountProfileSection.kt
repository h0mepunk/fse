package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import com.example.fse.data.local.db.entity.PeriodStartEntity
import com.example.fse.data.local.db.entity.UserProfileEntity
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.ActivityLevel
import com.example.fse.domain.model.NutritionGoal
import com.example.fse.domain.model.UserSex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileSection(container: AppContainer, modifier: Modifier = Modifier) {
    val profile by container.userProfileRepository.observeProfile().collectAsState(
        initial = UserProfileEntity()
    )
    val periodRows by container.userProfileRepository.observePeriodEntities().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(UserSex.UNSPECIFIED) }
    var selectedActivity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var selectedGoal by remember { mutableStateOf(NutritionGoal.MAINTENANCE) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showPeriodDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        selectedSex = runCatching { UserSex.valueOf(profile.sex) }.getOrDefault(UserSex.UNSPECIFIED)
        heightText = profile.heightCm?.toString() ?: ""
        weightText = profile.weightKg?.toString() ?: ""
        ageText = profile.ageYears?.toString() ?: ""
        selectedActivity = ActivityLevel.entries.minByOrNull {
            kotlin.math.abs(it.multiplier - profile.activityMultiplier.toDouble())
        } ?: ActivityLevel.MODERATE
        selectedGoal = runCatching { NutritionGoal.valueOf(profile.goal) }.getOrDefault(NutritionGoal.MAINTENANCE)
    }

    if (showPeriodDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPeriodDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                            scope.launch {
                                container.userProfileRepository.addPeriodStart(d)
                                showPeriodDatePicker = false
                            }
                        } ?: run { showPeriodDatePicker = false }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPeriodDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Профиль и цели КБЖУ", style = MaterialTheme.typography.titleMedium)

            Text("Пол", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    UserSex.FEMALE to "Ж",
                    UserSex.MALE to "М",
                    UserSex.UNSPECIFIED to "Не указывать"
                ).forEach { (sex, label) ->
                    FilterChip(
                        selected = selectedSex == sex,
                        onClick = { selectedSex = sex },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("Рост, см") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Вес, кг") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it },
                label = { Text("Возраст, лет") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Активность", style = MaterialTheme.typography.labelMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedActivity == level,
                        onClick = { selectedActivity = level },
                        label = { Text(level.labelRu) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("Цель", style = MaterialTheme.typography.labelMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NutritionGoal.entries.forEach { g ->
                    FilterChip(
                        selected = selectedGoal == g,
                        onClick = { selectedGoal = g },
                        label = { Text(g.labelRu) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            val h = heightText.replace(',', '.').toFloatOrNull()
                            val w = weightText.replace(',', '.').toFloatOrNull()
                            val a = ageText.toIntOrNull()
                            container.userProfileRepository.saveProfile(
                                sex = selectedSex,
                                heightCm = h,
                                weightKg = w,
                                ageYears = a,
                                activity = selectedActivity,
                                goal = selectedGoal
                            )
                            feedback = "Профиль сохранён"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить") }

                Button(
                    onClick = {
                        scope.launch {
                            val h = heightText.replace(',', '.').toFloatOrNull()
                            val w = weightText.replace(',', '.').toFloatOrNull()
                            val a = ageText.toIntOrNull()
                            container.userProfileRepository.saveProfile(
                                sex = selectedSex,
                                heightCm = h,
                                weightKg = w,
                                ageYears = a,
                                activity = selectedActivity,
                                goal = selectedGoal
                            )
                            val ok = container.userProfileRepository.applyCalculatedNormsToStore()
                            feedback = if (ok) {
                                "Нормы калорий и БЖУ записаны (см. дневник)"
                            } else {
                                "Укажите рост, вес и возраст"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("В нормы КБЖУ") }
            }

            if (selectedSex == UserSex.FEMALE) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Цикл (первый день месячных)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "После ${com.example.fse.domain.cycle.CyclePhaseCalculator.MIN_PERIODS_FOR_PREDICTION} дат в дневнике появится оценка фазы (овуляция / месячные).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showPeriodDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Добавить дату начала") }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(periodRows, key = { it.id }) { row ->
                        PeriodRowItem(row, dateFmt) {
                            scope.launch { container.userProfileRepository.removePeriodStart(row.id) }
                        }
                    }
                }
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PeriodRowItem(
    row: PeriodStartEntity,
    dateFmt: DateTimeFormatter,
    onDelete: () -> Unit
) {
    val d = LocalDate.parse(row.startDate)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(d.format(dateFmt), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить")
        }
    }
}
