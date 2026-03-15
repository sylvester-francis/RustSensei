package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.MainScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Main : Screen("main")
    data object Settings : Screen("settings")
}

@Composable
fun RustSenseiApp() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val modelViewModel: ModelViewModel = viewModel()
    val bookViewModel: BookViewModel = viewModel()
    val exerciseViewModel: ExerciseViewModel = viewModel()
    val progressViewModel: ProgressViewModel = viewModel()

    // P0 Fix #1: Process death guard — if we're on Main but model isn't loaded,
    // redirect back to Setup. This handles the case where Android kills the process
    // and NavController restores to Screen.Main but native model state is gone.
    val modelState by modelViewModel.uiState.collectAsState()
    LaunchedEffect(modelState.modelState) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute == Screen.Main.route &&
            modelState.modelState != ModelState.READY &&
            !chatViewModel.llamaEngine.isModelLoaded()
        ) {
            // Model was lost (process death). Redirect to setup to reload.
            modelViewModel.checkModelStatus()
            navController.navigate(Screen.Setup.route) {
                popUpTo(Screen.Main.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route
    ) {
        composable(Screen.Setup.route) {
            ModelSetupScreen(
                modelViewModel = modelViewModel,
                llamaEngine = chatViewModel.llamaEngine,
                onNavigateToChat = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            // P0 Fix #1: double-check model is loaded before showing main screen
            if (!chatViewModel.llamaEngine.isModelLoaded()) {
                LaunchedEffect(Unit) {
                    modelViewModel.checkModelStatus()
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
                return@composable
            }

            MainScreen(
                chatViewModel = chatViewModel,
                bookViewModel = bookViewModel,
                exerciseViewModel = exerciseViewModel,
                progressViewModel = progressViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
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
