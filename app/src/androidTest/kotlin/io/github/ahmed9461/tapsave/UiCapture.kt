package io.github.ahmed9461.tapsave

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** AGP collects this directory before uninstalling the test application. */
internal fun captureUi(name: String, image: Bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No screenshot")) {
    val directory = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
    check(directory.isDirectory || directory.mkdirs())
    try { File(directory, "$name.png").outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
    finally { image.recycle() }
}
