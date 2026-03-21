# ScrollLight ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }

# Keep Room entities
-keep class com.scrolllight.bible.data.model.** { *; }
-keep class com.scrolllight.bible.data.db.** { *; }

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep AI interface
-keep interface com.scrolllight.bible.ai.AiService { *; }
-keep class com.scrolllight.bible.ai.** { *; }

# Keep Navigation
-keepnames class androidx.navigation.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
