package com.tutorai.app.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tutorai.app.di.AppContainer
import com.tutorai.app.ui.components.TutorIcons
import com.tutorai.app.ui.history.HistoryScreen
import com.tutorai.app.ui.history.HistoryViewModel
import com.tutorai.app.ui.player.PlayerScreen
import com.tutorai.app.ui.player.PlayerViewModel
import com.tutorai.app.ui.settings.SettingsScreen
import com.tutorai.app.ui.settings.SettingsViewModel
import com.tutorai.app.ui.topic.TopicScreen
import com.tutorai.app.ui.topic.TopicViewModel

object Routes {
    const val HOME = "home"                         // topic entry
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{lessonId}"          // remote (just-generated) lesson
    const val LIBRARY_PLAYER = "library/{lessonId}" // saved/offline lesson

    fun player(lessonId: String) = "player/$lessonId"
    fun libraryPlayer(lessonId: String) = "library/$lessonId"
}

/** Top-level destinations reachable from the bottom navigation bar. */
enum class TutorTab(val route: String, val label: String, val icon: ImageVector) {
    Home(Routes.HOME, "Home", TutorIcons.Home),
    History(Routes.HISTORY, "History", TutorIcons.History),
    Settings(Routes.SETTINGS, "Settings", TutorIcons.Settings),
}

@Composable
fun TutorNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val onSelectTab: (TutorTab) -> Unit = { tab -> navController.switchTab(tab.route) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: TopicViewModel = viewModel(
                factory = TopicViewModel.factory(
                    container.generateLessonUseCase,
                    container.saveLessonUseCase,
                    container.libraryRepository,
                ),
            )
            TopicScreen(
                viewModel = vm,
                onPlayLesson = { id -> navController.navigate(Routes.player(id)) },
                onOpenSaved = { id -> navController.navigate(Routes.libraryPlayer(id)) },
                bottomBar = { TutorBottomBar(selected = TutorTab.Home, onSelect = onSelectTab) },
            )
        }

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(container.libraryRepository),
            )
            HistoryScreen(
                viewModel = vm,
                onOpen = { id -> navController.navigate(Routes.libraryPlayer(id)) },
                onCreateLesson = { onSelectTab(TutorTab.Home) },
                bottomBar = { TutorBottomBar(selected = TutorTab.History, onSelect = onSelectTab) },
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container.themePreferences),
            )
            SettingsScreen(
                viewModel = vm,
                bottomBar = { TutorBottomBar(selected = TutorTab.Settings, onSelect = onSelectTab) },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("lessonId").orEmpty()
            val repo = container.lessonRepository
            val vm: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    saver = { lesson -> container.saveLessonUseCase(lesson) },
                    savedChecker = { container.libraryRepository.isSaved(id) },
                ) {
                    val lesson = repo.getLesson(id)
                    lesson to repo.getSvg(lesson.svgUrl)
                },
            )
            PlayerScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LIBRARY_PLAYER,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("lessonId").orEmpty()
            val library = container.libraryRepository
            val vm: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory { library.load(id) },
            )
            PlayerScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}

/** Switch top-level tabs, preserving each tab's state and avoiding back-stack buildup. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun TutorBottomBar(selected: TutorTab, onSelect: (TutorTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TutorTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { if (selected != tab) onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}
