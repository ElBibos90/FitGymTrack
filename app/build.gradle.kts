
import java.util.*
fun getGitCommitCount(): Int {
    return try {
        val output = "git rev-list --count HEAD".runCommand()
        output.trim().toInt()
    } catch (e: Exception) {
        1
    }
}

fun getGitSha(): String {
    return try {
        "git rev-parse --short HEAD".runCommand().trim()
    } catch (e: Exception) {
        "dev"
    }
}

fun String.runCommand(): String {
    return ProcessBuilder(*split(" ").toTypedArray())
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .readText()
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fitgymtrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fitgymtrack.app"
        minSdk = 24
        targetSdk = 35
        versionCode = getGitCommitCount()
        versionName = "0.0.${getGitCommitCount()}-${getGitSha()}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Dipendenze standard Android
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navigazione Compose
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // Retrofit per le chiamate API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines per operazioni asincrone
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Coil per il caricamento delle immagini
    implementation("io.coil-kt:coil-compose:2.4.0")

    // DataStore per la memorizzazione sicura delle preferenze
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Coroutines per operazioni asincrone
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

// DataStore per la memorizzazione sicura delle preferenze
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")  // Per la serializzazione JSON

    // Compose
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

// Navigazione Compose
    implementation("androidx.navigation:navigation-compose:2.7.0")

    implementation("androidx.compose.material:material-icons-extended:1.7.2")

    // Compose Foundation per clickable e altri modificatori
    implementation("androidx.compose.foundation:foundation:1.7.2")

    implementation("androidx.compose.ui:ui:1.7.2")
    implementation("androidx.compose.material3:material3:1.3.0-alpha04")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.compose.foundation:foundation:1.7.2")

    implementation("io.github.vanpra.compose-material-dialogs:core:0.9.0")

// Per il NumberPicker
    implementation("com.chargemap.compose:numberpicker:1.0.3")

}

