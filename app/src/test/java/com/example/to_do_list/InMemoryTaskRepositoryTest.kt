package com.example.to_do_list

import com.example.to_do_list.data.InMemoryTaskRepository
import com.example.to_do_list.ui.TaskViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryTaskRepositoryTest {

    private val repo = InMemoryTaskRepository()

    @Test
    fun add_appendsTaskWithUniqueId() {
        val a = repo.add("A", "")
        val b = repo.add("B", "desc")

        assertEquals(listOf(a, b), repo.tasks.value)
        assertTrue(a.id != b.id)
        assertEquals("desc", repo.get(b.id)?.description)
    }

    @Test
    fun update_replacesTask() {
        val task = repo.add("Old", "")
        repo.update(task.copy(title = "New", description = "d"))

        assertEquals("New", repo.get(task.id)?.title)
        assertEquals("d", repo.get(task.id)?.description)
        assertEquals(1, repo.tasks.value.size)
    }

    @Test
    fun toggleDone_flipsDone() {
        val task = repo.add("A", "")

        repo.toggleDone(task.id)
        assertTrue(repo.get(task.id)!!.done)

        repo.toggleDone(task.id)
        assertFalse(repo.get(task.id)!!.done)
    }

    @Test
    fun delete_removesOnlyThatTask() {
        val a = repo.add("A", "")
        val b = repo.add("B", "")

        repo.delete(a.id)

        assertEquals(listOf(b), repo.tasks.value)
    }

    @Test
    fun get_missingId_returnsNull() {
        assertNull(repo.get(42))
    }

    @Test
    fun viewModel_rejectsBlankTitle_andTrimsInput() {
        val viewModel = TaskViewModel(repo)

        assertFalse(viewModel.add("   ", "desc"))
        assertTrue(repo.tasks.value.isEmpty())

        assertTrue(viewModel.add("  Buy milk  ", "  2L  "))
        val task = repo.tasks.value.single()
        assertEquals("Buy milk", task.title)
        assertEquals("2L", task.description)

        assertFalse(viewModel.update(task.id, "", ""))
        assertEquals("Buy milk", repo.get(task.id)?.title)
    }
}
