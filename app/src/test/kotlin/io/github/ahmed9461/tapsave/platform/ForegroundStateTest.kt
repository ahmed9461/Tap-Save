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
    @Test fun oldActivityStoppingDoesNotHideNewActivityInSameApp() {
        val state = ForegroundState()
        state.resumed("com.instagram.android", "FeedActivity")
        state.resumed("com.instagram.android", "ReelActivity")
        state.paused("com.instagram.android", "FeedActivity")
        assertEquals("com.instagram.android", state.packageName)
        state.paused("com.instagram.android", "ReelActivity")
        assertNull(state.packageName)
    }
}
