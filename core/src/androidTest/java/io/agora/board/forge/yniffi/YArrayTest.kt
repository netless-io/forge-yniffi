package io.agora.board.forge.yniffi

import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

data class TestType(val name: String, val age: Int)

class YArrayTest {
    private lateinit var document: YDocument
    private lateinit var array: YArray

    @Before
    fun setUp() {
        document = YDocument()
        array = document.getOrCreateArray("test")
    }

    @After
    fun tearDown() {
        // 若有需要可释放资源
    }

    @Test
    fun test_subscripts() {
        val aidar = TestType("Aidar", 24)
        val kevin = TestType("Kevin", 100)
        val joe = TestType("Joe", 55)
        val bart = TestType("Bart", 200)

        array.insertRange(listOf(aidar, kevin, joe), 0)
        array.set(0, bart)
        array.remove(1)

        assertEquals(2, array.length())
        assertEquals(bart, array.get(0, TestType::class.java))
        assertEquals(joe, array.get(1, TestType::class.java))
    }

    @Test
    fun test_HOFs() {
        val aidar = TestType("Aidar", 24)
        val joe = TestType("Joe", 55)
        array.insertRange(listOf(aidar, joe), 0)

        val arrayList = array.toList<TestType>()
        assertEquals(listOf(aidar), arrayList.filter { it.name == "Aidar" })
        assertEquals(listOf(TestType("Mr. Aidar", 100), TestType("Mr. Joe", 100)),
            arrayList.map { TestType("Mr. " + it.name, 100) })
        assertEquals(79, arrayList.fold(0) { sum, current -> sum + current.age })
    }

    @Test
    fun test_insert() {
        val initialInstance = TestType("Aidar", 24)
        array.insert(initialInstance, 0)
        assertEquals(initialInstance, array.get(0, TestType::class.java))
    }

    @Test
    fun test_getIndexOutOfBounds() {
        val initialInstance = TestType("Aidar", 24)
        array.insert(initialInstance, 0)
        // getOrNull 需手动实现或用 get + try/catch
        val value = try {
            array.get(1, TestType::class.java)
        } catch (e: Exception) {
            null
        }
        assertNull(value)
    }

    @Test
    fun test_insertArray() {
        val arrayToInsert = listOf(TestType("Aidar", 24), TestType("Joe", 55))
        array.insertRange(arrayToInsert, 0)
        assertEquals(arrayToInsert, array.toList<TestType>(TestType::class.java))
    }

    @Test
    fun test_length() {
        array.insert(TestType("Aidar", 24), 0)
        assertEquals(1, array.length())
    }

    @Test
    fun test_pushBack_and_pushFront() {
        val initial = TestType("Middleton", 77)
        val front = TestType("Aidar", 24)
        val back = TestType("Joe", 55)

        array.insert(initial, 0)
        array.append(back)
        array.prepend(front)

        assertEquals(listOf(front, initial, back), array.toList<TestType>(TestType::class.java))
    }

    @Test
    fun test_remove() {
        val initial = TestType("Middleton", 77)
        val front = TestType("Aidar", 24)
        val back = TestType("Joe", 55)

        array.insert(initial, 0)
        array.append(back)
        array.prepend(front)

        assertEquals(listOf(front, initial, back), array.toList<TestType>(TestType::class.java))

        array.remove(1)

        assertEquals(listOf(front, back), array.toList<TestType>(TestType::class.java))
    }

    @Test
    fun test_removeRange() {
        val initial = TestType("Middleton", 77)
        val front = TestType("Aidar", 24)
        val back = TestType("Joe", 55)

        array.insert(initial, 0)
        array.append(back)
        array.prepend(front)

        assertEquals(listOf(front, initial, back), array.toList<TestType>(TestType::class.java))

        array.removeRange(0u, 3u)

        assertEquals(0, array.length())
    }

    @Test
    fun test_forEach() {
        val arrayToInsert = listOf(TestType("Aidar", 24), TestType("Joe", 55))
        val collectedArray = mutableListOf<TestType>()

        array.insertRange(arrayToInsert, 0)
        array.forEach<TestType>(TestType::class.java) { collectedArray.add(it) }

        assertEquals(arrayToInsert, collectedArray)
    }

    // 内存泄漏和观察相关测试因 JVM/Android 机制与 Swift 不同，暂未实现
}
