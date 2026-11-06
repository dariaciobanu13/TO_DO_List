package com.example.to_do_list.data

import com.google.android.gms.tasks.Task as GmsTask
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow

private const val FIELD_TITLE = "title"
private const val FIELD_DESCRIPTION = "description"
private const val FIELD_DONE = "done"
private const val FIELD_CREATED_AT = "createdAt"

/**
 * Stores tasks in users/{userId}/tasks/{taskId}.
 *
 * Writes are not awaited: offline, a Firestore write only completes once the connection is back.
 * Firestore applies them locally right away and sends them when it can.
 */
class FirestoreTaskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TaskRepository {

    private val _writeErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 8)
    override val writeErrors: Flow<Throwable> = _writeErrors.asSharedFlow()

    override fun observeTasks(userId: String): Flow<List<Task>> = callbackFlow {
        val registration = tasks(userId)
            .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toTask() })
                }
            }
        awaitClose { registration.remove() }
    }

    override fun add(userId: String, title: String, description: String) {
        tasks(userId).document()
            .set(
                mapOf(
                    FIELD_TITLE to title,
                    FIELD_DESCRIPTION to description,
                    FIELD_DONE to false,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp()
                )
            )
            .reportFailure()
    }

    override fun update(userId: String, id: String, title: String, description: String) {
        tasks(userId).document(id)
            .update(mapOf(FIELD_TITLE to title, FIELD_DESCRIPTION to description))
            .reportFailure()
    }

    override fun setDone(userId: String, id: String, done: Boolean) {
        tasks(userId).document(id).update(FIELD_DONE, done).reportFailure()
    }

    override fun delete(userId: String, id: String) {
        tasks(userId).document(id).delete().reportFailure()
    }

    private fun tasks(userId: String): CollectionReference =
        firestore.collection("users").document(userId).collection("tasks")

    private fun GmsTask<Void>.reportFailure() {
        addOnFailureListener { _writeErrors.tryEmit(it) }
    }

    private fun DocumentSnapshot.toTask() = Task(
        id = id,
        title = getString(FIELD_TITLE).orEmpty(),
        description = getString(FIELD_DESCRIPTION).orEmpty(),
        done = getBoolean(FIELD_DONE) ?: false
    )
}
