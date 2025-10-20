plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    implementation(libs.material) // or newer


    // Concurrent futures used by some camera utils
    implementation(libs.concurrent.futures)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}