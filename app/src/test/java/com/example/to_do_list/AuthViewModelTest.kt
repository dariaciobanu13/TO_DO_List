package com.example.to_do_list

import com.example.to_do_list.data.AuthError
import com.example.to_do_list.ui.auth.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val auth = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyFields_showError_withoutCallingFirebase() = runTest {
        val viewModel = AuthViewModel(auth)

        viewModel.signIn("  ", "secret")
        assertEquals("Enter your email and password.", viewModel.uiState.value.error)

        viewModel.signUp("a@b.ro", "")
        assertEquals("Enter your email and password.", viewModel.uiState.value.error)

        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun signIn_trimsEmail_keepsPassword_andUpdatesUser() = runTest {
        val viewModel = AuthViewModel(auth)

        viewModel.signIn("  a@b.ro ", " secret ")

        assertEquals("a@b.ro" to " secret ", auth.calls.single())
        assertEquals("a@b.ro", viewModel.userId.value)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadingState_lastsUntilFirebaseAnswers_andBlocksSecondSubmit() = runTest {
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate
        val viewModel = AuthViewModel(auth)

        viewModel.signIn("a@b.ro", "secret")
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.signIn("a@b.ro", "secret")
        assertEquals(1, auth.calls.size)

        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun errors_becomeMessages() = runTest {
        val expected = mapOf(
            AuthError.INVALID_CREDENTIALS to "Wrong email or password.",
            AuthError.INVALID_EMAIL to "That email address isn't valid.",
            AuthError.EMAIL_IN_USE to "An account with this email already exists.",
            AuthError.WEAK_PASSWORD to "Password must have at least 6 characters.",
            AuthError.NETWORK to "No internet connection.",
            AuthError.UNKNOWN to "Something went wrong. Try again."
        )
        val viewModel = AuthViewModel(auth)

        expected.forEach { (error, message) ->
            auth.nextError = error
            viewModel.signUp("a@b.ro", "secret")
            assertEquals(message, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }
        assertNull(viewModel.userId.value)
    }

    @Test
    fun clearError_removesTheMessage() = runTest {
        auth.nextError = AuthError.NETWORK
        val viewModel = AuthViewModel(auth)
        viewModel.signIn("a@b.ro", "secret")

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun signOut_clearsUser() = runTest {
        auth.switchTo("ana")
        val viewModel = AuthViewModel(auth)

        viewModel.signOut()

        assertNull(viewModel.userId.value)
    }
}
