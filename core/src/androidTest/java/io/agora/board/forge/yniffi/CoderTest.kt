package io.agora.board.forge.yniffi

import org.junit.Assert.*

import org.junit.Test

class CoderTest {

    @Test
    fun encoded() {
        val text = "Hello, World!"
        val encodedText = Coder.encoded(text)
        assertEquals("\"Hello, World!\"", encodedText)

        val number = 42
        val encodedNumber = Coder.encoded(number)
        assertEquals("42", encodedNumber)

        val map = mapOf("key" to "value", "number" to 123)
        val encodedMap = Coder.encoded(map)
        assertEquals("{\"key\":\"value\",\"number\":123}", encodedMap)
    }

    @Test
    fun decoded() {
        val jsonText = "\"Hello, World!\""
        val decodedText: String? = Coder.decoded(jsonText)
        assertEquals("Hello, World!", decodedText)

        val jsonNumber = "42"
        val decodedNumber: Int? = Coder.decoded(jsonNumber)
        assertEquals(42, decodedNumber)

        val jsonMap = "{\"key\":\"value\",\"number\":123}"
        val decodedMap: Map<String, Any>? = Coder.decoded(jsonMap)
        assertNotNull(decodedMap)
        assertEquals("value", decodedMap?.get("key"))
        assertEquals(123.0, decodedMap?.get("number"))
    }

    @Test
    fun encodedArray() {
        val list = listOf("apple", "banana", "cherry")
        val encodedList = Coder.encodedArray(list)
        assertEquals(listOf("\"apple\"", "\"banana\"", "\"cherry\""), encodedList)

        val emptyList = emptyList<String>()
        val encodedEmptyList = Coder.encodedArray(emptyList)
        assertEquals(emptyList<String>(), encodedEmptyList)
    }

    @Test
    fun decodedArray() {
        val jsonArray = listOf("\"apple\"", "\"banana\"", "\"cherry\"")
        val decodedList: List<String> = Coder.decodedArray(jsonArray, String::class.java)
        assertEquals(listOf("apple", "banana", "cherry"), decodedList)

        val emptyJsonArray = emptyList<String>()
        val decodedEmptyList: List<String> = Coder.decodedArray(emptyJsonArray, String::class.java)
        assertEquals(emptyList<String>(), decodedEmptyList)
    }
}