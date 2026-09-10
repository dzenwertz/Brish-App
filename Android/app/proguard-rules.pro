# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep attributes required for reflection, generics, and Firestore deserialization
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe

# --- CRITICAL FIX FOR FIREBASE, FIRESTORE, ROOM, GSON & RETROFIT ---
# Prevent R8 from obfuscating/stripping any model or data classes and their constructors
-keep class com.katchy.focuslive.data.** { *; }
-keepclassmembers class com.katchy.focuslive.data.** { *; }

-keep class com.katchy.focuslive.data.model.** { *; }
-keepclassmembers class com.katchy.focuslive.data.model.** { *; }

-keep class com.katchy.focuslive.ui.chat.** { *; }
-keepclassmembers class com.katchy.focuslive.ui.chat.** { *; }

# Keep all classes annotated with @Keep or @Immutable or @Entity
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ViewModel constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Hilt / Dagger
-keep class com.katchy.focuslive.FocusLiveApp { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Util package (OcrManager lives here)
-keep class com.katchy.focuslive.util.** { *; }

# Room
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.TypeConverter { *; }
-dontwarn androidx.room.paging.**

# Gson TypeTokens
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.katchy.focuslive.data.local.Converters { *; }
-keepclassmembers class com.katchy.focuslive.data.local.Converters { *; }

# Firebase Firestore reflection deserialization
-keepclassmembers class * {
    public <init>();
    public <init>(...);
}
-keep class com.google.firebase.firestore.** { *; }

# Firebase Crashlytics
-keep public class * extends java.lang.Exception

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**

# ML Kit (for OcrManager)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Compose Animation & Material3
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
