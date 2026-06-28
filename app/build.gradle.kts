import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun readStringConfig(name: String): String {
    return providers.gradleProperty(name).orNull
        ?: System.getenv(name)
        ?: localProperties.getProperty(name, "")
}

fun escapeForBuildConfig(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}

val backendBaseUrl = readStringConfig("BACKEND_BASE_URL").trim()
val escapedBackendBaseUrl = escapeForBuildConfig(backendBaseUrl)

// Legal URLs are remote so the policy text can change without an app update.
// Defaults point at placeholder paths under the configured backend; override per env.
val privacyPolicyUrl = readStringConfig("PRIVACY_POLICY_URL").trim()
    .ifBlank { "https://example.com/legal/privacy" }
val tosUrl = readStringConfig("TOS_URL").trim()
    .ifBlank { "https://example.com/legal/terms" }
val escapedPrivacyPolicyUrl = escapeForBuildConfig(privacyPolicyUrl)
val escapedTosUrl = escapeForBuildConfig(tosUrl)

// applicationId is overridable via gradle property / env / local.properties so the
// Play Store id can be rotated to a real brand domain without editing build files.
// Default kept for backwards compatibility until namespace rename lands.
val defaultApplicationId = "com.example.carrentingtest"
val configuredApplicationId = readStringConfig("APPLICATION_ID").trim()
val effectiveApplicationId = configuredApplicationId.ifBlank { defaultApplicationId }

android {
    namespace = "com.example.carrentingtest"
    compileSdk = 35

    defaultConfig {
        applicationId = effectiveApplicationId
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["allowBackup"] = "true"
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("String", "BACKEND_BASE_URL", "\"$escapedBackendBaseUrl\"")
            buildConfigField("String", "CONCIERGE_ENDPOINT_PATH", "\"/v1/mobile/concierge\"")
            buildConfigField("String", "NOTIFICATION_ENDPOINT_PATH", "\"/v1/mobile/notifications/email\"")
            buildConfigField("String", "EXPORT_ENDPOINT_PATH", "\"/v1/user/export\"")
            buildConfigField("String", "DELETE_ENDPOINT_PATH", "\"/v1/user/delete\"")
            buildConfigField("String", "PRIVACY_POLICY_URL", "\"$escapedPrivacyPolicyUrl\"")
            buildConfigField("String", "TOS_URL", "\"$escapedTosUrl\"")
            buildConfigField("boolean", "REQUIRE_HTTPS_BACKEND", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            manifestPlaceholders["allowBackup"] = "false"
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "BACKEND_BASE_URL", "\"$escapedBackendBaseUrl\"")
            buildConfigField("String", "CONCIERGE_ENDPOINT_PATH", "\"/v1/mobile/concierge\"")
            buildConfigField("String", "NOTIFICATION_ENDPOINT_PATH", "\"/v1/mobile/notifications/email\"")
            buildConfigField("String", "EXPORT_ENDPOINT_PATH", "\"/v1/user/export\"")
            buildConfigField("String", "DELETE_ENDPOINT_PATH", "\"/v1/user/delete\"")
            buildConfigField("String", "PRIVACY_POLICY_URL", "\"$escapedPrivacyPolicyUrl\"")
            buildConfigField("String", "TOS_URL", "\"$escapedTosUrl\"")
            buildConfigField("boolean", "REQUIRE_HTTPS_BACKEND", "true")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/INDEX.LIST"
            )
        }
    }

    lint {
        disable += "MissingTranslation"
    }

}

afterEvaluate {
    val runningReleaseTask = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("release", ignoreCase = true)
    }

    if (runningReleaseTask) {
        check(backendBaseUrl.isNotBlank()) {
            "BACKEND_BASE_URL must be set for release builds."
        }
        check(backendBaseUrl.startsWith("https://")) {
            "BACKEND_BASE_URL must use HTTPS for release builds."
        }
        // Block release builds from shipping with the placeholder applicationId.
        // Why: prevents accidental Play Store upload under com.example.* which is
        // unowned and cannot be claimed later.
        check(effectiveApplicationId != defaultApplicationId) {
            "APPLICATION_ID must be overridden (not com.example.*) for release builds."
        }
        // Legal URLs must point at a real published policy in production.
        // Why: shipping with example.com placeholder violates app-store policy and GDPR transparency.
        check(!privacyPolicyUrl.contains("example.com")) {
            "PRIVACY_POLICY_URL must be set to a real URL for release builds."
        }
        check(!tosUrl.contains("example.com")) {
            "TOS_URL must be set to a real URL for release builds."
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.config)
    implementation(libs.androidx.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.picasso)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    implementation(libs.mpandroidchart)

    // ML Kit Face Detection
    implementation(libs.mlkit.facedetection)
    implementation(libs.tensorflow.lite)

    // Excel export support
    implementation(libs.poi.ooxml.lite)
    implementation(libs.poi.ooxml)

    // CameraX (basic deps; can be expanded later)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // Concurrent futures used by some camera utils
    implementation(libs.concurrent.futures)

    implementation(platform(libs.firebase.bom))

    implementation("com.google.guava:guava:31.1-android")

    // UI Animations
    implementation("com.airbnb.android:lottie:6.0.0")
}
