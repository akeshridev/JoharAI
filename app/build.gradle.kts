plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.akeshridev.johar"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.akeshridev.johar"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    implementation(project(":johar-domain"))
    implementation(project(":johar-data"))
    implementation(project(":johar-design-system"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Offline Ranchi map rendering. Local PMTiles support is built into MapLibre Native.
    implementation("org.maplibre.gl:android-sdk:13.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
