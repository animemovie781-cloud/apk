package com.tom.rv2ide.build.lightweight

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A token that can be checked to see if a build operation should be cancelled.
 */
class BuildCancellationToken {
    private val isCancelled = AtomicBoolean(false)
    
    /**
     * @return true if the build has been cancelled.
     */
    fun isCancelled(): Boolean = isCancelled.get()
    
    /**
     * Triggers a cancellation.
     */
    fun cancel() {
        isCancelled.set(true)
    }
    
    /**
     * Resets the cancellation state.
     */
    fun reset() {
        isCancelled.set(false)
    }
    
    /**
     * Throws an exception if the build has been cancelled.
     */
    fun checkCancelled() {
        if (isCancelled.get()) {
            throw BuildCancelledException()
        }
    }
}

/**
 * Exception thrown when a build is cancelled.
 */
class BuildCancelledException : Exception("Build cancelled by user")
