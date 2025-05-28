package io.agora.board.forge.yniffi

import uniffi.yniffi.YrsArray
import uniffi.yniffi.YrsArrayEachDelegate
import uniffi.yniffi.YrsArrayObservationDelegate
import uniffi.yniffi.YrsChange
import uniffi.yniffi.YrsCollectionPtr
import uniffi.yniffi.YrsTransaction
import java.io.Closeable
import java.lang.reflect.Type

/**
 * A type that provides a list shared data type.
 *
 * Store, order, and retrieve values within an array.
 */
class YArray : Closeable, YCollection {
    private val array: YrsArray
    private val document: YDocument

    /**
     * Creates a YArray from a native YrsArray instance.
     */
    internal constructor(array: YrsArray, document: YDocument) {
        this.array = array
        this.document = document
    }

    /**
     * The number of items in the array.
     */
    val count: Int
        get() = length()

    /**
     * Whether the array is empty.
     */
    val isEmpty: Boolean
        get() = count == 0

    /**
     * Returns the number of items in the array.
     *
     * @param transaction An optional transaction to use.
     * @return The number of items in the array.
     */
    fun length(transaction: YrsTransaction? = null): Int {
        return withTransaction(transaction) { txn ->
            array.length(txn).toInt()
        }
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index The index to look up.
     * @param type The type to decode the value as.
     * @param transaction An optional transaction to use.
     * @return The value at the index, or null if not found or can't be decoded.
     */
    fun <T : Any> get(index: Int, type: Type, transaction: YrsTransaction? = null): T? {
        return withTransaction(transaction) { txn ->
            try {
                val result = array.get(txn, index.toUInt())
                Coder.decoded(result, type)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun <T : Any> set(index: Int, value: T, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.remove(txn, index.toUInt())
            array.insert(txn, index.toUInt(), Coder.encoded(value))
        }
    }

    /**
     * Inserts a value at the specified index.
     *
     * @param value The value to insert.
     * @param index The index to insert at.
     * @param transaction An optional transaction to use.
     */
    fun <T : Any> insert(value: T, index: Int, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.insert(txn, index.toUInt(), Coder.encoded(value))
        }
    }

    /**
     * Inserts a range of values at the specified index.
     *
     * @param values The values to insert.
     * @param index The index to insert at.
     * @param transaction An optional transaction to use.
     */
    fun <T : Any> insertRange(values: List<T>, index: Int, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            val encodedValues = values.map { Coder.encoded(it) }
            array.insertRange(txn, index.toUInt(), encodedValues)
        }
    }

    /**
     * Removes the value at the specified index.
     *
     * @param index The index to remove at.
     * @param transaction An optional transaction to use.
     */
    fun remove(index: Int, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.remove(txn, index.toUInt())
        }
    }

    /**
     * Removes a range of values starting at the specified index.
     *
     * @param index The starting index to remove from.
     * @param length The number of values to remove.
     * @param transaction An optional transaction to use.
     */
    fun removeRange(index: UInt, length: UInt, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.removeRange(txn, index, length)
        }
    }

    /**
     * Appends a value to the end of the array.
     *
     * @param value The value to append.
     * @param transaction An optional transaction to use.
     */
    fun <T : Any> append(value: T, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.pushBack(txn, Coder.encoded(value))
        }
    }

    /**
     * Prepends a value to the beginning of the array.
     *
     * @param value The value to prepend.
     * @param transaction An optional transaction to use.
     */
    fun <T : Any> prepend(value: T, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            array.pushFront(txn, Coder.encoded(value))
        }
    }

    /**
     * Converts the array to a standard List.
     *
     * @param type The type to decode values as.
     * @param transaction An optional transaction to use.
     * @return A List containing all values.
     */
    fun <T : Any> toList(type: Type, transaction: YrsTransaction? = null): List<T> {
        return withTransaction(transaction) { txn ->
            val rawList = array.toA(txn)
            rawList.mapNotNull { Coder.decoded<T>(it, type) }
        }
    }

    /**
     * Iterates over each value in the array.
     *
     * @param type The type to decode values as.
     * @param transaction An optional transaction to use.
     * @param callback Function to call for each value.
     */
    fun <T : Any> forEach(type: Type, transaction: YrsTransaction? = null, callback: (T) -> Unit) {
        val delegate = object : YrsArrayEachDelegate {
            override fun call(value: String) {
                Coder.decoded<T>(value, type)?.let { callback(it) }
            }
        }
        withTransaction(transaction) { txn ->
            array.each(txn, delegate)
        }
    }

    /**
     * Observes changes to the array.
     *
     * @param type The type to decode values as.
     * @param callback Function to call when changes occur.
     * @return A subscription that can be used to stop observing changes.
     */
    fun <T : Any> observe(type: Type, callback: (List<YArrayChange<T>>) -> Unit): YSubscription {
        val delegate = object : YrsArrayObservationDelegate {
            override fun call(value: List<YrsChange>) {
                val changes = value.map { change ->
                    when (change) {
                        is YrsChange.Added -> {
                            val values = Coder.decodedArray<T>(change.elements, type)
                            YArrayChange.Added(values)
                        }

                        is YrsChange.Removed -> {
                            YArrayChange.Removed(change.range.toInt())
                        }

                        is YrsChange.Retained -> {
                            YArrayChange.Retained(change.range.toInt())
                        }
                    }
                }
                callback(changes)
            }
        }
        return YSubscription(array.observe(delegate))
    }

    /**
     * Gets the raw pointer to this collection.
     *
     * @return A YrsCollectionPtr representing this collection.
     */
    override fun pointer(): YrsCollectionPtr {
        return array.rawPtr()
    }

    /**
     * Closes this array, releasing any resources associated with it.
     */
    override fun close() {
        // YrsArray is managed by the Rust side, so no explicit closing needed
    }

    /**
     * Access the internal YrsArray object.
     */
    internal fun getArray(): YrsArray {
        return array
    }

    /**
     * Perform an operation with a temporary transaction.
     */
    private inline fun <T> withTransaction(
        transaction: YrsTransaction? = null, block: (YrsTransaction) -> T
    ): T {
        return document.withTransaction(transaction, block)
    }
}

inline fun <reified T : Any> YArray.toList(transaction: YrsTransaction? = null): List<T> {
    return toList(T::class.java, transaction)
}


/**
 * Represents a change to a YArray.
 */
sealed class YArrayChange<T> {
    /// Objects added to the list.
    data class Added<T>(val elements: List<T>) : YArrayChange<T>()

    /// An index position that is removed.
    data class Removed<T>(val range: Int) : YArrayChange<T>()

    /// An index position that is updated.
    data class Retained<T>(val range: Int) : YArrayChange<T>()
} 