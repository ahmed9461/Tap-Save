package io.github.ahmed9461.tapsave.platform

/** Only the current candidate is retained; this is not a browsing history. */
class ForegroundState {
    var packageName: String? = null
        private set

    fun resumed(packageName: String) { this.packageName = packageName }
    fun paused(packageName: String) {
        if (this.packageName == packageName) clear()
    }
    fun clear() { packageName = null }
}
