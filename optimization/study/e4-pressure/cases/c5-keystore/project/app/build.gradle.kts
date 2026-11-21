plugins {
    id("com.android.application")
}

android {
    namespace = "vn.shelftap.lab"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.shelftap.lab"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://api.shelftap.example/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.shelftap.example/\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("androidx.biometric:biometric:1.1.0")
}
