package com.example.to_do_list.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.to_do_list.ui.TaskViewModel
import com.example.to_do_list.ui.auth.AuthScreen
import com.example.to_do_list.ui.auth.AuthViewModel
import com.example.to_do_list.ui.edit.TaskEditScreen
import com.example.to_do_list.ui.list.TaskListScreen

private const val LIST_ROUTE = "list"
private const val EDIT_ROUTE = "edit"
private const val TASK_ID_ARG = "taskId"

@Composable
fun AppNavHost() {
    val authViewModel: AuthViewModel = viewModel()
    // Created here so both task screens share the same ViewModel (and the same tasks).
    val taskViewModel: TaskViewModel = viewModel()
    val userId by authViewModel.userId.collectAsState()

    if (userId == null) {
        val uiState by authViewModel.uiState.collectAsState()
        AuthScreen(
            uiState = uiState,
            onSignIn = authViewModel::signIn,
            onSignUp = authViewModel::signUp,
            onClearError = authViewModel::clearError
        )
    } else {
        TasksNavHost(taskViewModel, onSignOut = authViewModel::signOut)
    }
}

@Composable
private fun TasksNavHost(viewModel: TaskViewModel, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()

    NavHost(navController = navController, startDestination = LIST_ROUTE) {
        composable(LIST_ROUTE) {
            TaskListScreen(
                tasks = state.tasks,
                isLoading = state.isLoading,
                error = error,
                onErrorShown = viewModel::errorShown,
                onAddClick = { navController.navigate(EDIT_ROUTE) },
                onTaskClick = { id -> navController.navigate("$EDIT_ROUTE?$TASK_ID_ARG=$id") },
                onToggleDone = { task -> viewModel.setDone(task.id, !task.done) },
                onDelete = viewModel::delete,
                onSignOut = onSignOut
            )
        }

        composable(
            route = "$EDIT_ROUTE?$TASK_ID_ARG={$TASK_ID_ARG}",
            arguments = listOf(navArgument(TASK_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString(TASK_ID_ARG)
            val task = state.tasks.find { it.id == taskId }

            when {
                // Without this, a list that isn't loaded yet would open an empty "New task" form.
                taskId != null && task == null && state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                // Deleted, here or on another device.
                taskId != null && task == null -> LaunchedEffect(Unit) {
                    navController.popIfCurrent(backStackEntry)
                }

                else -> TaskEditScreen(
                    task = task,
                    onSave = { title, description ->
                        val saved = if (task == null) viewModel.add(title, description)
                        else viewModel.update(task.id, title, description)
                        if (saved) navController.popIfCurrent(backStackEntry)
                    },
                    onDelete = {
                        navController.popIfCurrent(backStackEntry)
                        if (task != null) viewModel.delete(task.id)
                    },
                    onBack = { navController.popIfCurrent(backStackEntry) }
                )
            }
        }
    }
}

// Popping twice (a double tap, or a delete plus the check above) would remove the list screen too.
private fun NavController.popIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry == entry) popBackStack()
}
