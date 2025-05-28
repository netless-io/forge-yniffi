package io.agora.board.forge.yniffi

import com.google.gson.reflect.TypeToken
import org.junit.Test

/**
 * Example usage of the YSwift-style Kotlin wrapper for yniffi.
 */
class Example {

    @Test
    fun demonstrateBasicUsage() {
        // Create a new document
        val document = YDocument()

        // Create shared data types
        val text = document.getOrCreateText("my-text")
        val map = document.getOrCreateMap("my-map")
        val array = document.getOrCreateArray("my-array")

        // Work with text
        document.transactSync { txn ->
            text.append("Hello, ", txn)
            text.append("World!", txn)

            // Insert with attributes
            text.insertWithAttributes(
                "formatted ", mapOf("bold" to true, "color" to "red"), 6u, txn
            )
        }

        // Work with map
        document.transactSync { txn ->
            map.set("name", "John Doe", txn)
            map.set("age", 30, txn)
            map.set("active", true, txn)
        }

        // Work with array
        val stringType = object : TypeToken<String>() {}.type
        document.transactSync { txn ->
            array.append("first", txn)
            array.append("second", txn)
            array.insert("inserted", 1, txn)
        }

        // Read data
        val textContent = text.getString()
        val name = map.get<String>("name", object : TypeToken<String>() {}.type)
        val arrayList = array.toList<String>(stringType)

        println("Text: $textContent")
        println("Name: $name")
        println("Array: $arrayList")

        // Observe changes
        val textSubscription = text.observe { changes ->
            println("Text changes: $changes")
        }

        val mapSubscription = map.observe<String>(stringType) { changes ->
            println("Map changes: $changes")
        }

        val arraySubscription = array.observe<String>(stringType) { changes ->
            println("Array changes: $changes")
        }

        // Make some changes to trigger observations
        document.transactSync { txn ->
            text.append(" More text!", txn)
            map.set("updated", "yes", txn)
            array.append("third", txn)
        }

        // Clean up
        textSubscription.close()
        mapSubscription.close()
        arraySubscription.close()

        text.close()
        map.close()
        array.close()
        document.close()
    }

    @Test
    fun demonstrateUndoRedo() {
        val document = YDocument()
        val text = document.getOrCreateText("undo-text")

        // Create undo manager
        val undoManager = document.undoManager(listOf(text))

        // Make some changes
        document.transactSync { txn ->
            text.append("First change", txn)
        }

        document.transactSync { txn ->
            text.append(" Second change", txn)
        }

        println("Before undo: ${text.getString()}")

        // Undo last change
        val undone = undoManager.undo()
        println("Undo successful: $undone")

        println("After undo: ${text.getString()}")

        // Redo
        val redone = undoManager.redo()
        println("Redo successful: $redone")

        println("After redo: ${text.getString()}")

        // Clean up
        undoManager.close()
        text.close()
        document.close()
    }

    @Test
    fun demonstrateSynchronization() {
        // Create two documents
        val doc1 = YDocument()
        val doc2 = YDocument()

        val text1 = doc1.getOrCreateText("sync-text")
        val text2 = doc2.getOrCreateText("sync-text")

        // Make changes in doc1
        doc1.transactSync { txn ->
            text1.append("Hello from doc1", txn)
        }

        // Get update from doc1
        val update = doc1.encodeStateAsUpdate()

        // Apply update to doc2
        doc2.applyUpdate(update)

        // Both documents should now have the same content
        println("Doc1 text: ${text1.getString()}")
        println("Doc2 text: ${text2.getString()}")

        // Protocol-based synchronization
        val protocol1 = YProtocol(doc1)
        val protocol2 = YProtocol(doc2)

        // Simulate connection start
        val step1Message = protocol1.handleConnectionStarted()
        val step2Message = protocol2.handleStep1(step1Message.buffer)

        protocol1.handleStep2(step2Message.buffer) {
            println("Synchronization complete")
        }

        // Clean up
        text1.close()
        text2.close()
        doc1.close()
        doc2.close()
    }
} 