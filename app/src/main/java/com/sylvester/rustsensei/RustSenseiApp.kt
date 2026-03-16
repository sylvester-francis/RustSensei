package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.MainScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Main : Screen("main")
    data object Settings : Screen("settings")
}

@Composable
fun RustSenseiApp() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val modelViewModel: ModelViewModel = hiltViewModel()
    val bookViewModel: BookViewModel = hiltViewModel()
    val exerciseViewModel: ExerciseViewModel = hiltViewModel()
    val progressViewModel: ProgressViewModel = hiltViewModel()
    val referenceViewModel: ReferenceViewModel = hiltViewModel()

    // Start directly at Main — all non-AI features work without a model.
    // The Chat tab gracefully handles missing model with a download prompt.
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Setup.route) {
            ModelSetupScreen(
                modelViewModel = modelViewModel,
                liteRtEngine = chatViewModel.liteRtEngine,
                onNavigateToChat = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            // No model gate — MainScreen always renders.
            // AI features check model state themselves.
            MainScreen(
                chatViewModel = chatViewModel,
                bookViewModel = bookViewModel,
                exerciseViewModel = exerciseViewModel,
                progressViewModel = progressViewModel,
                referenceViewModel = referenceViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                chatViewModel = chatViewModel,
                modelViewModel = modelViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
