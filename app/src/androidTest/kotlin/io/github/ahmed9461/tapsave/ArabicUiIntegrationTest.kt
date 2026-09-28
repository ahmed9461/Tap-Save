package io.github.ahmed9461.tapsave

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.View
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.SaveUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ArabicUiIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun arabicResourcesCoverNotificationsErrorsAndPluralForms() {
        val ar = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) })
        assertEquals(View.LAYOUT_DIRECTION_RTL, ar.resources.configuration.layoutDirection)
        for (id in listOf(R.string.overlay_notification, R.string.save_storage, R.string.save_auth_required,
            R.string.save_rate_limited, R.string.login_instructions, R.string.accessibility_description)) {
            assertTrue(ar.getString(id).any { it in '\u0600'..'\u06ff' })
            assertNotEquals(context.getString(id), ar.getString(id))
        }
        for (count in listOf(0, 1, 2, 3, 11, 100)) {
            val text = ar.resources.getQuantityString(R.plurals.setup_remaining, count, count)
            assertFalse(text.contains("%d"))
            assertTrue(text.any { it in '\u0600'..'\u06ff' })
        }
    }

    @Test fun nativeAppLocaleRendersArabicSetupHomeAndSettingsAndPersists() {
        // System-managed per-app language is supported from API 33. Older devices use device locale.
        if (Build.VERSION.SDK_INT < 33) { arabicResourcesCoverNotificationsErrorsAndPluralForms(); return }
        val manager = context.getSystemService(LocaleManager::class.java)
        val previous = manager.applicationLocales
        context.getSharedPreferences("setup", 0).edit().clear().commit()
        context.getSharedPreferences("latest-save", 0).edit().clear().commit()
        instrumentation.runOnMainSync { SaveUiState.current = null; manager.applicationLocales = LocaleList.forLanguageTags("ar") }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.onNodeWithText("احفظ بضغطة واحدة").assertIsDisplayed()
                compose.onNodeWithText("استخدام المشاركة حاليًا").assertIsDisplayed()
                captureUi("arabic-setup", compose.onRoot().captureToImage().asAndroidBitmap())
                scenario.onActivity { assertEquals(View.LAYOUT_DIRECTION_RTL, it.resources.configuration.layoutDirection) }
                compose.onNodeWithText("استخدام المشاركة حاليًا").performClick()
                compose.onNodeWithText("Tap Save متوقف").assertIsDisplayed()
                captureUi("arabic-home", compose.onRoot().captureToImage().asAndroidBitmap())
                scenario.recreate()
                compose.onNodeWithContentDescription("الإعدادات").performClick()
                compose.onNodeWithText("اختيار اللغة").assertIsDisplayed()
                compose.onNodeWithText("إظهار الزر العائم").performScrollTo().assertIsDisplayed()
                captureUi("arabic-settings", compose.onRoot().captureToImage().asAndroidBitmap())
                compose.onNodeWithText("قطع الاتصال ومسح الجلسة").performScrollTo().assertIsDisplayed()
                captureUi("arabic-session-settings", compose.onRoot().captureToImage().asAndroidBitmap())
                assertEquals("ar", manager.applicationLocales[0].language)
            }
        } finally {
            instrumentation.runOnMainSync { manager.applicationLocales = previous }
        }
    }
}
