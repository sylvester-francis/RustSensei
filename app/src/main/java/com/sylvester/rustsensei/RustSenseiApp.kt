package com.sylvester.rustsensei

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlinx.serialization.Serializable

// ── Type-safe route definitions ─────────────────────────────────────
// Each @Serializable object is a compile-time verified route.
// No more stringly-typed navigation — typos are caught at build time.

@Serializable object SetupRoute
@Serializable object MainRoute
@Serializable object SettingsRoute
@Serializable object ReviewRoute
@Serializable object LearningPathRoute
@Serializable object QuizRoute
@Serializable object SearchRoute

@Composable
fun RustSenseiApp() {
    val navController = rememberNavController()

    val chatViewModel: ChatViewModel = hiltViewModel()
    val modelViewModel: ModelViewModel = hiltViewModel()
    val bookViewModel: BookViewModel = hiltViewModel()
    val exerciseViewModel: ExerciseViewModel = hiltViewModel()
    val progressViewModel: ProgressViewModel = hiltViewModel()
    val referenceViewModel: ReferenceViewModel = hiltViewModel()
    val reviewViewModel: ReviewViewModel = hiltViewModel()
    val learningPathViewModel: LearningPathViewModel = hiltViewModel()
    val quizViewModel: QuizViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = MainRoute
    ) {
        composable<SetupRoute> {
            ModelSetupScreen(
                modelViewModel = modelViewModel,
                onNavigateToChat = {
                    navController.navigate(MainRoute) {
                        popUpTo<SetupRoute> { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(MainRoute) {
                        popUpTo<SetupRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<MainRoute> {
            MainScreen(
                chatViewModel = chatViewModel,
                bookViewModel = bookViewModel,
                exerciseViewModel = exerciseViewModel,
                progressViewModel = progressViewModel,
                referenceViewModel = referenceViewModel,
                reviewViewModel = reviewViewModel,
                learningPathViewModel = learningPathViewModel,
                modelViewModel = modelViewModel,
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToSetup = { navController.navigate(SetupRoute) },
                onNavigateToReview = { navController.navigate(ReviewRoute) },
                onNavigateToLearningPaths = { navController.navigate(LearningPathRoute) },
                onNavigateToQuiz = { navController.navigate(QuizRoute) },
                onNavigateToSearch = { navController.navigate(SearchRoute) }
            )
        }

        composable<ReviewRoute> {
            val reviewUiState by reviewViewModel.uiState.collectAsState()
            val pendingStep by learningPathViewModel.pendingStep.collectAsState()

            LaunchedEffect(reviewUiState.sessionComplete) {
                if (reviewUiState.sessionComplete && pendingStep != null && pendingStep?.type == "review") {
                    learningPathViewModel.completePendingStep()
                }
            }

            ReviewScreen(
                viewModel = reviewViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<LearningPathRoute> {
            LearningPathScreen(
                viewModel = learningPathViewModel,
                onNavigateBack = { navController.popBackStack() },
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
                    navController.navigate(ReviewRoute) {
                        popUpTo<LearningPathRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<QuizRoute> {
            QuizScreen(viewModel = quizViewModel)
        }

        composable<SearchRoute> {
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                chatViewModel = chatViewModel,
                modelViewModel = modelViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
