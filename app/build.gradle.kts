plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
}

// Apply the version script
apply(from = "../version.gradle")

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    signingConfigs {
        create("release") {
            // For CI environment, we'll use environment variables
            // For local development, we'll use the debug keystore
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        getByName("debug") {
            // Use default debug keystore
        }
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 30
        targetSdk = 35
        // Version code and name will be set by the version.gradle script
        // Default values in case the script fails
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // Versioning will be handled by the CI/CD pipeline
    // For local development, we'll use the default values

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isDebuggable = true
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // For CI, we'll use debug signing
        }
        
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Compose dependencies
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // Bluetooth LE
    implementation("no.nordicsemi.android:ble:2.6.1")
    implementation("no.nordicsemi.android:ble-ktx:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Logging with Timber
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Firebase for crash reporting and analytics
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-perf-ktx")
    
    // Sensor fusion and math libraries
    implementation("org.apache.commons:commons-math3:3.6.1")
    
    // JSON parsing
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    
    // Dependency Injection with Koin
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0")
    
    // Window size classes for responsive layouts
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    
    // WorkManager for background processing
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("io.insert-koin:koin-test:3.5.0")
    androidTestImplementation("io.insert-koin:koin-test-junit4:3.5.0")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}