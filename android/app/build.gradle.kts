plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun prop(name: String, default: String) = providers.gradleProperty(name).orElse(default).get().trim()

// Dev server from the emulator. Release has no usable default: it must be set with -Pshelftap.apiBaseUrl.
val debugApiBaseUrl = prop("shelftap.apiBaseUrl", "https://10.0.2.2:8443/")
val releaseApiBaseUrl = prop("shelftap.apiBaseUrl", "https://api.shelftap.example/")
val apiPins = prop("shelftap.apiPins", "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
val signingCertSha256 = prop("shelftap.signingCertSha256", "").replace(":", "").lowercase()
val inactivityMinutes = prop("shelftap.inactivityMinutes", "5").toInt()

require(debugApiBaseUrl.startsWith("https://") && releaseApiBaseUrl.startsWith("https://")) {
    "shelftap.apiBaseUrl must be https://"
}
require(debugApiBaseUrl.endsWith("/")) { "shelftap.apiBaseUrl must end with /" }
apiPins.forEach { require(Regex("sha256/[A-Za-z0-9+/]{43}=").matches(it)) { "bad pin '$it', want sha256/<44 base64 chars>" } }
require(signingCertSha256.isEmpty() || Regex("[0-9a-f]{64}").matches(signingCertSha256)) {
    "shelftap.signingCertSha256 must be 64 hex chars"
}
if (apiPins.isEmpty() && gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
    logger.warn("WARNING: release built WITHOUT certificate pinning (shelftap.apiPins is empty)")
}

android {
    namespace = "shelftap.android"
    // Compose 1.12, lifecycle 2.11, navigation 2.10, coil 3.6 and OkHttp 5.5 all require compileSdk 37; behaviour stays at targetSdk 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "shelftap.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("long", "INACTIVITY_LIMIT_MS", "${inactivityMinutes * 60_000L}L")
        buildConfigField("String", "SIGNING_CERT_SHA256", "\"$signingCertSha256\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            // Dev TLS is a user-installed CA (network_security_config debug-overrides), so debug never pins.
            buildConfigField("String", "API_PINS", "\"\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("String", "API_PINS", "\"${apiPins.joinToString(",")}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all { it.useJUnit() }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlin.test.junit)
}
