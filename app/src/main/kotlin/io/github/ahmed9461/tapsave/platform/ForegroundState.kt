package io.github.ahmed9461.tapsave.platform

/** Only the current candidate is retained; this is not a browsing history. */
class ForegroundState {
    var packageName: String? = null
        private set
    private var activityName: String? = null

    fun resumed(packageName: String, activityName: String? = null) {
        this.packageName = packageName
        this.activityName = activityName
    }
    fun paused(packageName: String, activityName: String? = null) {
        if (this.packageName == packageName && (activityName == null || this.activityName == activityName)) clear()
    }
    fun clear() {
        packageName = null
        activityName = null
    }
}
