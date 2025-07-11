# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.activity.ComponentActivity { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.google.android.datatransport.runtime.ForcedSender
-dontwarn com.google.android.gms.auth.api.credentials.Credential$Builder
-dontwarn com.google.android.gms.auth.api.credentials.Credential
-dontwarn com.google.android.gms.auth.api.credentials.CredentialRequest$Builder
-dontwarn com.google.android.gms.auth.api.credentials.CredentialRequest
-dontwarn com.google.android.gms.auth.api.credentials.CredentialRequestResponse
-dontwarn com.google.android.gms.auth.api.credentials.Credentials
-dontwarn com.google.android.gms.auth.api.credentials.CredentialsClient
-dontwarn com.google.android.gms.auth.api.credentials.CredentialsOptions$Builder
-dontwarn com.google.android.gms.auth.api.credentials.CredentialsOptions
-dontwarn com.google.android.gms.auth.api.credentials.HintRequest$Builder
-dontwarn com.google.android.gms.auth.api.credentials.HintRequest
