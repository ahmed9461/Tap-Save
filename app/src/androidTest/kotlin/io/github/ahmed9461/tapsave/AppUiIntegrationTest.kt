package io.github.ahmed9461.tapsave

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.SaveUiState
import io.github.ahmed9461.tapsave.overlay.OverlayPreferences
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class AppUiIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    @Before fun cleanUiState() {
        context.getSharedPreferences("setup", 0).edit().clear().commit()
        context.getSharedPreferences("latest-save", 0).edit().clear().commit()
        instrumentation.runOnMainSync { SaveUiState.current = null }
    }
    @After fun restoreAppearance() { context.getSharedPreferences("overlay", 0).edit().clear().commit() }
    @Test fun firstRunShowsOnePermissionAndHomeKeepsSettingsOutOfTheWay() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Make saving one tap").assertIsDisplayed()
            compose.onNodeWithText("Open Android settings").assertIsDisplayed()
            compose.onNodeWithText("Required permissions").assertDoesNotExist()
            screenshot("setup")
            compose.onNodeWithText("Use Share for now").performClick()
            compose.onNodeWithTag("master-toggle").assertIsDisplayed().assertIsOff()
            compose.onNodeWithText("Connect Instagram").assertDoesNotExist()
            compose.onNodeWithText("Acquisition diagnostics").assertDoesNotExist()
            screenshot("home")
            scenario.recreate()
            compose.onNodeWithTag("master-toggle").assertIsDisplayed()
            compose.onNodeWithTag("master-toggle").performClick()
            compose.onNodeWithText("Make saving one tap").assertIsDisplayed()
        }
    }
    @Test fun appearanceSettingsPersistAndDiagnosticsStayUnderAdvanced() {
        context.getSharedPreferences("setup", 0).edit().putBoolean("seen", true).commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Acquisition diagnostics").assertDoesNotExist()
            compose.onNodeWithTag("button-size").performSemanticsAction(SemanticsActions.SetProgress) { it(64f) }
            compose.onNodeWithTag("button-opacity").performSemanticsAction(SemanticsActions.SetProgress) { it(.6f) }
            compose.waitForIdle()
            assertEquals(64, OverlayPreferences(context).sizeDp)
            assertEquals(.6f, OverlayPreferences(context).opacity, .01f)
            screenshot("settings")
            compose.onNodeWithText("Advanced").performScrollTo().performClick()
            compose.onNodeWithText("Acquisition diagnostics").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Copy report").performScrollTo().assertIsDisplayed()
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val image = instrumentation.uiAutomation.takeScreenshot() ?: error("No screenshot")
        val directory = File(context.getExternalFilesDir(null), "ui-checks").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
