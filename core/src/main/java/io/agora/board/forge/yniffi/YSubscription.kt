package io.agora.board.forge.yniffi

import java.io.Closeable
import uniffi.yniffi.YSubscription as YSubscriptionNative

/**
 * A class that represents a subscription to changes in a shared data structure.
 *
 * Used to observe changes in YrsMap, YrsArray, and YrsText objects.
 */
class YSubscription : Closeable {
    private val _subscription: YSubscriptionNative

    /**
     * Creates a YrsSubscription from a native YSubscription.
     */
    internal constructor(subscription: YSubscriptionNative) {
        _subscription = subscription
    }

    /**
     * Cancels the subscription, preventing further callbacks.
     */
    fun cancel() {
        close()
    }

    /**
     * Closes this subscription, releasing any resources associated with it.
     */
    override fun close() {
        _subscription.destroy()
    }
} 