package com.example.to_do_list.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.to_do_list.data.AuthRepository
import com.example.to_do_list.data.FirebaseAuthRepository
import com.example.to_do_list.data.FirestoreTaskRepository
import com.example.to_do_list.data.Task
import com.example.to_do_list.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true
)

class TaskViewModel(
    private val repository: TaskRepository = FirestoreTaskRepository(),
    authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val userId: StateFlow<String?> = authRepository.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUserId)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Follows the signed-in user, so signing out or switching accounts swaps the list.
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TasksState> = userId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(TasksState(isLoading = false))
            } else {
                repository.observeTasks(id)
                    .map { TasksState(tasks = it, isLoading = false) }
                    .onStart { emit(TasksState(isLoading = true)) }
                    .catch {
                        _error.value = "Couldn't load your tasks."
                        emit(TasksState(isLoading = false))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    init {
        viewModelScope.launch {
            repository.writeErrors.collect { _error.value = "Couldn't save your changes." }
        }
    }

    fun errorShown() {
        _error.value = null
    }

    /** Returns false (and saves nothing) when the title is blank. */
    fun add(title: String, description: String): Boolean {
        val id = userId.value ?: return false
        if (title.isBlank()) return false
        repository.add(id, title.trim(), description.trim())
        return true
    }

    /** Returns false (and saves nothing) when the title is blank. */
    fun update(taskId: String, title: String, description: String): Boolean {
        val id = userId.value ?: return false
        if (title.isBlank()) return false
        repository.update(id, taskId, title.trim(), description.trim())
        return true
    }

    fun setDone(taskId: String, done: Boolean) {
        userId.value?.let { repository.setDone(it, taskId, done) }
    }

    fun delete(taskId: String) {
        userId.value?.let { repository.delete(it, taskId) }
    }
}
