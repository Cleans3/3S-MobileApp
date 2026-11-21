// Macrobenchmark module for study E2. Drives :optlab from outside (self-instrumenting test APK); never ships.
plugins {
    id("com.android.test")
}

android {
    namespace = "shelftap.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":optlab"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Pairs with :optlab release (minified). The noR8 target is driven by the same APK via -e st.target.
        create("release") { signingConfig = signingConfigs.getByName("debug") }
    }
}

// Only the release variant is a valid benchmark target (non-debuggable, profileable, R8 on).
androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "release" }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
