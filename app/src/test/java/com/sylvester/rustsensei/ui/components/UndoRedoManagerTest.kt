package com.sylvester.rustsensei.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoManagerTest {

    @Test
    fun `undo returns previous state`() {
        val manager = UndoRedoManager()
        manager.push("first")
        manager.push("second")
        val result = manager.undo()
        assertEquals("first", result)
    }

    @Test
    fun `redo returns undone state`() {
        val manager = UndoRedoManager()
        manager.push("first")
        manager.push("second")
        manager.undo()
        val result = manager.redo()
        assertEquals("second", result)
    }

    @Test
    fun `undo on empty returns null`() {
        val manager = UndoRedoManager()
        assertNull(manager.undo())
        assertFalse(manager.canUndo())
    }

    @Test
    fun `push clears redo stack`() {
        val manager = UndoRedoManager()
        manager.push("first")
        manager.push("second")
        manager.undo() // back to "first"
        assertTrue(manager.canRedo())
        manager.push("third") // new edit clears redo
        assertFalse(manager.canRedo())
        assertNull(manager.redo())
    }

    @Test
    fun `max history respected`() {
        val manager = UndoRedoManager(maxHistory = 3)
        // Push 5 items to exceed max history of 3
        manager.push("a") // undo=[], current=a
        manager.push("b") // undo=[a], current=b
        manager.push("c") // undo=[a,b], current=c
        manager.push("d") // undo=[a,b,c], current=d
        manager.push("e") // undo=[a,b,c,d] -> trim -> undo=[b,c,d], current=e

        // Can undo 3 times (b, c, d are in stack)
        assertEquals("d", manager.undo())
        assertEquals("c", manager.undo())
        assertEquals("b", manager.undo())
        // "a" was evicted, so no more undo
        assertNull(manager.undo())
    }

    @Test
    fun `duplicate consecutive pushes ignored`() {
        val manager = UndoRedoManager()
        manager.push("hello")
        manager.push("hello")
        manager.push("hello")
        // Only one state was pushed since duplicates are ignored
        assertFalse(manager.canUndo())
    }
}
