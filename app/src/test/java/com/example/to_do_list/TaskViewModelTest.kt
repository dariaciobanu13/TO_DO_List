package com.example.to_do_list

import com.example.to_do_list.ui.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class TaskViewModelTest {

    private val repository = FakeTaskRepository()
    private val auth = FakeAuthRepository(initialUser = "ana")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel() = TaskViewModel(repository, auth).also { viewModel ->
        // state only runs while something is collecting it, like the UI does.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    @Test
    fun add_rejectsBlankTitle_andTrimsInput() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.add("   ", "desc"))
        assertTrue(repository.tasksOf("ana").isEmpty())

        assertTrue(viewModel.add("  Buy milk  ", "  2L  "))
        val task = repository.tasksOf("ana").single()
        assertEquals("Buy milk", task.title)
        assertEquals("2L", task.description)
    }

    @Test
    fun update_rejectsBlankTitle_andKeepsDone() = runTest {
        val viewModel = createViewModel()
        viewModel.add("Old", "")
        val id = repository.tasksOf("ana").single().id
        viewModel.setDone(id, true)

        assertFalse(viewModel.update(id, " ", ""))
        assertEquals("Old", repository.tasksOf("ana").single().title)

        assertTrue(viewModel.update(id, "New", "d"))
        val task = repository.tasksOf("ana").single()
        assertEquals("New", task.title)
        assertEquals("d", task.description)
        assertTrue(task.done)
    }

    @Test
    fun state_showsTasksOfCurrentUser_andDelete_removesThem() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.state.value.isLoading)

        viewModel.add("A", "")
        viewModel.add("B", "")
        assertEquals(listOf("A", "B"), viewModel.state.value.tasks.map { it.title })

        viewModel.delete(viewModel.state.value.tasks.first().id)
        assertEquals(listOf("B"), viewModel.state.value.tasks.map { it.title })
    }

    @Test
    fun switchingUser_swapsTheList_andWritesGoToTheNewUser() = runTest {
        val viewModel = createViewModel()
        viewModel.add("Ana's task", "")

        auth.switchTo("bob")
        assertTrue(viewModel.state.value.tasks.isEmpty())

        viewModel.add("Bob's task", "")
        assertEquals(listOf("Bob's task"), viewModel.state.value.tasks.map { it.title })
        assertEquals(listOf("Ana's task"), repository.tasksOf("ana").map { it.title })
    }

    @Test
    fun signedOut_listIsEmpty_andWritesAreIgnored() = runTest {
        val viewModel = createViewModel()
        viewModel.add("A", "")

        auth.signOut()

        assertTrue(viewModel.state.value.tasks.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.add("B", ""))
        assertEquals(1, repository.tasksOf("ana").size)
    }

    @Test
    fun loadFailure_stopsLoading_andReportsError() = runTest {
        repository.failObserve = true

        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Couldn't load your tasks.", viewModel.error.value)
    }

    @Test
    fun writeFailure_isReported_untilShown() = runTest {
        val viewModel = createViewModel()

        repository.writeErrorsSource.tryEmit(IllegalStateException())
        assertEquals("Couldn't save your changes.", viewModel.error.value)

        viewModel.errorShown()
        assertNull(viewModel.error.value)
    }
}
