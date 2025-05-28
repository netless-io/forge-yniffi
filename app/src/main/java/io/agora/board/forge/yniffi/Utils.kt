package io.agora.board.forge.yniffi

import android.content.Context

object Utils {
    fun getAssetsFile(c: Context, filename: String): ByteArray {
        c.resources.assets.open(filename).use {
            return it.readBytes()
        }
    }
}