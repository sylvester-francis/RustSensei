package com.sylvester.rustsensei.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatContext
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// ── Type-safe tab route definitions ─────────────────────────────────

@Serializable object HomeRoute
@Serializable object LearnRoute
@Serializable object ChatTabRoute
@Serializable object PracticeRoute
@Serializable object ProfileRoute

enum class Tab(val title: String, val icon: ImageVector, val routeClass: KClass<*>) {
    Home("Home", Icons.Default.Home, HomeRoute::class),
    Learn("Learn", Icons.Default.MenuBook, LearnRoute::class),
    Chat("Chat", Icons.Default.Chat, ChatTabRoute::class),
    Practice("Practice", Icons.Default.Code, PracticeRoute::class),
    Profile("Settings", Icons.Default.Settings, ProfileRoute::class);
}

/** Navigate to a tab with standard bottom-nav options (save/restore state, single top). */
private fun NavController.navigateToTab(tab: Tab) {
    val navOpts: androidx.navigation.NavOptionsBuilder.() -> Unit = {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (tab) {
        Tab.Home -> navigate(HomeRoute, navOpts)
        Tab.Learn -> navigate(LearnRoute, navOpts)
        Tab.Chat -> navigate(ChatTabRoute, navOpts)
        Tab.Practice -> navigate(PracticeRoute, navOpts)
        Tab.Profile -> navigate(ProfileRoute, navOpts)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    bookViewModel: BookViewModel,
    exerciseViewModel: ExerciseViewModel,
    progressViewModel: ProgressViewModel,
    referenceViewModel: ReferenceViewModel,
    reviewViewModel: ReviewViewModel,
    learningPathViewModel: LearningPathViewModel,
    modelViewModel: ModelViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToLearningPaths: () -> Unit = {},
    onNavigateToQuiz: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Switch tab when returning from Learning Paths with content loaded
    val requestedTab by learningPathViewModel.requestedTab.collectAsState()
    LaunchedEffect(requestedTab) {
        val tab = requestedTab ?: return@LaunchedEffect
        when (tab) {
            "learn" -> tabNavController.navigateToTab(Tab.Learn)
            "practice" -> tabNavController.navigateToTab(Tab.Practice)
        }
        learningPathViewModel.clearTabRequest()
    }

    // Mark learning path step complete when the opened content is actually finished
    val pendingStep by learningPathViewModel.pendingStep.collectAsState()
    val bookUiState by bookViewModel.uiState.collectAsState()
    val exerciseUiState by exerciseViewModel.uiState.collectAsState()
    val reviewUiState by reviewViewModel.uiState.collectAsState()

    LaunchedEffect(bookUiState.sectionMarkedComplete) {
        val pending = pendingStep
        if (bookUiState.sectionMarkedComplete && pending != null && pending.type == "read" &&
            bookUiState.currentChapterId == pending.targetId) {
            learningPathViewModel.completePendingStep()
        }
    }

    LaunchedEffect(exerciseUiState.checkResult) {
        val pending = pendingStep
        if (exerciseUiState.checkResult == "correct" && pending != null && pending.type == "exercise" &&
            exerciseUiState.currentExercise?.id == pending.targetId) {
            learningPathViewModel.completePendingStep()
        }
    }

    LaunchedEffect(reviewUiState.sessionComplete) {
        val pending = pendingStep
        if (reviewUiState.sessionComplete && pending != null && pending.type == "review") {
            learningPathViewModel.completePendingStep()
        }
    }

    val isChatActive = currentDestination?.hasRoute<ChatTabRoute>() == true
    val hideTopBar = isChatActive || currentDestination?.hasRoute<ProfileRoute>() == true

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                "RustSensei",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER))
                    )
                }
            }
        },
        bottomBar = {
            if (!isChatActive) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER))
                    )
                    RustSenseiNavigationBar(
                        onTabSelected = { tab -> tabNavController.navigateToTab(tab) },
                        currentDestination = currentDestination
                    )
                }
            }
        }
    ) { innerPadding ->
        val tabEnterTransition = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + slideInVertically(
            initialOffsetY = { 24 },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
        val tabExitTransition = fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + slideOutVertically(
            targetOffsetY = { -24 },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )

        NavHost(
            navController = tabNavController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            composable<HomeRoute> {
                DashboardScreen(
                    viewModel = progressViewModel,
                    reviewViewModel = reviewViewModel,
                    learningPathViewModel = learningPathViewModel,
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToLearningPaths = onNavigateToLearningPaths,
                    onNavigateToQuiz = onNavigateToQuiz,
                    onNavigateToLearn = { tabNavController.navigateToTab(Tab.Learn) },
                    onNavigateToExercises = { tabNavController.navigateToTab(Tab.Practice) },
                    onContinueReading = { chapterId, sectionId ->
                        bookViewModel.openChapter(chapterId)
                        bookViewModel.openSection(chapterId, sectionId)
                        tabNavController.navigateToTab(Tab.Learn)
                    },
                    onContinueExercise = { exerciseId ->
                        exerciseViewModel.openExercise(exerciseId)
                        tabNavController.navigateToTab(Tab.Practice)
                    }
                )
            }
            composable<LearnRoute> {
                BookScreen(
                    viewModel = bookViewModel,
                    referenceViewModel = referenceViewModel,
                    reviewViewModel = reviewViewModel,
                    learningPathViewModel = learningPathViewModel,
                    onOpenReference = { sectionId -> referenceViewModel.openSection(sectionId) },
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToLearningPaths = onNavigateToLearningPaths,
                    onAskSensei = { sectionContent, _ ->
                        chatViewModel.setChatContext(
                            ChatContext.BookSection(
                                sectionId = bookViewModel.getCurrentSectionId(),
                                content = sectionContent
                            )
                        )
                        tabNavController.navigateToTab(Tab.Chat)
                    }
                )
            }
            composable<ChatTabRoute> {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSetup = onNavigateToSetup,
                    onNavigateBack = { tabNavController.navigateToTab(Tab.Home) }
                )
            }
            composable<PracticeRoute> {
                ExercisesScreen(
                    viewModel = exerciseViewModel,
                    onAskSensei = { exerciseDesc, userCode ->
                        chatViewModel.setChatContext(
                            ChatContext.Exercise(
                                exerciseId = exerciseViewModel.getExerciseId(),
                                description = exerciseDesc,
                                userCode = userCode
                            )
                        )
                        tabNavController.navigateToTab(Tab.Chat)
                    },
                    onNavigateToQuiz = onNavigateToQuiz
                )
            }
            composable<ProfileRoute> {
                SettingsScreen(
                    chatViewModel = chatViewModel,
                    modelViewModel = modelViewModel,
                    onNavigateBack = { tabNavController.navigateToTab(Tab.Home) }
                )
            }
        }
    }
}

@Composable
private fun RustSenseiNavigationBar(
    onTabSelected: (Tab) -> Unit,
    currentDestination: NavDestination?
) {
    val inactiveColor = Color(0xFF8B95A5)
    val activeIconColor = MaterialTheme.colorScheme.onPrimary
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.BottomBarHeight)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = currentDestination?.hasRoute(tab.routeClass) == true

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (isSelected) Modifier.background(primaryColor)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) activeIconColor else inactiveColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) primaryColor else inactiveColor
                        )
                    }
                }
            }
        }
    }
}
