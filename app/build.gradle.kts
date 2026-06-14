import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}



// Credenciales de firma (archivo local, NO versionado)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.example.travelplannerai"
    compileSdk = 35

    signingConfigs {
        if (rootProject.file("keystore.properties").exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        // applicationId = identidad pública en Play Store (no puede ser com.example.*)
        // El namespace del código sigue siendo com.example.travelplannerai (no afecta a Play Store)
        applicationId = "com.borja.travelplannerai"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.1"

        // Leer keys desde local.properties
        val localProps = Properties()
        localProps.load(rootProject.file("local.properties").inputStream())

        // ⚠️ Las API keys ya NO van en el APK — viven en Cloud Functions (backend proxy)
        // Solo exponemos las URLs del proxy (que no son secretas)
        val chatProxyUrl  = localProps.getProperty("CHAT_PROXY_URL",  "")
        val photoProxyUrl = localProps.getProperty("PHOTO_PROXY_URL", "")

        buildConfigField("String", "CHAT_PROXY_URL",  "\"$chatProxyUrl\"")
        buildConfigField("String", "PHOTO_PROXY_URL", "\"$photoProxyUrl\"")

    }

    buildFeatures {
        buildConfig = true  // DEBE ESTAR AQUÍ
    }

    buildTypes {
        getByName("release") {
            // Firma automática si existe keystore.properties
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
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