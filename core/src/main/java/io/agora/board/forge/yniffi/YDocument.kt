package io.agora.board.forge.yniffi

import io.agora.board.forge.yniffi.Util.byteArray
import io.agora.board.forge.yniffi.Util.toUByteList
import io.agora.board.forge.yniffi.Util.toYrsOrigin
import uniffi.yniffi.YrsArray
import uniffi.yniffi.YrsDoc
import uniffi.yniffi.YrsMap
import uniffi.yniffi.YrsText
import uniffi.yniffi.YrsTransaction
import java.io.Closeable

/**
 * A shared document that contains shared data structures.
 *
 * Use YrsDocument to create and access shared data types such as arrays, maps, and text.
 */
class YDocument : Closeable {
    private val _doc: YrsDoc
    private var isFreed = false

    /**
     * Creates a new, empty document.
     */
    constructor() {
        _doc = YrsDoc()
    }

    /**
     * Creates a document from an existing YrsDoc instance.
     */
    internal constructor(doc: YrsDoc) {
        _doc = doc
    }

    /**
     * Gets or creates a shared array with the specified name.
     *
     * @param name The name of the array to access or create.
     * @return A YrsArray instance.
     */
    fun getArray(name: String): YrsArray {
        return _doc.getArray(name)
    }

    /**
     * Gets or creates a generic shared array with the specified name.
     *
     * @param name The name of the array to access or create.
     * @return A YArray instance.
     */
    fun getOrCreateArray(name: String): YArray {
        val array = getArray(name)
        return YArray(array, this)
    }

    /**
     * Gets or creates a shared map with the specified name.
     *
     * @param name The name of the map to access or create.
     * @return A YrsMap instance.
     */
    fun getMap(name: String): YrsMap {
        return _doc.getMap(name)
    }

    /**
     * Gets or creates a generic shared map with the specified name.
     *
     * @param name The name of the map to access or create.
     * @return A YMap instance.
     */
    fun getOrCreateMap(name: String): YMap {
        val map = getMap(name)
        return YMap(map, this)
    }

    /**
     * Gets or creates a shared text with the specified name.
     *
     * @param name The name of the text to access or create.
     * @return A YrsText instance.
     */
    fun getText(name: String): YrsText {
        return _doc.getText(name)
    }

    /**
     * Gets or creates a generic shared text with the specified name.
     *
     * @param name The name of the text to access or create.
     * @return A YText instance.
     */
    fun getOrCreateText(name: String): YText {
        val text = getText(name)
        return YText(text, this)
    }

    /**
     * Creates a transaction for modifying the document.
     *
     * @param origin Optional string identifying the origin of the transaction.
     * @return A transaction object.
     */
    fun transact(origin: String? = null): YrsTransaction {
        return _doc.transact(origin.toYrsOrigin())
    }

    /**
     * Creates a synchronous transaction and provides that transaction to a trailing closure, within which you make changes to shared data types.
     *
     * @param origin Optional origin identifying the source of the transaction.
     * @param changes The closure in which you make changes to the document.
     * @return The value that you return from the closure.
     */
    fun <T> transactSync(origin: Origin? = null, changes: (YrsTransaction) -> T): T {
        val transaction = _doc.transact(origin?.origin.toYrsOrigin())
        return try {
            changes(transaction)
        } finally {
            transaction.free()
        }
    }

    /**
     * Compares the state vector from another YSwift document to return a data buffer you can use to synchronize with another YSwift document.
     *
     * Use transactionStateVector() on a transaction to get a state buffer to compare with this method.
     *
     * @param transaction A transaction within which to compare the state of the document.
     * @param state A data buffer from another YSwift document.
     * @return A buffer that contains the diff you can use to synchronize another YSwift document.
     */
    fun diff(transaction: YrsTransaction, state: ByteArray = byteArrayOf()): ByteArray {
        return _doc.encodeDiffV1(transaction, state.toUByteList()).byteArray()
    }

    /**
     * Compares the state vector from another document to return a data buffer you can use to synchronize with another document.
     *
     * @param state A data buffer from another document.
     * @return A buffer that contains the diff you can use to synchronize another document.
     */
    fun diff(state: ByteArray = byteArrayOf()): ByteArray {
        return transact().use { transaction ->
            diff(transaction, state)
        }
    }

    /**
     * Creates an undo manager to track changes in specific parts of the document.
     *
     * @param trackedRefs List of references to track for undo/redo operations.
     * @return An undo manager instance.
     */
    // fun undoManager(trackedRefs: List<String>): YrsUndoManager {
    //     return _doc.undoManager(trackedRefs)
    // }

    /**
     * Creates an undo manager for a document with the collections that it tracks.
     *
     * @param trackedRefs The collections to track to undo and redo changes.
     * @return A reference to the undo manager to control those actions.
     */
    fun undoManager(trackedRefs: List<YCollection>): YUndoManager {
        val mapped = trackedRefs.map { it.pointer() }
        return YUndoManager(_doc.undoManager(mapped))
    }

    /**
     * Encodes changes made to the document since the specified state vector.
     *
     * @param stateVector The state vector to encode changes from.
     * @return Binary data representing the changes.
     */
    fun encodeDiffV1(stateVector: ByteArray): ByteArray {
        return transact().use { transaction ->
            _doc.encodeDiffV1(transaction, stateVector.toUByteList()).byteArray()
        }
    }

    fun encodeStateAsUpdate(stateVector: ByteArray? = null): ByteArray {
        return transact().use { transaction ->
            if (stateVector == null) {
                return transaction.transactionEncodeStateAsUpdate().byteArray()
            } else {
                transaction.transactionEncodeStateAsUpdateFromSv(stateVector.toUByteList())
                    .byteArray()
            }
        }
    }

    fun applyUpdate(update: ByteArray) {
        transact().use { transaction ->
            transaction.transactionApplyUpdate(update.toUByteList())
        }
    }

    /**
     * Access the internal YrsDoc object.
     */
    internal fun getDoc(): YrsDoc {
        return _doc
    }

    /**
     * Perform an operation with a temporary transaction.
     *
     * @param transaction An optional transaction to use.
     * @param block The operation to perform with the transaction.
     * @return The result of the operation.
     */
    inline fun <T> withTransaction(
        transaction: YrsTransaction? = null, block: (YrsTransaction) -> T
    ): T {
        val shouldFreeTransaction = transaction == null
        val txn = transaction ?: transact()

        return try {
            block(txn)
        } finally {
            if (shouldFreeTransaction) {
                txn.free()
            }
        }
    }

    /**
     * Closes this document, releasing any resources associated with it.
     */
    override fun close() {
        free()
    }

    /**
     * Frees the native resources associated with this document.
     */
    fun free() {
        if (!isFreed) {
            isFreed = true
            _doc.close()
        }
    }
}

