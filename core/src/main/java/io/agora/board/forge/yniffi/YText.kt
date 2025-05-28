package io.agora.board.forge.yniffi

import uniffi.yniffi.YrsCollectionPtr
import uniffi.yniffi.YrsDelta
import uniffi.yniffi.YrsText
import uniffi.yniffi.YrsTextObservationDelegate
import uniffi.yniffi.YrsTransaction
import java.io.Closeable

/**
 * A type that provides a text-oriented shared data type.
 *
 * Create a new YText instance using YDocument.getOrCreateText(name) from a YDocument.
 */
class YText : Closeable, YCollection {
    private val _text: YrsText
    private val document: YDocument

    /**
     * Creates a YText from a YrsText instance.
     */
    internal constructor(text: YrsText, document: YDocument) {
        _text = text
        this.document = document
    }

    /**
     * Appends a string to the shared text data type.
     *
     * @param text The string to append.
     * @param transaction An optional transaction to use when appending the string.
     */
    fun append(text: String, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _text.append(txn, text)
        }
    }

    /**
     * Inserts a string at an index position.
     *
     * @param text The string to insert.
     * @param index The position, within the UTF-8 buffer view, to insert the string.
     * @param transaction An optional transaction to use when inserting the string.
     */
    fun insert(text: String, index: UInt, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _text.insert(txn, index, text)
        }
    }

    /**
     * Inserts a string with attributes at an index position.
     *
     * @param text The string to insert.
     * @param attributes The attributes to associate with the inserted string.
     * @param index The position, within the UTF-8 buffer view, to insert the string.
     * @param transaction An optional transaction to use when inserting the string.
     */
    fun insertWithAttributes(
        text: String, attributes: Map<String, Any>, index: UInt, transaction: YrsTransaction? = null
    ) {
        withTransaction(transaction) { txn ->
            _text.insertWithAttributes(txn, index, text, Coder.encoded(attributes))
        }
    }

    /**
     * Embeds a serializable object within the text at the specified location.
     *
     * @param embed The object to embed.
     * @param index The position, within the UTF-8 buffer view, to embed the object.
     * @param transaction An optional transaction to use when embedding the object.
     */
    fun <T : Any> insertEmbed(embed: T, index: UInt, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _text.insertEmbed(txn, index, Coder.encoded(embed))
        }
    }

    /**
     * Embeds a serializable object with attributes within the text at the specified location.
     *
     * @param embed The object to embed.
     * @param attributes The attributes to associate with the embedded object.
     * @param index The position, within the UTF-8 buffer view, to embed the object.
     * @param transaction An optional transaction to use when embedding the object.
     */
    fun <T : Any> insertEmbedWithAttributes(
        embed: T, attributes: Map<String, Any>, index: UInt, transaction: YrsTransaction? = null
    ) {
        withTransaction(transaction) { txn ->
            _text.insertEmbedWithAttributes(
                txn, index, Coder.encoded(embed), Coder.encoded(attributes)
            )
        }
    }

    /**
     * Applies or updates attributes associated with a range of the string.
     *
     * @param index The index position, in the UTF-8 view of the string, to start formatting characters.
     * @param length The length of characters to update.
     * @param attributes The attributes to associate with the string.
     * @param transaction An optional transaction to use when formatting.
     */
    fun format(
        index: Int, length: Int, attributes: Map<String, Any>, transaction: YrsTransaction? = null
    ) {
        withTransaction(transaction) { txn ->
            _text.format(txn, index.toUInt(), length.toUInt(), Coder.encoded(attributes))
        }
    }

    /**
     * Removes a range of text starting at a position, removing the specified length.
     *
     * @param start The index position, in the UTF-8 view of the string, to start removing characters.
     * @param length The length of characters to remove.
     * @param transaction An optional transaction to use when removing text.
     */
    fun removeRange(start: UInt, length: UInt, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _text.removeRange(txn, start.toUInt(), length.toUInt())
        }
    }

    /**
     * Returns the string within the text.
     *
     * @param transaction An optional transaction to use when getting the string.
     * @return The current string content.
     */
    fun getString(transaction: YrsTransaction? = null): String {
        return withTransaction(transaction) { txn ->
            _text.getString(txn)
        }
    }

    /**
     * Returns the length of the string.
     *
     * @param transaction An optional transaction to use when getting the length.
     * @return The length of the text.
     */
    fun length(transaction: YrsTransaction? = null): UInt {
        return withTransaction(transaction) { txn ->
            _text.length(txn).toUInt()
        }
    }

    /**
     * Registers a callback that is called with changes to the text.
     *
     * @param callback The callback to process reported changes from the text.
     * @return A subscription that you can use to stop observing the text.
     */
    fun observe(callback: (List<YTextChange>) -> Unit): YSubscription {
        val delegate = object : YrsTextObservationDelegate {
            override fun call(changes: List<YrsDelta>) {
                val textChanges = changes.map { delta ->
                    when (delta) {
                        is YrsDelta.Deleted -> {
                            YTextChange.Deleted(index = delta.index)
                        }

                        is YrsDelta.Inserted -> {
                            YTextChange.Inserted(
                                value = delta.value,
                                attributes = Coder.decoded(delta.attrs) ?: emptyMap()
                            )
                        }

                        is YrsDelta.Retained -> {
                            YTextChange.Retained(
                                index = delta.index,
                                attributes = Coder.decoded(delta.attrs) ?: emptyMap()
                            )
                        }
                    }
                }
                callback(textChanges)
            }
        }
        return YSubscription(_text.observe(delegate))
    }

    /**
     * Gets the raw pointer for this text collection.
     */
    override fun pointer(): YrsCollectionPtr {
        return _text.rawPtr()
    }

    /**
     * Perform an operation with a temporary transaction.
     */
    private inline fun <T> withTransaction(
        transaction: YrsTransaction? = null, block: (YrsTransaction) -> T
    ): T {
        return document.withTransaction(transaction, block)
    }

    override fun close() {
        _text.close()
    }
}

/**
 * Represents a change to a YText.
 */
sealed class YTextChange {
    /**
     * Retains a number of characters, optionally with attributes.
     */
    data class Retained(
        val index: UInt, val attributes: Map<String, Any>? = null
    ) : YTextChange()

    /**
     * Inserts content, optionally with attributes.
     */
    data class Inserted(
        val value: String, val attributes: Map<String, Any>? = null
    ) : YTextChange()

    /**
     * Deletes a number of characters.
     */
    data class Deleted(
        val index: UInt
    ) : YTextChange()
} 