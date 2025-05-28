package io.agora.board.forge.yniffi

import uniffi.yniffi.YrsOrigin

object Util {
    fun String?.toYrsOrigin(): YrsOrigin? {
        return this?.toByteArray()?.map { it.toUByte() }
    }

    fun YrsOrigin?.originStr(): String? {
        return this?.map { it.toByte() }?.toByteArray()?.decodeToString()
    }

    fun ByteArray.toUByteList(): List<UByte> {
        return this.map { it.toUByte() }
    }

    fun List<UByte>.byteArray(): ByteArray {
        return this.map { it.toByte() }.toByteArray()
    }
}