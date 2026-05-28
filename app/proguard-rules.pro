# ProGuard rules for SuvMusic

# Keep NewPipe Extractor classes
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Mozilla Rhino (used by NewPipe) - ignore missing Java SE classes
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**
-dontwarn javax.script.**

# Ignore missing Java SE classes not available on Android
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.naming.**

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep field names on data models so Gson reflection works, but allow
# R8 to obfuscate the class names themselves (less surface for static
# analysis to identify the upstream payload shape).
-keepclassmembers,allowobfuscation class com.suvojeet.suvmusic.data.model.** {
    <fields>;
}
-keepclassmembers,allowobfuscation class com.suvojeet.suvmusic.core.model.** {
    <fields>;
}
-keepclassmembers,allowobfuscation class com.suvojeet.suvmusic.data.repository.remote.** {
    <fields>;
}

# Strip android.util.Log calls in the release build. Log tags are
# otherwise visible in DEX strings and leak code-path names.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Flatten the obfuscated package hierarchy and let R8 widen accessors
# where it helps shrinking. Strips package-name hints from the DEX.
-repackageclasses ''
-allowaccessmodification

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# General Android rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-dontwarn com.google.re2j.**
# Ktor Client
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Slf4j (used by Ktor, but we don't need it on Android)
-dontwarn org.slf4j.**
-dontwarn org.apache.log4j.**

# JAudioTagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn javax.imageio.**
-dontwarn java.awt.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.suvojeet.suvmusic.**$$serializer { *; }
-keepclassmembers class com.suvojeet.suvmusic.** {
    *** Companion;
}
-keepclasseswithmembers class com.suvojeet.suvmusic.** {
    kotlinx.serialization.KSerializer serializer(...);
}

## Protobuf
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

## Listen Together Protobuf
-keep class com.suvojeet.suvmusic.shareplay.proto.** { *; }
-keepclassmembers class com.suvojeet.suvmusic.shareplay.proto.** { *; }
