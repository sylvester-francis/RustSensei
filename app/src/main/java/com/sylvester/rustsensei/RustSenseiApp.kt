package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.ChatScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
}

@Composable
fun RustSenseiApp() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val modelViewModel: ModelViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route
    ) {
        composable(Screen.Setup.route) {
            ModelSetupScreen(
                modelViewModel = modelViewModel,
                llamaEngine = chatViewModel.llamaEngine,
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
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
