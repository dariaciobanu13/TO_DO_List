package com.example.to_do_list

import com.example.to_do_list.data.AuthError
import com.example.to_do_list.data.AuthRepository
import com.example.to_do_list.data.Task
import com.example.to_do_list.data.TaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTaskRepository : TaskRepository {
    private val tasksByUser = MutableStateFlow<Map<String, List<Task>>>(emptyMap())
    private var nextId = 1

    val writeErrorsSource = MutableSharedFlow<Throwable>(extraBufferCapacity = 8)
    override val writeErrors: Flow<Throwable> = writeErrorsSource

    var failObserve = false

    fun tasksOf(userId: String): List<Task> = tasksByUser.value[userId].orEmpty()

    override fun observeTasks(userId: String): Flow<List<Task>> {
        if (failObserve) return flow { throw IllegalStateException() }
        return tasksByUser.map { it[userId].orEmpty() }
    }

    override fun add(userId: String, title: String, description: String) {
        edit(userId) { it + Task((nextId++).toString(), title, description) }
    }

    override fun update(userId: String, id: String, title: String, description: String) {
        edit(userId) { list ->
            list.map { if (it.id == id) it.copy(title = title, description = description) else it }
        }
    }

    override fun setDone(userId: String, id: String, done: Boolean) {
        edit(userId) { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    override fun delete(userId: String, id: String) {
        edit(userId) { list -> list.filterNot { it.id == id } }
    }

    private fun edit(userId: String, change: (List<Task>) -> List<Task>) {
        tasksByUser.update { it + (userId to change(it[userId].orEmpty())) }
    }
}

class FakeAuthRepository(initialUser: String? = null) : AuthRepository {
    private val user = MutableStateFlow(initialUser)

    /** The result of the next sign in or sign up; null means success. */
    var nextError: AuthError? = null

    /** When set, sign in and sign up wait for it, so tests can see the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    val calls = mutableListOf<Pair<String, String>>()

    override val currentUserId: String?
        get() = user.value

    override val userId: Flow<String?> = user

    override suspend fun signIn(email: String, password: String): AuthError? =
        authenticate(email, password)

    override suspend fun signUp(email: String, password: String): AuthError? =
        authenticate(email, password)

    override fun signOut() {
        user.value = null
    }

    fun switchTo(userId: String?) {
        user.value = userId
    }

    private suspend fun authenticate(email: String, password: String): AuthError? {
        calls += email to password
        gate?.await()
        return nextError ?: run {
            user.value = email
            null
        }
    }
}
