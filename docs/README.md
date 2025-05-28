# YSwift-style Kotlin Wrapper for yniffi

This package provides a high-level, Swift-inspired Kotlin wrapper around the uniffi-generated yniffi bindings. It offers a more ergonomic and type-safe API for working with Y-CRDT shared data types.

## Features

- **Type-safe shared data types**: `YText`, `YMap`, `YArray` with generic type support
- **Transaction management**: Automatic transaction handling with `transactSync`
- **Observation system**: Subscribe to changes in shared data types
- **Undo/Redo support**: Built-in undo manager for tracking changes
- **Synchronization protocol**: Y-CRDT protocol implementation for document synchronization
- **Resource management**: Proper cleanup with `Closeable` interface

## Core Classes

### YDocument
The main document class that coordinates shared data types and transactions.

```kotlin
val document = YDocument()

// Create shared data types
val text = document.getOrCreateText("my-text")
val map = document.getOrCreateMap("my-map")
val array = document.getOrCreateArray<String>("my-array")

// Perform operations in transactions
document.transactSync { txn ->
    text.append("Hello, World!", txn)
    map.set("key", "value", txn)
    array.append("item", txn)
}
```

### YText
A shared text data type with rich text support.

```kotlin
val text = document.getOrCreateText("text")

document.transactSync { txn ->
    text.append("Hello, ", txn)
    text.insert("World!", 7u, txn)
    
    // Insert with formatting
    text.insertWithAttributes(
        "formatted ",
        mapOf("bold" to true, "color" to "red"),
        0u,
        txn
    )
    
    // Format existing text
    text.format(0u, 9u, mapOf("italic" to true), txn)
}

val content = text.getString()
val length = text.length()
```

### YMap
A shared map data type for key-value storage.

```kotlin
val map = document.getOrCreateMap("map")

// Set values
map.set("name", "John Doe")
map.set("age", 30)
map.set("active", true)

// Get values with type safety
val name = map.get<String>("name", object : TypeToken<String>() {}.type)
val age = map.get<Int>("age", object : TypeToken<Int>() {}.type)

// Check existence
val hasName = map.containsKey("name")

// Iterate over entries
map.each(object : TypeToken<String>() {}.type) { key, value ->
    println("$key: $value")
}
```

### YArray
A shared array data type for ordered collections.

```kotlin
val array = document.getOrCreateArray<String>("array")
val stringType = object : TypeToken<String>() {}.type

document.transactSync { txn ->
    array.append("first", txn)
    array.append("second", txn)
    array.insert("inserted", 1u, txn)
    array.remove(0u, txn)
}

val items = array.toList(stringType)
val count = array.count
val isEmpty = array.isEmpty
```

## Observing Changes

All shared data types support observation of changes:

```kotlin
// Observe text changes
val textSubscription = text.observe { changes ->
    changes.forEach { change ->
        when (change) {
            is YTextChange.Insert -> println("Inserted: ${change.content}")
            is YTextChange.Delete -> println("Deleted: ${change.length} chars")
            is YTextChange.Retain -> println("Retained: ${change.length} chars")
        }
    }
}

// Observe map changes
val mapSubscription = map.observe(stringType) { changes ->
    changes.forEach { change ->
        when (change) {
            is YMapChange.Added -> println("Added: ${change.key} = ${change.value}")
            is YMapChange.Updated -> println("Updated: ${change.key}")
            is YMapChange.Removed -> println("Removed: ${change.key}")
        }
    }
}

// Don't forget to clean up
textSubscription.close()
mapSubscription.close()
```

## Undo/Redo

Track changes and provide undo/redo functionality:

```kotlin
val text = document.getOrCreateText("text")
val undoManager = document.undoManager(listOf(text))

document.transactSync { txn ->
    text.append("First change", txn)
}

document.transactSync { txn ->
    text.append(" Second change", txn)
}

// Undo last change
document.transactSync { txn ->
    val success = undoManager.undo(txn)
    println("Undo successful: $success")
}

// Redo
document.transactSync { txn ->
    val success = undoManager.redo(txn)
    println("Redo successful: $success")
}
```

## Document Synchronization

Synchronize documents using the Y-CRDT protocol:

```kotlin
val doc1 = YDocument()
val doc2 = YDocument()

// Make changes in doc1
doc1.transactSync { txn ->
    val text1 = doc1.getOrCreateText("sync-text")
    text1.append("Hello from doc1", txn)
}

// Get update and apply to doc2
val update = doc1.encodeStateAsUpdate()
doc2.applyUpdate(update)

// Or use the protocol for more sophisticated synchronization
val protocol1 = YProtocol(doc1)
val protocol2 = YProtocol(doc2)

val step1Message = protocol1.handleConnectionStarted()
val step2Message = protocol2.handleStep1(step1Message.buffer)
protocol1.handleStep2(step2Message.buffer) {
    println("Synchronization complete")
}
```

## Resource Management

All classes implement `Closeable` for proper resource cleanup:

```kotlin
val document = YDocument()
val text = document.getOrCreateText("text")
val subscription = text.observe { changes -> /* ... */ }

try {
    // Use the objects
} finally {
    subscription.close()
    text.close()
    document.close()
}

// Or use try-with-resources
document.use { doc ->
    val text = doc.getOrCreateText("text")
    text.use { t ->
        // Use text
    }
}
```

## Type Safety

The wrapper provides compile-time type safety through generics and TypeToken:

```kotlin
// Type-safe map operations
val stringMap = document.getOrCreateMap("strings")
val intMap = document.getOrCreateMap("integers")

stringMap.set("key", "string value")  // ✓ Correct
intMap.set("key", 42)                 // ✓ Correct

val stringValue: String? = stringMap.get("key", object : TypeToken<String>() {}.type)
val intValue: Int? = intMap.get("key", object : TypeToken<Int>() {}.type)
```

## Error Handling

The wrapper handles errors gracefully and provides safe defaults:

```kotlin
// Safe operations that return null on error
val value = map.get<String>("nonexistent", stringType)  // Returns null
val item = array.get(999u, stringType)                  // Returns null

// Operations that fail silently
map.removeValue("nonexistent")  // No exception thrown
array.remove(999u)              // No exception thrown
```

This wrapper provides a clean, type-safe, and ergonomic API that closely mirrors the Swift YSwift implementation while leveraging Kotlin's language features for better developer experience. 