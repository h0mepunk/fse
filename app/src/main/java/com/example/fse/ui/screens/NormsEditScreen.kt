package com.example.fse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.Nutrient
import kotlinx.coroutines.launch

private val MICRONUTRIENTS = listOf(
    Nutrient.Fiber,
    Nutrient.Sodium,
    Nutrient.Calcium,
    Nutrient.Iron,
    Nutrient.VitaminA,
    Nutrient.VitaminC,
    Nutrient.VitaminD,
    Nutrient.Potassium
)

private val KBJU_NUTRIENTS = listOf(
    Nutrient.Calories,
    Nutrient.Protein,
    Nutrient.Carbs,
    Nutrient.Fat
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormsEditScreen(
    container: AppContainer,
    navController: NavController
) {
    val customNorms by container.normsStore.getAllCustomNorms().collectAsState(initial = emptyMap())
    val allEditable = MICRONUTRIENTS + KBJU_NUTRIENTS
    var values by remember {
        mutableStateOf(
            allEditable.associateWith { n ->
                customNorms[n]?.toString() ?: n.dailyRecommended.toInt().toString()
            }
        )
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(customNorms) {
        values = allEditable.associateWith { n ->
            customNorms[n]?.toString() ?: n.dailyRecommended.toInt().toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit daily norms") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Micronutrients (shown in Norms)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MICRONUTRIENTS.forEach { nutrient ->
                OutlinedTextField(
                    value = values[nutrient] ?: nutrient.dailyRecommended.toInt().toString(),
                    onValueChange = { values = values + (nutrient to it) },
                    label = { Text("${nutrient.displayName} (${nutrient.unit})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Text(
                "Daily goals (shown in Diary)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            KBJU_NUTRIENTS.forEach { nutrient ->
                OutlinedTextField(
                    value = values[nutrient] ?: nutrient.dailyRecommended.toInt().toString(),
                    onValueChange = { values = values + (nutrient to it) },
                    label = { Text("${nutrient.displayName} (${nutrient.unit})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    scope.launch {
                        container.normsStore.resetToDefaults()
                        values = allEditable.associateWith { n ->
                            n.dailyRecommended.toInt().toString()
                        }
                    }
                }) {
                    Text("Reset to defaults")
                }
                androidx.compose.material3.Button(
                    onClick = {
                        scope.launch {
                            allEditable.forEach { n ->
                                val v = (values[n] ?: "").toDoubleOrNull() ?: n.dailyRecommended
                                container.normsStore.setCustomNorm(n, v)
                            }
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
