package io.agora.board.forge.yniffi

import uniffi.yniffi.YrsUndoManager
import java.io.Closeable

/**
 * An undo manager that tracks changes to shared data types and provides undo/redo functionality.
 */
class YUndoManager : Closeable {
    private val _manager: YrsUndoManager

    /**
     * Creates a YUndoManager from a YrsUndoManager instance.
     */
    internal constructor(manager: YrsUndoManager) {
        _manager = manager
    }

    /**
     * Undoes the last operation.
     *
     * @param transaction The transaction to use for the undo operation.
     * @return True if an operation was undone, false if there was nothing to undo.
     */
    fun undo(): Boolean {
        return try {
            _manager.undo()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Redoes the last undone operation.
     *
     * @param transaction The transaction to use for the redo operation.
     * @return True if an operation was redone, false if there was nothing to redo.
     */
    fun redo(): Boolean {
        return try {
            _manager.redo()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears the undo/redo history.
     */
    fun clear() {
        _manager.clear()
    }

    fun wrap() {
        _manager.wrapChanges()
    }

    override fun close() {
        _manager.close()
    }
} 