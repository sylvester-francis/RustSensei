package com.sylvester.rustsensei

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.screens.DailyChallengeScreen
import com.sylvester.rustsensei.ui.screens.DocsScreen
import com.sylvester.rustsensei.ui.screens.ExplainErrorScreen
import com.sylvester.rustsensei.ui.screens.LearningPathScreen
import com.sylvester.rustsensei.ui.screens.MainScreen
import com.sylvester.rustsensei.ui.screens.ModelSetupScreen
import com.sylvester.rustsensei.ui.screens.PlaygroundScreen
import com.sylvester.rustsensei.ui.screens.ProjectScreen
import com.sylvester.rustsensei.ui.screens.RefactoringScreen
import com.sylvester.rustsensei.ui.screens.VisualizerScreen
import com.sylvester.rustsensei.ui.screens.QuizScreen
import com.sylvester.rustsensei.ui.screens.ReviewScreen
import com.sylvester.rustsensei.ui.screens.SearchScreen
import com.sylvester.rustsensei.ui.screens.SettingsScreen
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.work.ReminderScheduler
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.DailyChallengeViewModel
import com.sylvester.rustsensei.viewmodel.DocsViewModel
import com.sylvester.rustsensei.viewmodel.ExplainErrorViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.QuizViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.PlaygroundViewModel
import com.sylvester.rustsensei.viewmodel.ProjectViewModel
import com.sylvester.rustsensei.viewmodel.RefactoringViewModel
import com.sylvester.rustsensei.viewmodel.VisualizerViewModel
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
@Serializable object ExplainErrorRoute
@Serializable object DailyChallengeRoute
@Serializable object PlaygroundRoute
@Serializable object VisualizerRoute
@Serializable object ProjectRoute
@Serializable object RefactoringRoute
@Serializable object DocsRoute

@Composable
fun RustSenseiApp(
    preferencesManager: PreferencesManager,
    reminderScheduler: ReminderScheduler
) {
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
    val explainErrorViewModel: ExplainErrorViewModel = hiltViewModel()
    val dailyChallengeViewModel: DailyChallengeViewModel = hiltViewModel()
    val playgroundViewModel: PlaygroundViewModel = hiltViewModel()
    val refactoringViewModel: RefactoringViewModel = hiltViewModel()
    val visualizerViewModel: VisualizerViewModel = hiltViewModel()
    val projectViewModel: ProjectViewModel = hiltViewModel()
    val docsViewModel: DocsViewModel = hiltViewModel()

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
                preferencesManager = preferencesManager,
                reminderScheduler = reminderScheduler,
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToSetup = { navController.navigate(SetupRoute) },
                onNavigateToReview = { navController.navigate(ReviewRoute) },
                onNavigateToLearningPaths = { navController.navigate(LearningPathRoute) },
                onNavigateToQuiz = { navController.navigate(QuizRoute) },
                onNavigateToSearch = { navController.navigate(SearchRoute) },
                onNavigateToExplainError = { navController.navigate(ExplainErrorRoute) },
                onNavigateToDailyChallenge = { navController.navigate(DailyChallengeRoute) },
                onNavigateToPlayground = { navController.navigate(PlaygroundRoute) },
                onNavigateToRefactoring = { navController.navigate(RefactoringRoute) },
                onNavigateToDocs = { navController.navigate(DocsRoute) },
                onNavigateToVisualizer = { navController.navigate(VisualizerRoute) },
                onNavigateToProjects = { navController.navigate(ProjectRoute) }
            )
        }

        composable<RefactoringRoute> {
            RefactoringScreen(
                viewModel = refactoringViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DocsRoute> {
            DocsScreen(
                viewModel = docsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ExplainErrorRoute> {
            ExplainErrorScreen(
                viewModel = explainErrorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DailyChallengeRoute> {
            DailyChallengeScreen(
                viewModel = dailyChallengeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<PlaygroundRoute> {
            PlaygroundScreen(
                viewModel = playgroundViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<VisualizerRoute> {
            VisualizerScreen(
                viewModel = visualizerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ProjectRoute> {
            ProjectScreen(
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() }
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
            QuizScreen(
                viewModel = quizViewModel,
                onNavigateToLearningPaths = { navController.navigate(LearningPathRoute) }
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            var remindersEnabled by remember {
                mutableStateOf(preferencesManager.areRemindersEnabled())
            }
            val context = LocalContext.current

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    remindersEnabled = true
                    preferencesManager.setRemindersEnabled(true)
                    reminderScheduler.scheduleReminders()
                } else {
                    remindersEnabled = false
                    preferencesManager.setRemindersEnabled(false)
                }
            }

            SettingsScreen(
                chatViewModel = chatViewModel,
                modelViewModel = modelViewModel,
                onNavigateBack = { navController.popBackStack() },
                remindersEnabled = remindersEnabled,
                onRemindersToggled = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            remindersEnabled = true
                            preferencesManager.setRemindersEnabled(true)
                            reminderScheduler.scheduleReminders()
                        }
                    } else {
                        remindersEnabled = false
                        preferencesManager.setRemindersEnabled(false)
                        reminderScheduler.cancelReminders()
                    }
                }
            )
        }
    }
}
