plugins { id("com.android.application") }
android {
    namespace = "com.instagram.android"
    compileSdk = 37
    defaultConfig { applicationId = "com.instagram.android"; minSdk = 29; targetSdk = 37; versionCode = 1; versionName = "fixture-only" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
