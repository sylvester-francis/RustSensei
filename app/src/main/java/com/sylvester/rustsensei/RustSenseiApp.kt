package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.ChatScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import kotlinx.coroutines.launch

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
    val modelUiState by modelViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var hasTriedAutoLoad by remember { mutableStateOf(false) }

    // Determine start destination
    val startDestination = if (modelViewModel.modelManager.isModelDownloaded()) {
        Screen.Setup.route // Still go to setup to show loading
    } else {
        Screen.Setup.route
    }

    // Auto-load model if already downloaded
    LaunchedEffect(modelUiState.modelState) {
        if (modelUiState.modelState == ModelState.DOWNLOADED && !hasTriedAutoLoad) {
            hasTriedAutoLoad = true
            modelViewModel.setModelLoading()
            scope.launch {
                val modelPath = modelViewModel.modelManager.modelFile.absolutePath
                val success = chatViewModel.loadModel(modelPath)
                if (success) {
                    modelViewModel.setModelReady()
                    // Auto-navigate to chat
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                } else {
                    modelViewModel.setModelError("Failed to load model")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            ModelSetupScreen(
                viewModel = modelViewModel,
                onModelReady = {
                    if (chatViewModel.isModelLoaded) {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    } else {
                        scope.launch {
                            modelViewModel.setModelLoading()
                            val modelPath = modelViewModel.modelManager.modelFile.absolutePath
                            val success = chatViewModel.loadModel(modelPath)
                            if (success) {
                                modelViewModel.setModelReady()
                                navController.navigate(Screen.Chat.route) {
                                    popUpTo(Screen.Setup.route) { inclusive = true }
                                }
                            } else {
                                modelViewModel.setModelError("Failed to load model")
                            }
                        }
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
