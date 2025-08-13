# Indoor Positioning System Deployment Guide

This document provides comprehensive instructions for deploying the indoor positioning system application to production environments. It covers build configuration, deployment options, and best practices for ensuring optimal performance and reliability.

## Prerequisites

Before deploying the application, ensure you have the following:

- Android Studio Flamingo (2023.2.1) or newer
- JDK 11 or newer
- Gradle 8.0 or newer
- Android SDK with API level 35 (compileSdk)
- Google Play Developer account (for Play Store deployment)
- Signing keys for the application

## Build Configuration

### Production Build Settings

1. Open the project in Android Studio
2. Navigate to `app/build.gradle.kts` and ensure the following settings are configured:

```kotlin
android {
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    signingConfigs {
        create("release") {
            // Configure your signing information
            storeFile = file("../keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
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
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Performance Optimization

For optimal performance in production builds:

1. Enable R8 full mode for better code optimization:

```kotlin
android {
    buildTypes {
        release {
            // Enable R8 full mode
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isShrinkResources = true
            isMinifyEnabled = true
        }
    }
}
```

2. Add the following to your `proguard-rules.pro` file to preserve essential classes:

```
# Keep sensor and positioning related classes
-keep class com.example.myapplication.domain.model.** { *; }
-keep class com.example.myapplication.data.repository.** { *; }

# Keep Wi-Fi and BLE related classes
-keep class com.example.myapplication.wifi.** { *; }

# Keep SLAM related classes
-keep class com.example.myapplication.slam.** { *; }
```

## Building the Release APK

### Command Line Build

To build a release APK from the command line:

```bash
# Clean the project
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Build release bundle (for Play Store)
./gradlew bundleRelease
```

The release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

The release bundle will be located at:
`app/build/outputs/bundle/release/app-release.aab`

### Android Studio Build

To build a release APK from Android Studio:

1. Select `Build > Generate Signed Bundle / APK` from the menu
2. Choose `APK` or `Android App Bundle` depending on your deployment target
3. Select your keystore and enter your credentials
4. Choose the release build variant
5. Click `Finish` to generate the signed APK or bundle

## Deployment Options

### Google Play Store

For deploying to the Google Play Store:

1. Create a developer account at [play.google.com/apps/publish](https://play.google.com/apps/publish)
2. Create a new application
3. Fill in the store listing details, including:
   - App name
   - Short and full descriptions
   - Screenshots and feature graphic
   - Category and content rating
4. Upload the signed AAB file
5. Set up pricing and distribution
6. Submit for review

### Enterprise Deployment

For enterprise deployment:

1. Set up an MDM (Mobile Device Management) solution
2. Upload the signed APK to your MDM
3. Configure deployment policies
4. Push the application to managed devices

### Direct APK Distribution

For direct APK distribution:

1. Host the signed APK on a secure server
2. Provide installation instructions to users
3. Ensure devices have "Install from Unknown Sources" enabled for your distribution method

## Environment Configuration

### Wi-Fi Fingerprinting Setup

Before deploying to a new environment:

1. Conduct a Wi-Fi survey using the built-in survey tool
2. Collect fingerprints at key locations throughout the environment
3. Export the fingerprint database
4. Include the fingerprint database in the assets folder before building

### Map Configuration

To configure maps for a new environment:

1. Create floor plan images in PNG format
2. Define coordinate systems and scale factors
3. Place the map files in the `app/src/main/assets/maps/` directory
4. Update the `maps.json` configuration file with the new maps

## Performance Monitoring

### Firebase Integration

To monitor application performance in production:

1. Add Firebase Performance Monitoring to your project:

```kotlin
dependencies {
    // Firebase Performance Monitoring
    implementation("com.google.firebase:firebase-perf-ktx:20.3.1")
}
```

2. Configure Firebase in your `google-services.json` file
3. Add custom traces for critical operations:

```kotlin
private fun monitorPositioningPerformance() {
    val trace = FirebasePerformance.getInstance().newTrace("positioning_calculation")
    trace.start()
    
    // Positioning calculation code
    
    trace.stop()
}
```

### Crash Reporting

To track and fix crashes in production:

1. Add Firebase Crashlytics to your project:

```kotlin
dependencies {
    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.3.7")
}
```

2. Configure custom keys for better crash context:

```kotlin
Crashlytics.setCustomKey("environment_type", environmentClassifier.getCurrentEnvironment().name)
Crashlytics.setCustomKey("positioning_method", currentPositioningMethod)
```

## Updating the Application

### In-App Updates

To support in-app updates:

1. Add the Play Core library:

```kotlin
dependencies {
    implementation("com.google.android.play:core:1.10.3")
}
```

2. Implement the update flow in your main activity:

```kotlin
private fun checkForUpdates() {
    val appUpdateManager = AppUpdateManagerFactory.create(context)
    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.FLEXIBLE,
                this,
                REQUEST_CODE_UPDATE
            )
        }
    }
}
```

## Troubleshooting

### Common Deployment Issues

1. **Signing Issues**: Ensure your keystore is valid and passwords are correct
2. **Permissions Issues**: Verify all required permissions are declared in the manifest
3. **Compatibility Issues**: Test on a range of devices to ensure compatibility
4. **Performance Issues**: Use the built-in performance monitoring tools to identify bottlenecks

### Support Procedures

For production support:

1. Establish a support email or ticketing system
2. Document common issues and resolutions
3. Create a process for emergency updates
4. Set up monitoring alerts for critical issues

## Security Considerations

### Data Protection

1. Ensure all sensitive data is encrypted at rest
2. Use secure communication channels for data transmission
3. Implement proper authentication for admin functions
4. Regularly audit security practices

### Privacy Compliance

1. Ensure the application complies with relevant privacy regulations (GDPR, CCPA, etc.)
2. Include a privacy policy that clearly explains data collection and usage
3. Provide users with options to control data collection
4. Implement data retention policies

## Conclusion

Following these deployment procedures will ensure a smooth rollout of the indoor positioning system to production environments. Regular monitoring and updates will help maintain optimal performance and user satisfaction.

For any questions or issues during deployment, contact the development team at support@example.com.