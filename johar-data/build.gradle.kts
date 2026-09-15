plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.akeshridev.johar.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":johar-domain"))

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("androidx.work:work-runtime:2.11.2")
    implementation("org.jsoup:jsoup:1.23.2")

    // Official LiteRT-LM Android Maven package. Keep model delivery separate from the APK.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.0")

    testImplementation("org.json:json:20240303")
    testImplementation(kotlin("test-junit"))
}
