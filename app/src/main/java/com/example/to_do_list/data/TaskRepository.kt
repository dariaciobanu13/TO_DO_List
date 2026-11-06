package com.example.to_do_list.data

import kotlinx.coroutines.flow.StateFlow

// Where tasks are stored. Today it's in memory; later a Room implementation can replace it.
interface TaskRepository {
    val tasks: StateFlow<List<Task>>

    fun get(id: Long): Task?
    fun add(title: String, description: String): Task
    fun update(task: Task)
    fun toggleDone(id: Long)
    fun delete(id: Long)
}
