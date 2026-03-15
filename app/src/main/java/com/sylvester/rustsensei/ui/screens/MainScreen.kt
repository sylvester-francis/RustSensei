package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel

sealed class Tab(val route: String, val title: String, val icon: ImageVector) {
    data object Learn : Tab("learn", "Learn", Icons.Default.MenuBook)
    data object Practice : Tab("practice", "Practice", Icons.Default.Code)
    data object Reference : Tab("reference", "Ref", Icons.Default.Book)
    data object Chat : Tab("chat", "Chat", Icons.Default.Chat)
    data object Progress : Tab("progress", "Progress", Icons.Default.TrendingUp)
}

private val tabs = listOf(Tab.Learn, Tab.Practice, Tab.Reference, Tab.Chat, Tab.Progress)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    bookViewModel: BookViewModel,
    exerciseViewModel: ExerciseViewModel,
    progressViewModel: ProgressViewModel,
    referenceViewModel: ReferenceViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit = {}
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "RustSensei",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // 1dp neon bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                )
            }
        },
        bottomBar = {
            Column {
                // 1dp neon top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                tabNavController.navigate(tab.route) {
                                    popUpTo(tabNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Tab.Learn.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Tab.Learn.route) {
                BookScreen(
                    viewModel = bookViewModel,
                    // Fix #10: pass actual section ID from the ViewModel
                    onAskSensei = { sectionContent, _ ->
                        chatViewModel.setChatContext(
                            ChatContext.BookSection(
                                sectionId = bookViewModel.getCurrentSectionId(),
                                content = sectionContent
                            )
                        )
                        tabNavController.navigate(Tab.Chat.route)
                    }
                )
            }
            composable(Tab.Practice.route) {
                ExercisesScreen(
                    viewModel = exerciseViewModel,
                    // Fix #10: pass actual exercise ID from the ViewModel
                    onAskSensei = { exerciseDesc, userCode ->
                        chatViewModel.setChatContext(
                            ChatContext.Exercise(
                                exerciseId = exerciseViewModel.getExerciseId(),
                                description = exerciseDesc,
                                userCode = userCode
                            )
                        )
                        tabNavController.navigate(Tab.Chat.route)
                    }
                )
            }
            composable(Tab.Reference.route) {
                ReferenceScreen(viewModel = referenceViewModel)
            }
            composable(Tab.Chat.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSetup = onNavigateToSetup
                )
            }
            composable(Tab.Progress.route) {
                DashboardScreen(viewModel = progressViewModel)
            }
        }
    }
}
