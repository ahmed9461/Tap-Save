package io.github.ahmed9461.tapsave.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundStateTest {
    @Test fun unknownStartsHidden() { assertNull(ForegroundState().packageName) }
    @Test fun appSwitchReplacesCandidate() {
        val state = ForegroundState()
        state.resumed("com.instagram.android")
        state.resumed("other.app")
        state.paused("com.instagram.android")
        assertEquals("other.app", state.packageName)
    }
    @Test fun pauseClearsCurrentApp() {
        val state = ForegroundState()
        state.resumed("com.instagram.android")
        state.paused("com.instagram.android")
        assertNull(state.packageName)
    }
    @Test fun lockClearsStaleCandidate() {
        val state = ForegroundState()
        state.resumed("com.instagram.android")
        state.clear()
        assertNull(state.packageName)
    }
}
