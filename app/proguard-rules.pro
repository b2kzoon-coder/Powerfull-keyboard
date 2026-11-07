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

# Keep Android entry points and InputMethodService
-keep class com.example.ime.PersonalKeyboardService { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.voice.VoicePermissionActivity { *; }

# Keep ML Kit Translate & Language Identification classes accessed via reflection/JNI
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.odml.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.odml.**

# Keep Update models and network classes
-keep class com.example.update.** { *; }

