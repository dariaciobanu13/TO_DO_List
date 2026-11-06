package com.example.to_do_list.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val userId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthError? =
        runAuthCall { auth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun signUp(email: String, password: String): AuthError? =
        runAuthCall { auth.createUserWithEmailAndPassword(email, password).await() }

    override fun signOut() = auth.signOut()

    private suspend fun runAuthCall(call: suspend () -> Unit): AuthError? = try {
        call()
        null
    } catch (e: FirebaseException) {
        e.toAuthError()
    }

    private fun FirebaseException.toAuthError(): AuthError = when (this) {
        // Must stay first: it is a subclass of FirebaseAuthInvalidCredentialsException.
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == ERROR_INVALID_EMAIL) AuthError.INVALID_EMAIL
            else AuthError.INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }
}
