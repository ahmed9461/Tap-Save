plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.ahmed9461.tapsave"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.ahmed9461.tapsave"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint {
        warningsAsErrors = true
        textReport = true
        textOutput = file("build/reports/lint-results-debug.txt")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
}
