package com.krishna.infodeck.core

/**
 * The engine behind Undo / Discard (design 1.2.D1 C2). Pure, bounded, and it can
 * never pop past the bottom — a Discard always has the open-time snapshot to fall back on.
 */
class UndoStack<T>(private val original: T, private val limit: Int = MAX) {

    private val steps = ArrayDeque<T>()

    val canUndo: Boolean get() = steps.isNotEmpty()
    val depth: Int get() = steps.size
    val dirtyCount: Int get() = steps.size

    /** Call BEFORE applying a change, with the value as it is now. */
    fun push(before: T) {
        steps.addLast(before)
        while (steps.size > limit) steps.removeFirst()
    }

    /** One step back, or null when there is nothing to undo. */
    fun undo(): T? = if (steps.isEmpty()) null else steps.removeLast()

    /** Everything back to how it was when the screen opened. */
    fun discard(): T {
        steps.clear()
        return original
    }

    fun clear() = steps.clear()

    companion object { const val MAX = 50 }
}
