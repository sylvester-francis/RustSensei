package com.sylvester.rustsensei.ui.components

/**
 * Simple undo/redo manager that tracks text state changes.
 * Maintains two stacks (undo and redo) with a configurable maximum history size.
 */
class UndoRedoManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var currentText: String? = null

    /**
     * Push a new text state. Clears the redo stack since a new edit
     * invalidates any previously undone states.
     */
    fun push(text: String) {
        if (text == currentText) return
        currentText?.let { undoStack.addLast(it) }
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        currentText = text
    }

    /**
     * Undo the last change. Returns the previous text state, or null if
     * there is nothing to undo.
     */
    fun undo(): String? {
        if (undoStack.isEmpty()) return null
        currentText?.let { redoStack.addLast(it) }
        val previous = undoStack.removeLast()
        currentText = previous
        return previous
    }

    /**
     * Redo a previously undone change. Returns the restored text state,
     * or null if there is nothing to redo.
     */
    fun redo(): String? {
        if (redoStack.isEmpty()) return null
        currentText?.let { undoStack.addLast(it) }
        val next = redoStack.removeLast()
        currentText = next
        return next
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()
}
