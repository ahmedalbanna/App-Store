# Phase 4.5: R8 keep rules for Moshi, Room, Retrofit, Firebase, Media3
-keep class com.example.data.database.** { *; }
-keep class com.example.data.api.** { *; }
-keep class com.example.data.rag.** { *; }
-keep class androidx.room.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.firebase.** { *; }
-keep class androidx.media3.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**

# Add project specific ProGuard rules here.

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
