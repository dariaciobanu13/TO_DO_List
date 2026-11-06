package com.example.to_do_list.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.to_do_list.ui.TaskViewModel
import com.example.to_do_list.ui.edit.TaskEditScreen
import com.example.to_do_list.ui.list.TaskListScreen

private const val LIST_ROUTE = "list"
private const val EDIT_ROUTE = "edit"
private const val TASK_ID_ARG = "taskId"
private const val NEW_TASK_ID = -1L

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // Created here so both screens share the same ViewModel (and the same tasks).
    val viewModel: TaskViewModel = viewModel()

    NavHost(navController = navController, startDestination = LIST_ROUTE) {
        composable(LIST_ROUTE) {
            val tasks by viewModel.tasks.collectAsState()
            TaskListScreen(
                tasks = tasks,
                onAddClick = { navController.navigate(EDIT_ROUTE) },
                onTaskClick = { id -> navController.navigate("$EDIT_ROUTE?$TASK_ID_ARG=$id") },
                onToggleDone = viewModel::toggleDone,
                onDelete = viewModel::delete
            )
        }

        composable(
            route = "$EDIT_ROUTE?$TASK_ID_ARG={$TASK_ID_ARG}",
            arguments = listOf(navArgument(TASK_ID_ARG) {
                type = NavType.LongType
                defaultValue = NEW_TASK_ID
            })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong(TASK_ID_ARG) ?: NEW_TASK_ID
            val task = viewModel.get(taskId)

            TaskEditScreen(
                task = task,
                onSave = { title, description ->
                    val saved = if (task == null) viewModel.add(title, description)
                    else viewModel.update(task.id, title, description)
                    if (saved) navController.popBackStack()
                },
                onDelete = {
                    if (task != null) viewModel.delete(task.id)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
