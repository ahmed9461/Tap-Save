package io.github.ahmed9461.tapsave.platform

/** A fresh user-requested target, independent of saved jobs and prior shares. Main-thread API. */
fun interface CurrentReelAcquirer {
    fun acquire(completed: (Result<String>) -> Unit)
}
