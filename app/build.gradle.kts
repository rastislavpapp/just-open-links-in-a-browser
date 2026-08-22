import java.util.Properties

plugins {
    id("com.android.application")
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "io.github.rastislavpapp.justopenlinksinabrowser"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.rastislavpapp.justopenlinksinabrowser"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeType = "PKCS12"
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")

            // AGP stamps the current git commit into the APK by default. F-Droid
            // builds from a shallow checkout of the release tag, so that stamp
            // would differ from ours and the build would not be reproducible.
            vcsInfo { include = false }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
