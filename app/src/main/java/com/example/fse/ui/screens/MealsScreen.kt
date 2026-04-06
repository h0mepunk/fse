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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.MealType
import com.example.fse.domain.model.PendingMealTemplateFromDiary
import com.example.fse.domain.model.UserMealTemplate
import com.example.fse.domain.model.UserMealTemplateLine
import com.example.fse.ui.AppDestination
import com.example.fse.ui.format.extractGrams
import com.example.fse.ui.format.formatAmountNumber
import com.example.fse.ui.format.formatEntryAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Cookbook tab: locally saved meal templates (from diary or built here).
 */
@Composable
fun CookbookTemplatesScreen(container: AppContainer, navController: NavController) {
    val templates by container.savedMealTemplateRepository.observeTemplates()
        .collectAsState(initial = emptyList())
    val expandedMap = remember { mutableStateMapOf<Long, Boolean>() }
    var mealPickTemplateId by remember { mutableStateOf<Long?>(null) }
    var nameDialogPending by remember { mutableStateOf<PendingMealTemplateFromDiary?>(null) }
    var renameTarget by remember { mutableStateOf<UserMealTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<UserMealTemplate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingTemplateId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(templates.map { it.id }) {
        val ids = templates.map { it.id }.toSet()
        expandedMap.keys.retainAll(ids)
        templates.forEach { expandedMap.putIfAbsent(it.id, true) }
    }

    val pendingOffer by container.pendingMealTemplateFromDiary.collectAsState(initial = null)
    LaunchedEffect(pendingOffer) {
        val p = pendingOffer ?: return@LaunchedEffect
        container.clearMealTemplateFromDiary()
        nameDialogPending = p
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Шаблоны приёмов", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        if (templates.isEmpty()) {
            Text(
                "Долгое нажатие на завтрак, обед или другой приём в дневнике — сохранить набор продуктов как шаблон. Здесь можно править граммы и состав.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    val expanded = expandedMap[template.id] != false
                    TemplateMealCard(
                        template = template,
                        expanded = expanded,
                        editing = template.id == editingTemplateId,
                        onToggleExpand = {
                            expandedMap[template.id] = !expanded
                        },
                        onToggleEdit = {
                            editingTemplateId =
                                if (editingTemplateId == template.id) null else template.id
                        },
                        onAddToDiary = { mealPickTemplateId = template.id },
                        onRename = { renameTarget = template },
                        onDeleteTemplate = { deleteTarget = template },
                        onAddIngredient = {
                            navController.navigate(AppDestination.searchFoodTemplatePath(template.id))
                        },
                        onApplyGrams = { line, text ->
                            scope.launch {
                                error = null
                                val r = withContext(Dispatchers.IO) {
                                    container.savedMealTemplateRepository.updateLineGrams(line, text)
                                }
                                r.onFailure { error = it.message }
                            }
                        },
                        onApplyMultiplier = { line, text ->
                            scope.launch {
                                error = null
                                val r = withContext(Dispatchers.IO) {
                                    container.savedMealTemplateRepository.updateLineMultiplier(line, text)
                                }
                                r.onFailure { error = it.message }
                            }
                        },
                        onDeleteLine = { line ->
                            scope.launch {
                                error = null
                                withContext(Dispatchers.IO) {
                                    container.savedMealTemplateRepository.deleteLine(line.id, line.templateId)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    nameDialogPending?.let { pending ->
        var nameText by remember(pending) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { nameDialogPending = null },
            title = { Text("Название приёма пищи") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Например: обычный завтрак") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameText.isBlank()) return@TextButton
                        scope.launch {
                            error = null
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    container.savedMealTemplateRepository.createFromDiary(
                                        nameText.trim(),
                                        pending.entries
                                    )
                                }
                            }.onSuccess { nameDialogPending = null }
                                .onFailure { error = it.message }
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { nameDialogPending = null }) { Text("Отмена") }
            }
        )
    }

    mealPickTemplateId?.let { tid ->
        AlertDialog(
            onDismissRequest = { mealPickTemplateId = null },
            title = { Text("Добавить в дневник на сегодня") },
            text = {
                Column {
                    Text(
                        "Выберите приём пищи — все продукты из шаблона будут добавлены.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    listOf(
                        MealType.Breakfast,
                        MealType.Lunch,
                        MealType.Dinner,
                        MealType.Snack
                    ).forEach { mt ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    error = null
                                    val r = withContext(Dispatchers.IO) {
                                        container.savedMealTemplateRepository.applyToDiary(
                                            tid,
                                            LocalDate.now(),
                                            mt
                                        )
                                    }
                                    r.onSuccess { mealPickTemplateId = null }
                                        .onFailure { error = it.message }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(mt.displayName) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mealPickTemplateId = null }) { Text("Закрыть") }
            }
        )
    }

    renameTarget?.let { t ->
        var nameText by remember(t.id) { mutableStateOf(t.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameText.isBlank()) return@TextButton
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                container.savedMealTemplateRepository.renameTemplate(t.id, nameText.trim())
                            }
                            renameTarget = null
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Отмена") } }
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить шаблон?") },
            text = { Text("«${t.name}»") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                container.savedMealTemplateRepository.deleteTemplate(t.id)
                            }
                            deleteTarget = null
                        }
                    }
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun TemplateMealCard(
    template: UserMealTemplate,
    expanded: Boolean,
    editing: Boolean,
    onToggleExpand: () -> Unit,
    onToggleEdit: () -> Unit,
    onAddToDiary: () -> Unit,
    onRename: () -> Unit,
    onDeleteTemplate: () -> Unit,
    onAddIngredient: () -> Unit,
    onApplyGrams: (UserMealTemplateLine, String) -> Unit,
    onApplyMultiplier: (UserMealTemplateLine, String) -> Unit,
    onDeleteLine: (UserMealTemplateLine) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddToDiary) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить в дневник")
                }
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = if (editing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }
            if (!expanded) {
                TemplateMacroRow(
                    p = template.totalProtein.toInt(),
                    fat = template.totalFat.toInt(),
                    carbs = template.totalCarbs.toInt(),
                    kcal = template.totalCalories.toInt(),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onRename) {
                                Text("Переименовать")
                            }
                            TextButton(onClick = onDeleteTemplate) {
                                Text("Удалить шаблон", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    template.lines.forEach { line ->
                        if (editing) {
                            TemplateLineEditor(
                                line = line,
                                onApplyGrams = onApplyGrams,
                                onApplyMultiplier = onApplyMultiplier,
                                onDeleteLine = onDeleteLine
                            )
                        } else {
                            TemplateLineReadOnly(line = line)
                        }
                    }
                    if (editing) {
                        Button(onClick = onAddIngredient, modifier = Modifier.fillMaxWidth()) {
                            Text("Добавить продукт")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateLineReadOnly(line: UserMealTemplateLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                line.foodName + (line.brandName?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatEntryAmount(line.servingDescription, line.multiplier),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        TemplateMacroRow(
            p = line.lineProtein.toInt(),
            fat = line.lineFat.toInt(),
            carbs = line.lineCarbs.toInt(),
            kcal = line.lineCalories.toInt(),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TemplateLineEditor(
    line: UserMealTemplateLine,
    onApplyGrams: (UserMealTemplateLine, String) -> Unit,
    onApplyMultiplier: (UserMealTemplateLine, String) -> Unit,
    onDeleteLine: (UserMealTemplateLine) -> Unit
) {
    val baseGrams = extractGrams(line.servingDescription)
    val gramsInitial = baseGrams?.let { formatAmountNumber(it * line.multiplier) } ?: ""
    var gramsText by remember(line.id, line.multiplier, line.servingDescription) {
        mutableStateOf(gramsInitial)
    }
    var multText by remember(line.id, line.multiplier) {
        mutableStateOf(formatAmountNumber(line.multiplier))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line.foodName + (line.brandName?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatEntryAmount(line.servingDescription, line.multiplier),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TemplateMacroRow(
                    p = line.lineProtein.toInt(),
                    fat = line.lineFat.toInt(),
                    carbs = line.lineCarbs.toInt(),
                    kcal = line.lineCalories.toInt(),
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (baseGrams != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = gramsText,
                            onValueChange = { gramsText = it },
                            label = { Text("Граммы") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onApplyGrams(line, gramsText) }) { Text("OK") }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = multText,
                            onValueChange = { multText = it },
                            label = { Text("Множитель порции") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onApplyMultiplier(line, multText) }) { Text("OK") }
                    }
                }
            }
            IconButton(onClick = { onDeleteLine(line) }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TemplateMacroRow(
    p: Int,
    fat: Int,
    carbs: Int,
    kcal: Int,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodySmall
    Row(modifier.fillMaxWidth()) {
        MacroCell("P", "${p}g", style, Modifier.weight(1f))
        MacroCell("F", "${fat}g", style, Modifier.weight(1f))
        MacroCell("C", "${carbs}g", style, Modifier.weight(1f))
        MacroCell("kcal", "$kcal", style, Modifier.weight(1f))
    }
}

@Composable
private fun MacroCell(label: String, value: String, valueStyle: androidx.compose.ui.text.TextStyle, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(value, style = valueStyle, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
