package io.agora.board.forge.yniffi

import uniffi.yniffi.YrsMap
import uniffi.yniffi.YrsMapChange
import uniffi.yniffi.YrsMapIteratorDelegate
import uniffi.yniffi.YrsMapKvIteratorDelegate
import uniffi.yniffi.YrsMapObservationDelegate
import uniffi.yniffi.YrsTransaction
import java.io.Closeable
import java.lang.reflect.Type

/**
 * A type that provides a map shared data type.
 *
 * Store, order, and retrieve any serializable type within a YMap keyed with a String.
 */
class YMap : Closeable, YCollection {
    private val _map: YrsMap
    private val document: YDocument

    /**
     * Creates a YMap from a YrsMap instance.
     */
    internal constructor(map: YrsMap, document: YDocument) {
        _map = map
        this.document = document
    }

    /**
     * The number of key-value pairs in the map.
     */
    val count: Int
        get() = length().toInt()

    /**
     * Whether the map is empty.
     */
    val isEmpty: Boolean
        get() = count == 0

    /**
     * Gets the value for a key in the map.
     *
     * @param key The key to look up.
     * @param type The type to decode the value as.
     * @return The value for the key, or null if not found or can't be decoded.
     */
    fun <T : Any> get(key: String, type: Type): T? {
        return get(key, type, null)
    }

    /**
     * Sets the value for a key in the map.
     *
     * @param key The key to set.
     * @param value The value to set, or null to remove the key.
     */
    fun <T : Any> set(key: String, value: T?, transaction: YrsTransaction? = null) {
        if (value != null) {
            updateValue(value, key, transaction)
        } else {
            removeValue(key, transaction)
        }
    }

    /**
     * Returns the length of the map.
     *
     * @param transaction An optional transaction to use.
     * @return The number of key-value pairs in the map.
     */
    fun length(transaction: YrsTransaction? = null): Int {
        return withTransaction(transaction) { txn ->
            _map.length(txn).toInt()
        }
    }

    /**
     * Gets the value for the specified key.
     *
     * @param key The key to look up.
     * @param type The type to decode the value as.
     * @param transaction An optional transaction to use.
     * @return The value for the key, or null if not found or can't be decoded.
     */
    fun <T : Any> get(key: String, type: Type, transaction: YrsTransaction? = null): T? {
        return withTransaction(transaction) { txn ->
            try {
                val result = _map.get(txn, key)
                result.let { Coder.decoded(it, type) }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Updates or inserts the value for the specified key.
     *
     * @param value The value to update or insert.
     * @param key The key to update or insert.
     * @param transaction An optional transaction to use.
     */
    fun <T : Any> updateValue(value: T, key: String, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _map.insert(txn, key, Coder.encoded(value))
        }
    }

    /**
     * Removes the value for the specified key.
     *
     * @param key The key to remove.
     * @param type The type to decode the removed value as.
     * @param transaction An optional transaction to use.
     * @return The removed value, or null if the key didn't exist or couldn't be decoded.
     */
    fun <T : Any> removeValue(key: String, type: Type, transaction: YrsTransaction? = null): T? {
        return withTransaction(transaction) { txn ->
            try {
                val result = _map.remove(txn, key)
                result?.let { Coder.decoded(it, type) }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Removes the value for the specified key without returning it.
     *
     * @param key The key to remove.
     * @param transaction An optional transaction to use.
     */
    fun removeValue(key: String, transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            try {
                _map.remove(txn, key)
            } catch (_: Exception) {
                // Ignore exceptions
            }
        }
    }

    /**
     * Checks if the map contains the specified key.
     *
     * @param key The key to check.
     * @param transaction An optional transaction to use.
     * @return True if the key exists, false otherwise.
     */
    fun containsKey(key: String, transaction: YrsTransaction? = null): Boolean {
        return withTransaction(transaction) { txn ->
            _map.containsKey(txn, key)
        }
    }

    /**
     * Removes all key-value pairs from the map.
     *
     * @param transaction An optional transaction to use.
     */
    fun removeAll(transaction: YrsTransaction? = null) {
        withTransaction(transaction) { txn ->
            _map.clear(txn)
        }
    }

    /**
     * Iterates over the keys in the map.
     *
     * @param transaction An optional transaction to use.
     * @param callback Function to call for each key.
     */
    fun keys(transaction: YrsTransaction? = null, callback: (String) -> Unit) {
        val delegate = object : YrsMapIteratorDelegate {
            override fun call(value: String) {
                callback(value)
            }
        }
        withTransaction(transaction) { txn ->
            _map.keys(txn, delegate)
        }
    }

    /**
     * Iterates over the values in the map.
     *
     * @param type The type to decode values as.
     * @param transaction An optional transaction to use.
     * @param callback Function to call for each value.
     */
    fun <T : Any> values(type: Type, transaction: YrsTransaction? = null, callback: (T) -> Unit) {
        val delegate = object : YrsMapIteratorDelegate {
            override fun call(value: String) {
                Coder.decoded<T>(value, type)?.let { callback(it) }
            }
        }
        withTransaction(transaction) { txn ->
            _map.values(txn, delegate)
        }
    }

    /**
     * Iterates over each key-value pair in the map.
     *
     * @param type The type to decode values as.
     * @param transaction An optional transaction to use.
     * @param callback Function to call for each key-value pair.
     */
    fun <T : Any> each(
        type: Type, transaction: YrsTransaction? = null, callback: (String, T) -> Unit
    ) {
        val delegate = object : YrsMapKvIteratorDelegate {
            override fun call(key: String, value: String) {
                Coder.decoded<T>(value, type)?.let { callback(key, it) }
            }
        }
        withTransaction(transaction) { txn ->
            _map.each(txn, delegate)
        }
    }

    /**
     * Observes changes to the map.
     *
     * @param type The type to decode values as.
     * @param callback Function to call with changes.
     * @return A subscription that can be used to stop observing changes.
     */
    fun <T : Any> observe(type: Type, callback: (List<YMapChange<T>>) -> Unit): YSubscription {
        val delegate = object : YrsMapObservationDelegate {
            override fun call(value: List<YrsMapChange>) {
                val changes = convertChanges<T>(value, type)
                callback(changes)
            }
        }
        return YSubscription(_map.observe(delegate))
    }

    /**
     * Converts the map to a standard Map.
     *
     * @param type The type to decode values as.
     * @param transaction An optional transaction to use.
     * @return A Map containing all key-value pairs.
     */
    fun <T : Any> toMap(type: Type, transaction: YrsTransaction? = null): Map<String, T> {
        val result = mutableMapOf<String, T>()
        each<T>(type, transaction) { key, value ->
            result[key] = value
        }
        return result
    }

    /**
     * Closes this map, releasing any resources associated with it.
     */
    override fun close() {
        // YrsMap is managed by the Rust side, so no explicit closing needed
    }

    /**
     * Access the internal YrsMap object.
     */
    internal fun getMap(): YrsMap {
        return _map
    }

    /**
     * Perform an operation with a temporary transaction.
     */
    private inline fun <R> withTransaction(
        transaction: YrsTransaction? = null, block: (YrsTransaction) -> R
    ): R {
        return document.withTransaction(transaction, block)
    }

    private fun <T : Any> convertChanges(
        changes: List<YrsMapChange>, type: Type
    ): List<YMapChange<T>> {
        return emptyList()
    }

    override fun pointer(): ULong {
        return _map.rawPtr()
    }
}

/**
 * Represents a change to a YMap.
 */
sealed class YMapChange<T> {
    /**
     * Represents the addition of a key-value pair.
     */
    data class Added<T>(val key: String, val value: T) : YMapChange<T>()

    /**
     * Represents the removal of a key-value pair.
     */
    data class Removed<T>(val key: String, val oldValue: T?) : YMapChange<T>()

    /**
     * Represents the update of a value for an existing key.
     */
    data class Updated<T>(val key: String, val oldValue: T?, val newValue: T) : YMapChange<T>()
} 