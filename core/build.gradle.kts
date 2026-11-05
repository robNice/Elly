plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "de.robnice.elly.core"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.android)
    api(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
