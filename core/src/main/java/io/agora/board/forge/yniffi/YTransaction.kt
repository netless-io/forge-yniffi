package io.agora.board.forge.yniffi

import io.agora.board.forge.yniffi.Util.byteArray
import io.agora.board.forge.yniffi.Util.originStr
import io.agora.board.forge.yniffi.Util.toUByteList
import java.io.Closeable
import uniffi.yniffi.YrsTransaction

/**
 * A transactional context for making changes to shared data types.
 *
 * Transactions are used to group multiple changes together and ensure consistency.
 */
class YTransaction : Closeable {
    private val _transaction: YrsTransaction
    private var isFreed = false

    /**
     * Creates a transaction from a native YrsTransaction.
     */
    internal constructor(transaction: YrsTransaction) {
        _transaction = transaction
    }

    /**
     * Gets the origin of this transaction.
     *
     * @return A string identifying the origin of the transaction.
     */
    fun origin(): String? {
        return _transaction.origin().originStr()
    }

    /**
     * Returns the state vector for the document associated with this transaction.
     *
     * @return A byte array representing the state vector.
     */
    fun stateVector(): ByteArray {
        return _transaction.transactionStateVector().byteArray()
    }

    /**
     * Applies an update to the document.
     *
     * @param update The byte array containing the update to apply.
     */
    fun applyUpdate(update: ByteArray) {
        _transaction.transactionApplyUpdate(update.toUByteList())
    }

    /**
     * Encodes all changes made within this transaction.
     *
     * @return A byte array containing the encoded changes.
     */
    fun encodeUpdate(): ByteArray {
        return _transaction.transactionEncodeUpdate().byteArray()
    }

    /**
     * Encodes the state of the document as an update.
     *
     * @return A byte array containing the encoded state.
     */
    fun encodeStateAsUpdate(): ByteArray {
        return _transaction.transactionEncodeStateAsUpdate().byteArray()
    }

    /**
     * Encodes the state of the document as an update from a specific state vector.
     *
     * @param stateVector The state vector to encode from.
     * @return A byte array containing the encoded state.
     */
    fun encodeStateAsUpdateFromSV(stateVector: ByteArray): ByteArray {
        return _transaction.transactionEncodeStateAsUpdateFromSv(stateVector.toUByteList())
            .byteArray()
    }

    /**
     * Frees the native resources associated with this transaction.
     */
    fun free() {
        if (!isFreed) {
            _transaction.free()
            isFreed = true
        }
    }

    /**
     * Closes this transaction, releasing any resources associated with it.
     */
    override fun close() {
        free()
    }

    /**
     * Access the internal transaction object.
     */
    internal fun getTransaction(): YrsTransaction {
        return _transaction
    }
}

