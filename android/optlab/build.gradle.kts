// Lab app for study E2: paired slop (A) / tuned (B) screens per taxonomy factor. NEVER ship; the product is :app.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "shelftap.optlab"
    compileSdk = 37

    defaultConfig {
        applicationId = "shelftap.optlab"
        minSdk = 29 // android.os.Trace.setCounter
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        // B of P10: what a release should be. Profileable (manifest) but not debuggable, so Macrobenchmark accepts it.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        // A of P10: same code, R8 and resource shrinking off. Own application id so both can be installed side by side.
        create("noR8") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".nor8"
            matchingFallbacks += "release"
        }
    }

    buildFeatures { compose = true }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.profileinstaller)
}
