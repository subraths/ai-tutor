package com.tutorai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tutorai.app.di.AppContainer
import com.tutorai.app.ui.history.HistoryScreen
import com.tutorai.app.ui.history.HistoryViewModel
import com.tutorai.app.ui.player.PlayerScreen
import com.tutorai.app.ui.player.PlayerViewModel
import com.tutorai.app.ui.topic.TopicScreen
import com.tutorai.app.ui.topic.TopicViewModel

object Routes {
    const val TOPIC = "topic"
    const val HISTORY = "history"
    const val PLAYER = "player/{lessonId}"          // remote (just-generated) lesson
    const val LIBRARY_PLAYER = "library/{lessonId}" // saved/offline lesson

    fun player(lessonId: String) = "player/$lessonId"
    fun libraryPlayer(lessonId: String) = "library/$lessonId"
}

@Composable
fun TutorNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.TOPIC) {
        composable(Routes.TOPIC) {
            val vm: TopicViewModel = viewModel(
                factory = TopicViewModel.factory(
                    container.generateLessonUseCase,
                    container.saveLessonUseCase,
                ),
            )
            TopicScreen(
                viewModel = vm,
                onPlayLesson = { id -> navController.navigate(Routes.player(id)) },
                onShowHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("lessonId").orEmpty()
            val repo = container.lessonRepository
            val vm: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory {
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

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(container.libraryRepository),
            )
            HistoryScreen(
                viewModel = vm,
                onOpen = { id -> navController.navigate(Routes.libraryPlayer(id)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
