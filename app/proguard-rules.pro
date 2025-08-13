# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Firebase Crashlytics ProGuard rules
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers
-keep public class * extends java.lang.Exception  # Keep custom exceptions

# Keep the classes used by Firebase Crashlytics
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep the CrashReporter class
-keep class com.example.myapplication.service.CrashReporter { *; }

# If you're using Kotlin, keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Koin for dependency injection
-keep class org.koin.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }