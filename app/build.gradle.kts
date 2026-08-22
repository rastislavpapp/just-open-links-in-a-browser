plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.rastislavpapp.justopenlinksinabrowser"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.rastislavpapp.justopenlinksinabrowser"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // AppCompat makes the action bar part of the view hierarchy rather than
    // window decor, so it takes part in normal layout and inset handling.
    // activity provides enableEdgeToEdge(), which applies the same edge to edge
    // behaviour on every release instead of only on Android 15 and later.
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
}

// Kotlin is compiled by AGP's built-in Kotlin support, so no Kotlin plugin
// is applied and jvmTarget follows compileOptions.targetCompatibility above.
