import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}



android {
    namespace = "com.example.travelplannerai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.travelplannerai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Leer keys desde local.properties
        val localProps = Properties()
        localProps.load(rootProject.file("local.properties").inputStream())

        val openAiKey     = localProps.getProperty("OPENAI_API_KEY",     "")
        val unsplashKey   = localProps.getProperty("UNSPLASH_API_KEY",   "")


        buildConfigField("String", "OPENAI_API_KEY",     "\"$openAiKey\"")
        buildConfigField("String", "UNSPLASH_API_KEY",   "\"$unsplashKey\"")

    }

    buildFeatures {
        buildConfig = true  // DEBE ESTAR AQUÍ
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.glide)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)



    // HTTP Client for Gemini API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}