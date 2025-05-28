package io.agora.board.forge.yniffi

/**
 * A type that identifies the origin of a transaction.
 */
data class Origin(val origin: String?) {
    companion object {
        /**
         * No origin specified.
         */
        val none: Origin? = null
    }
} 