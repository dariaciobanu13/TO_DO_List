package com.example.to_do_list.data

import kotlinx.coroutines.flow.Flow

enum class AuthError {
    INVALID_CREDENTIALS,
    INVALID_EMAIL,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    UNKNOWN
}

interface AuthRepository {
    /** The signed-in user right now, or null. */
    val currentUserId: String?

    /** Emits the signed-in user's id, and null after signing out. */
    val userId: Flow<String?>

    /** Returns null on success. */
    suspend fun signIn(email: String, password: String): AuthError?

    /** Returns null on success. */
    suspend fun signUp(email: String, password: String): AuthError?

    fun signOut()
}
