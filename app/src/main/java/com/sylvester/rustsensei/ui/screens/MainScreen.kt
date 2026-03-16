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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatContext
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel

sealed class Tab(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Default.Home)
    data object Learn : Tab("learn", "Learn", Icons.Default.MenuBook)
    data object Chat : Tab("chat", "Chat", Icons.Default.Chat)
    data object Practice : Tab("practice", "Practice", Icons.Default.Code)
    data object Profile : Tab("profile", "Settings", Icons.Default.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Learn, Tab.Chat, Tab.Practice, Tab.Profile)

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

    // Track selected index for the nav bar pill highlight
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Hide chrome when Chat is active (it has its own top bar and needs full screen)
    val isChatActive = currentDestination?.route == Tab.Chat.route
    val hideTopBar = isChatActive || currentDestination?.route == Tab.Profile.route

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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    )
                }
            }
        },
        bottomBar = {
            if (!isChatActive) {
                Column {
                    // 1dp neon top border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    )
                    RustSenseiNavigationBar(
                        tabs = tabs,
                        onTabSelected = { index, tab ->
                            selectedTabIndex = index
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        currentDestination = currentDestination
                    )
                }
            }
        }
    ) { innerPadding ->
        // Cross-fade tab transitions with 8dp vertical slide via enterTransition/exitTransition
        val tabEnterTransition = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + slideInVertically(
            initialOffsetY = { 24 }, // ~8dp in pixels
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
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            composable(Tab.Home.route) {
                DashboardScreen(
                    viewModel = progressViewModel,
                    reviewViewModel = reviewViewModel,
                    learningPathViewModel = learningPathViewModel,
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToLearningPaths = onNavigateToLearningPaths,
                    onNavigateToQuiz = onNavigateToQuiz,
                    onNavigateToLearn = {
                        tabNavController.navigate(Tab.Learn.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = tabs.indexOf(Tab.Learn)
                    },
                    onNavigateToExercises = {
                        tabNavController.navigate(Tab.Practice.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = tabs.indexOf(Tab.Practice)
                    },
                    onContinueReading = { chapterId, sectionId ->
                        bookViewModel.openChapter(chapterId)
                        bookViewModel.openSection(chapterId, sectionId)
                        tabNavController.navigate(Tab.Learn.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = tabs.indexOf(Tab.Learn)
                    },
                    onContinueExercise = { exerciseId ->
                        exerciseViewModel.openExercise(exerciseId)
                        tabNavController.navigate(Tab.Practice.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = tabs.indexOf(Tab.Practice)
                    }
                )
            }
            composable(Tab.Learn.route) {
                BookScreen(
                    viewModel = bookViewModel,
                    referenceViewModel = referenceViewModel,
                    reviewViewModel = reviewViewModel,
                    learningPathViewModel = learningPathViewModel,
                    onOpenReference = { sectionId ->
                        referenceViewModel.openSection(sectionId)
                    },
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToLearningPaths = onNavigateToLearningPaths,
                    onAskSensei = { sectionContent, _ ->
                        chatViewModel.setChatContext(
                            ChatContext.BookSection(
                                sectionId = bookViewModel.getCurrentSectionId(),
                                content = sectionContent
                            )
                        )
                        tabNavController.navigate(Tab.Chat.route)
                        selectedTabIndex = tabs.indexOf(Tab.Chat)
                    }
                )
            }
            composable(Tab.Chat.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSetup = onNavigateToSetup,
                    onNavigateBack = {
                        tabNavController.navigate(Tab.Home.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = 0
                    }
                )
            }
            composable(Tab.Practice.route) {
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
                        tabNavController.navigate(Tab.Chat.route)
                        selectedTabIndex = tabs.indexOf(Tab.Chat)
                    },
                    onNavigateToQuiz = onNavigateToQuiz
                )
            }
            composable(Tab.Profile.route) {
                SettingsScreen(
                    chatViewModel = chatViewModel,
                    modelViewModel = modelViewModel,
                    onNavigateBack = {
                        // Navigate to Home when back is pressed from inline profile
                        tabNavController.navigate(Tab.Home.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        selectedTabIndex = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun RustSenseiNavigationBar(
    tabs: List<Tab>,
    onTabSelected: (Int, Tab) -> Unit,
    currentDestination: NavDestination?
) {
    // M3 Navigation Bar spec: 80dp height, 24dp icons, 64x32dp active indicator
    val inactiveColor = Color(0xFF8B95A5)
    val activeIconColor = MaterialTheme.colorScheme.onPrimary
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == tab.route
                } == true

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onTabSelected(index, tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // M3 active indicator pill: 64x32dp with 16dp corner radius
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(primaryColor)
                                    } else {
                                        Modifier
                                    }
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
