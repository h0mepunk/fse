package com.example.fse.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Info
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fse.di.AppContainer
import com.example.fse.domain.model.MealType
import com.example.fse.ui.screens.DiaryScreen
import com.example.fse.ui.screens.FavoritesScreen
import com.example.fse.ui.screens.AuthScreen
import com.example.fse.ui.screens.MealsScreen
import com.example.fse.ui.screens.NormsEditScreen
import com.example.fse.ui.screens.NormsScreen
import com.example.fse.ui.screens.CookbookScreen
import com.example.fse.ui.screens.SearchFoodScreen
import com.example.fse.ui.theme.FSETheme

enum class AppDestination(val route: String) {
    Diary("diary"),
    SearchFood("search_food"),
    Cookbook("cookbook"),
    Meals("meals"),
    Norms("norms"),
    NormsEdit("norms_edit"),
    Auth("auth")
}

private fun parseMealType(value: String?): MealType? = when (value?.lowercase()) {
    "breakfast" -> MealType.Breakfast
    "lunch" -> MealType.Lunch
    "dinner" -> MealType.Dinner
    "snack" -> MealType.Snack
    "other" -> MealType.Other
    else -> null
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
                            icon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search", style = labelStyle) },
                            selected = currentRoute == AppDestination.SearchFood.route,
                            onClick = { navController.navigate(AppDestination.SearchFood.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = null) },
                            label = { Text("Cookbook", style = labelStyle) },
                            selected = currentRoute == AppDestination.Cookbook.route,
                            onClick = { navController.navigate(AppDestination.Cookbook.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                            label = { Text("Meals", style = labelStyle) },
                            selected = currentRoute == AppDestination.Meals.route,
                            onClick = { navController.navigate(AppDestination.Meals.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            label = { Text("Norms", style = labelStyle) },
                            selected = currentRoute == AppDestination.Norms.route,
                            onClick = { navController.navigate(AppDestination.Norms.route) }
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
                            route = "${AppDestination.SearchFood.route}?mealType={mealType}",
                            arguments = listOf(
                                navArgument("mealType") {
                                    defaultValue = "breakfast"
                                    nullable = false
                                }
                            )
                        ) { backStack ->
                            SearchFoodScreen(
                                container = container,
                                navController = navController,
                                initialMealType = parseMealType(backStack.arguments?.getString("mealType"))
                                    ?: MealType.Breakfast
                            )
                        }
                        composable(AppDestination.Cookbook.route) {
                            CookbookScreen(container = container)
                        }
                        composable(AppDestination.Meals.route) {
                            MealsScreen(container = container, navController = navController)
                        }
                    composable(AppDestination.Norms.route) {
                        NormsScreen(container = container, navController = navController)
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
