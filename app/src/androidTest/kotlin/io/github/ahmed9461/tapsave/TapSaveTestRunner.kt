package io.github.ahmed9461.tapsave

import android.app.UiAutomation
import androidx.test.runner.AndroidJUnitRunner

/** Keep one automation connection policy across Compose and accessibility tests. */
class TapSaveTestRunner : AndroidJUnitRunner() {
    override fun getUiAutomation(): UiAutomation? = getUiAutomation(0)

    override fun getUiAutomation(flags: Int): UiAutomation? =
        super.getUiAutomation(flags or UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
}
