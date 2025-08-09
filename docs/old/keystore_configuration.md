# Debug Keystore Configuration

## Issue
The project was originally configured to use a custom debug keystore located at `app/debug.keystore`. This caused build failures when the keystore file was missing, with the error:

```
Execution failed for task ':app:validateSigningDebug'.
> Keystore file 'C:\Users\schoo\Desktop\java\Android_gps\app\debug.keystore' not found for signing config 'debug'.
```

## Solution
The build configuration was modified to use the default Android debug keystore instead of a custom one. This was done by removing the explicit configuration for the debug signing config in `app/build.gradle.kts`:

```kotlin
getByName("debug") {
    // Use default debug keystore
}
```

## Default Debug Keystore Location
Android Studio automatically creates and manages a debug keystore at the following locations:

- Windows: `%USERPROFILE%\.android\debug.keystore`
- macOS/Linux: `~/.android/debug.keystore`

## Debug Keystore Information
The default debug keystore uses the following credentials:
- Keystore password: `android`
- Key alias: `androiddebugkey`
- Key password: `android`
- Validity: 10,000 days

## Creating a Custom Debug Keystore
If you need to create a custom debug keystore, you can use the following command:

```
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

Note: The `keytool` command is part of the Java Development Kit (JDK). Make sure the JDK's bin directory is in your system PATH or use the full path to the keytool executable.