
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
        debug {
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
            buildConfigField("int", "VERSION_CODE", "${defaultConfig.versionCode}")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
            buildConfigField("int", "VERSION_CODE", "${defaultConfig.versionCode}")
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
        buildConfig = true
    }
}

dependencies {
    // === CORE ANDROID ===
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // === COMPOSE ===
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.foundation)

    // === NAVIGATION ===
    implementation(libs.androidx.navigation.compose)

    // === NETWORKING ===
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // === ASYNC & COROUTINES ===
    implementation(libs.kotlinx.coroutines.android)

    // === LIFECYCLE & VIEWMODEL ===
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // === IMAGE LOADING ===
    implementation(libs.coil.compose)

    // === DATA STORAGE ===
    implementation(libs.androidx.datastore.preferences)

    // === UI COMPONENTS ===
    implementation(libs.compose.material.dialogs)
    implementation(libs.compose.numberpicker)

    // === TESTING ===
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // === DEBUG ===
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.compose.material3.window.size)
}

