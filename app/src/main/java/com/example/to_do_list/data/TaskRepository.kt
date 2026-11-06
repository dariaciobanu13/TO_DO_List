package com.example.to_do_list.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Failed writes, reported here because the write functions return before the server answers. */
    val writeErrors: Flow<Throwable>

    fun observeTasks(userId: String): Flow<List<Task>>
    fun add(userId: String, title: String, description: String)
    fun update(userId: String, id: String, title: String, description: String)
    fun setDone(userId: String, id: String, done: Boolean)
    fun delete(userId: String, id: String)
}
