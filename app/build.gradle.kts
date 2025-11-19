plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aktarjabed.jagallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aktarjabed.jagallery"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Ndk configuration for native libraries
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/license.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/notice.txt",
                "/META-INF/INDEX.LIST",
                "/META-INF/*.kotlin_module"
            )
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
            pickFirsts += setOf(
                "lib/x86/libtensorflowlite_jni.so",
                "lib/x86_64/libtensorflowlite_jni.so",
                "lib/armeabi-v7a/libtensorflowlite_jni.so",
                "lib/arm64-v8a/libtensorflowlite_jni.so"
            )
        }
    }

    // Optional: generate per-ABI APKs to reduce download size
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {
    // ============== Core Modules ==============
    implementation(project(":core-data"))
    implementation(project(":core-ai"))
    implementation(project(":core-security"))
    implementation(project(":core-sync"))
    implementation(project(":core-ui"))

    // ============== Feature Modules ==============
    implementation(project(":feature-album"))
    implementation(project(":feature-camera"))
    implementation(project(":feature-details"))
    implementation(project(":feature-duplicates"))
    implementation(project(":feature-editor"))
    implementation(project(":feature-search"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-timeline"))
    implementation(project(":feature-vault"))
    implementation(project(":feature-ai"))

    // ============== Compose BOM & UI ==============
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ============== Core Android ==============
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // ============== Permissions & Accompanist ==============
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // ============== Image Loading ==============
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ============== Room Database ==============
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ============== WorkManager ==============
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ============== Security & Biometrics ==============
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ============== TensorFlow Lite AI ==============
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    // Uncomment for GPU acceleration (adds ~15MB to APK)
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // ============== ML Kit OCR ==============
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // ============== Coroutines ==============
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ============== ExifInterface ==============
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ============== Testing ==============
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Optional: Configure Android Test options
tasks.withType<Test> {
    useJUnitPlatform()
}
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
