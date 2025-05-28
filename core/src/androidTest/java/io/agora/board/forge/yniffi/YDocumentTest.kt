/**
 * FIXME: This file should move out of Android Tests ASAP. It only exists here because I haven't yet
 * figured out how to build and link the platform-native binaries via JNI just yet and this works.
 * See https://github.com/willir/cargo-ndk-android-gradle/issues/12.
 *
 * This solution is STUPIDLY INEFFICIENT and will probably contribute to global climate change since
 * an Android emulator uses like two whole CPU cores when idling.
 */
package io.agora.board.forge.yniffi

import org.junit.Test
import uniffi.yniffi.YrsDoc
import uniffi.yniffi.YrsTransaction

class YDocumentTest {
    class UserData() {
        var a = 1;
        var b = "123";
    }

    @Test
    fun testYDocment() {
        val doc = YDocument() // 实例化 Doc
        val map = doc.getOrCreateMap("map")
        map.set("key", UserData())

        val value = map.get<UserData>(key = "key", type = UserData::class.java)
        assert(value?.a == 1) { "Expected 'value', got $value" }
        assert(value?.b == "123") { "Expected 'value', got $value" }
        doc.close()
    }

    @Test
    fun applyUpdate() {
        val doc = YDocument()
        val map = doc.getOrCreateMap("map")
        map.set("key", "123")
        map.set("key2", 456)

        val update = doc.encodeStateAsUpdate()
        val doc2 = YDocument()
        doc2.applyUpdate(update)
        val map2 = doc2.getOrCreateMap("map")

        val value = map2.get<String>(key = "key", type = String::class.java)
        val value2 = map2.get<Int>(key = "key2", type = Int::class.java)

        assert(value == "123") { "Expected 'value', got $value" }
        assert(value2 == 456) { "Expected 'value', got $value" }

        doc.close()
    }
}