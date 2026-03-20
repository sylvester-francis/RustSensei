package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.LearningPathScreen
import com.sylvester.rustsensei.ui.screens.MainScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.QuizScreen
import com.sylvester.rustsensei.ui.screens.ReviewScreen
import com.sylvester.rustsensei.ui.screens.SearchScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.QuizViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import com.sylvester.rustsensei.viewmodel.SearchViewModel

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Main : Screen("main")
    data object Settings : Screen("settings")
    data object Review : Screen("review")
    data object LearningPath : Screen("learning_path")
    data object Quiz : Screen("quiz")
    data object Search : Screen("search")
}

@Composable
fun RustSenseiApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as RustSenseiApplication
    val factory = remember { AppViewModelFactory(app, app.container) }

    val chatViewModel: ChatViewModel = viewModel(factory = factory)
    val modelViewModel: ModelViewModel = viewModel(factory = factory)
    val bookViewModel: BookViewModel = viewModel(factory = factory)
    val exerciseViewModel: ExerciseViewModel = viewModel(factory = factory)
    val progressViewModel: ProgressViewModel = viewModel(factory = factory)
    val referenceViewModel: ReferenceViewModel = viewModel(factory = factory)
    val reviewViewModel: ReviewViewModel = viewModel(factory = factory)
    val learningPathViewModel: LearningPathViewModel = viewModel(factory = factory)
    val quizViewModel: QuizViewModel = viewModel(factory = factory)
    val searchViewModel: SearchViewModel = viewModel(factory = factory)

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
            MainScreen(
                chatViewModel = chatViewModel,
                bookViewModel = bookViewModel,
                exerciseViewModel = exerciseViewModel,
                progressViewModel = progressViewModel,
                referenceViewModel = referenceViewModel,
                reviewViewModel = reviewViewModel,
                learningPathViewModel = learningPathViewModel,
                modelViewModel = modelViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route)
                },
                onNavigateToReview = {
                    navController.navigate(Screen.Review.route)
                },
                onNavigateToLearningPaths = {
                    navController.navigate(Screen.LearningPath.route)
                },
                onNavigateToQuiz = {
                    navController.navigate(Screen.Quiz.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Review.route) {
            ReviewScreen(
                viewModel = reviewViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LearningPath.route) {
            LearningPathScreen(
                viewModel = learningPathViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChapter = { chapterId ->
                    bookViewModel.openChapter(chapterId)
                    learningPathViewModel.requestTabNavigation("learn")
                    navController.popBackStack()
                },
                onNavigateToExercise = { exerciseId ->
                    exerciseViewModel.openExercise(exerciseId)
                    learningPathViewModel.requestTabNavigation("practice")
                    navController.popBackStack()
                },
                onNavigateToReview = {
                    navController.navigate(Screen.Review.route) {
                        popUpTo(Screen.LearningPath.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Quiz.route) {
            QuizScreen(viewModel = quizViewModel)
        }

        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = {
                    navController.popBackStack()
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
