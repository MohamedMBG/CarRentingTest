plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.carrentingtest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carrentingtest"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    implementation(libs.picasso)  // Add this line for Picasso

    implementation(libs.android.mail)
    implementation(libs.android.activation)

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

    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Google AI (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:31.1-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // UI Animations
    implementation("com.airbnb.android:lottie:6.0.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
