package com.example.to_do_list.ui

import androidx.lifecycle.ViewModel
import com.example.to_do_list.data.InMemoryTaskRepository
import com.example.to_do_list.data.Task
import com.example.to_do_list.data.TaskRepository
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel(
    private val repository: TaskRepository = InMemoryTaskRepository()
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.tasks

    fun get(id: Long): Task? = repository.get(id)

    /** Returns false (and saves nothing) when the title is blank. */
    fun add(title: String, description: String): Boolean {
        if (title.isBlank()) return false
        repository.add(title.trim(), description.trim())
        return true
    }

    /** Returns false (and saves nothing) when the title is blank. */
    fun update(id: Long, title: String, description: String): Boolean {
        val existing = repository.get(id) ?: return false
        if (title.isBlank()) return false
        repository.update(existing.copy(title = title.trim(), description = description.trim()))
        return true
    }

    fun toggleDone(id: Long) = repository.toggleDone(id)

    fun delete(id: Long) = repository.delete(id)
}
