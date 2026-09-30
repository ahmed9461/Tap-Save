import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersion = Properties().apply { file("version.properties").inputStream().use { load(it) } }

android {
    namespace = "io.github.ahmed9461.tapsave"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.ahmed9461.tapsave"
        minSdk = 29
        targetSdk = 37
        versionCode = providers.gradleProperty("tapSaveVersionCode").orElse(appVersion.getProperty("versionCode")).get().toInt()
        versionName = appVersion.getProperty("versionName")
        testInstrumentationRunner = "io.github.ahmed9461.tapsave.TapSaveTestRunner"
        testInstrumentationRunnerArguments["additionalTestOutputDir"] = "/sdcard/Android/media/io.github.ahmed9461.tapsave/ui-checks"
    }

    // Release signing is explicit: never fall back to the disposable debug key.
    val releaseStore = providers.environmentVariable("TAP_SAVE_KEYSTORE_PATH").orNull
    signingConfigs {
        if (releaseStore != null) create("personalRelease") {
            storeFile = file(releaseStore)
            storeType = "PKCS12"
            storePassword = providers.environmentVariable("TAP_SAVE_KEYSTORE_PASSWORD").get()
            keyAlias = "tap-save"
            keyPassword = storePassword
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            signingConfig = signingConfigs.findByName("personalRelease")
        }
    }

    // Missing credentials must not produce a deliverable unsigned release.
    tasks.matching { it.name == "validateSigningRelease" || it.name == "packageRelease" }.configureEach {
        doFirst { check(releaseStore != null) { "Release requires the existing Tap Save signing identity; see docs/SIGNING.md" } }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests.all { it.testLogging.events("passed", "failed", "skipped") }
    }
    lint {
        warningsAsErrors = true
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
