package io.agora.board.forge.yniffi

import io.agora.board.forge.yniffi.Util.byteArray
import io.agora.board.forge.yniffi.Util.toUByteList

/**
 * A contiguous buffer of bytes.
 */
typealias Buffer = ByteArray

/**
 * Represents a Y-CRDT synchronization message.
 */
data class YSyncMessage(
    val kind: Kind,
    val buffer: Buffer
) {
    enum class Kind(val value: Int) {
        STEP_1(0),
        STEP_2(1),
        UPDATE(2)
    }
}

/**
 * A class that implements the Y-CRDT synchronization protocol.
 *
 * For more information on the synchronization protocol, see [Y Protocol Specification](https://github.com/yjs/y-protocols/blob/master/PROTOCOL.md).
 */
class YProtocol(private val document: YDocument) {

    /**
     * Handles the start of a connection by sending the initial state vector.
     */
    fun handleConnectionStarted(): YSyncMessage {
        return sendStep1()
    }

    /**
     * Handles step 1 of the synchronization protocol.
     *
     * @param stateVector The state vector from the remote peer.
     * @return A step 2 message containing the update.
     */
    fun handleStep1(stateVector: Buffer): YSyncMessage {
        val update = document.transactSync { txn ->
            txn.transactionEncodeStateAsUpdateFromSv(stateVector.toUByteList()).byteArray()
        }
        return sendStep2(update)
    }

    /**
     * Handles step 2 of the synchronization protocol.
     *
     * @param update The update from the remote peer.
     * @param completionHandler Called when the update has been applied.
     */
    fun handleStep2(update: Buffer, completionHandler: () -> Unit) {
        document.transactSync { txn ->
            txn.transactionApplyUpdate(update.toUByteList())
        }
        completionHandler()
    }

    /**
     * Handles an update message.
     *
     * @param update The update from the remote peer.
     * @param completionHandler Called when the update has been applied.
     */
    fun handleUpdate(update: Buffer, completionHandler: () -> Unit) {
        handleStep2(update, completionHandler)
    }

    /**
     * Sends step 1 of the synchronization protocol.
     */
    private fun sendStep1(): YSyncMessage {
        val stateVector = document.transactSync { txn ->
            txn.transactionStateVector().byteArray()
        }
        return YSyncMessage(YSyncMessage.Kind.STEP_1, stateVector)
    }

    /**
     * Sends step 2 of the synchronization protocol.
     */
    private fun sendStep2(update: Buffer): YSyncMessage {
        return YSyncMessage(YSyncMessage.Kind.STEP_2, update)
    }

    /**
     * Sends an update message.
     */
    private fun sendUpdate(update: Buffer): YSyncMessage {
        return YSyncMessage(YSyncMessage.Kind.UPDATE, update)
    }
} 