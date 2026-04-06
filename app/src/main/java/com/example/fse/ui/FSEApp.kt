package com.example.fse.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.MealType
import com.example.fse.ui.screens.DiaryScreen
import com.example.fse.ui.screens.FavoritesScreen
import com.example.fse.ui.screens.AuthScreen
import com.example.fse.ui.screens.CookbookTemplatesScreen
import com.example.fse.ui.screens.MealsTabScreen
import com.example.fse.ui.screens.NormsEditScreen
import com.example.fse.ui.screens.SearchFoodScreen
import com.example.fse.ui.theme.FSETheme

enum class AppDestination(val route: String) {
    Diary("diary"),
    SearchFood("search_food/{diaryDate}"),
    SearchFoodTemplate("search_food_template/{templateId}"),
    SearchFoodSavedMeal("search_food_saved_meal/{savedMealId}"),
    Cookbook("cookbook"),
    Meals("meals"),
    Favorites("favorites"),
    NormsEdit("norms_edit"),
    Auth("auth");

    companion object {
        /** Concrete path for [androidx.navigation.NavController.navigate]. */
        fun searchFoodPath(date: LocalDate) = "search_food/$date"

        /** Opens food search with the serving picker defaulting to this meal type. */
        fun searchFoodPath(date: LocalDate, mealType: MealType) = "search_food/$date/${mealType.name}"

        fun searchFoodTemplatePath(templateId: Long) = "search_food_template/$templateId"

        fun searchFoodSavedMealPath(savedMealId: String) = "search_food_saved_meal/$savedMealId"
    }
}

@PreviewScreenSizes
@Composable
fun FSEApp(container: AppContainer?) {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    FSETheme {
        if (container != null) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    val labelStyle = MaterialTheme.typography.labelSmall
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Diary", style = labelStyle) },
                            selected = currentRoute == AppDestination.Diary.route,
                            onClick = { navController.navigate(AppDestination.Diary.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                            label = { Text("Cookbook", style = labelStyle) },
                            selected = currentRoute == AppDestination.Cookbook.route,
                            onClick = { navController.navigate(AppDestination.Cookbook.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = null) },
                            label = { Text("Meals", style = labelStyle) },
                            selected = currentRoute == AppDestination.Meals.route,
                            onClick = { navController.navigate(AppDestination.Meals.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            label = { Text("Fav", style = labelStyle) },
                            selected = currentRoute == AppDestination.Favorites.route,
                            onClick = { navController.navigate(AppDestination.Favorites.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            label = { Text("Acc", style = labelStyle) },
                            selected = currentRoute == AppDestination.Auth.route,
                            onClick = { navController.navigate(AppDestination.Auth.route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Diary.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                        composable(AppDestination.Diary.route) {
                            DiaryScreen(container = container, navController = navController)
                        }
                        composable(
                            route = "search_food/{diaryDate}/{mealType}",
                            arguments = listOf(
                                navArgument("diaryDate") { type = NavType.StringType },
                                navArgument("mealType") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val raw = entry.arguments?.getString("diaryDate")
                            val diaryDate = raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                                ?: LocalDate.now()
                            val mealName = entry.arguments?.getString("mealType")
                            val presetMealType = mealName?.let { runCatching { MealType.valueOf(it) }.getOrNull() }
                            SearchFoodScreen(
                                container = container,
                                navController = navController,
                                diaryDate = diaryDate,
                                templateId = null,
                                presetMealType = presetMealType
                            )
                        }
                        composable(
                            route = AppDestination.SearchFood.route,
                            arguments = listOf(
                                navArgument("diaryDate") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val raw = entry.arguments?.getString("diaryDate")
                            val diaryDate = raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                                ?: LocalDate.now()
                            SearchFoodScreen(
                                container = container,
                                navController = navController,
                                diaryDate = diaryDate,
                                templateId = null,
                                presetMealType = null
                            )
                        }
                        composable(
                            route = AppDestination.SearchFoodTemplate.route,
                            arguments = listOf(
                                navArgument("templateId") { type = NavType.LongType }
                            )
                        ) { entry ->
                            val templateId = entry.arguments?.getLong("templateId") ?: return@composable
                            SearchFoodScreen(
                                container = container,
                                navController = navController,
                                diaryDate = LocalDate.now(),
                                templateId = templateId
                            )
                        }
                        composable(
                            route = AppDestination.SearchFoodSavedMeal.route,
                            arguments = listOf(
                                navArgument("savedMealId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val savedMealId = entry.arguments?.getString("savedMealId") ?: return@composable
                            SearchFoodScreen(
                                container = container,
                                navController = navController,
                                diaryDate = LocalDate.now(),
                                savedMealId = savedMealId
                            )
                        }
                        composable(AppDestination.Cookbook.route) {
                            CookbookTemplatesScreen(container = container, navController = navController)
                        }
                        composable(AppDestination.Meals.route) {
                            MealsTabScreen(container = container, navController = navController)
                        }
                        composable(AppDestination.Favorites.route) {
                            FavoritesScreen(container = container, navController = navController)
                        }
                    composable(AppDestination.NormsEdit.route) {
                        NormsEditScreen(container = container, navController = navController)
                    }
                    composable(AppDestination.Auth.route) {
                        AuthScreen(container = container)
                    }
                }
            }
        } else {
            Text("Loading...", modifier = Modifier.padding(16.dp))
        }
    }
}
