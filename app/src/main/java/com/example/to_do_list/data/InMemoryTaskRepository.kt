package com.example.to_do_list.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryTaskRepository : TaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    override val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private var nextId = 1L

    override fun get(id: Long): Task? = _tasks.value.find { it.id == id }

    override fun add(title: String, description: String): Task {
        val task = Task(id = nextId++, title = title, description = description)
        _tasks.update { it + task }
        return task
    }

    override fun update(task: Task) {
        _tasks.update { list -> list.map { if (it.id == task.id) task else it } }
    }

    override fun toggleDone(id: Long) {
        _tasks.update { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }
    }

    override fun delete(id: Long) {
        _tasks.update { list -> list.filterNot { it.id == id } }
    }
}
