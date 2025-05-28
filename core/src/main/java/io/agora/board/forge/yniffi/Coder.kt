package io.agora.board.forge.yniffi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Utility class for encoding and decoding objects to and from JSON.
 */
object Coder {
    val gson = Gson()

    /**
     * Encodes an object to a JSON string.
     *
     * @param value The object to encode.
     * @return A JSON string representation of the object.
     */
    fun <T> encoded(value: T): String {
        return gson.toJson(value)
    }

    fun <T> decoded(json: String, type: Type): T? {
        return try {
            gson.fromJson<T>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a JSON string to an object of the specified type.
     *
     * @param json The JSON string to decode.
     * @return The decoded object, or null if decoding fails.
     */
    inline fun <reified T> decoded(json: String): T? {
        return decoded<T>(json, object : TypeToken<T>() {}.type)
    }

    /**
     * Encodes a list of objects to a list of JSON strings.
     *
     * @param values The list of objects to encode.
     * @return A list of JSON string representations of the objects.
     */
    fun <T> encodedArray(values: List<T>): List<String> {
        return values.map { encoded(it) }
    }

    /**
     * Decodes a list of JSON strings to a list of objects of the specified type.
     *
     * @param jsonArray The list of JSON strings to decode.
     * @return A list of decoded objects, with null values filtered out.
     */
    fun <T> decodedArray(jsonArray: List<String>, type: Type): List<T> {
        return jsonArray.mapNotNull { decoded(it, type) }
    }
} 