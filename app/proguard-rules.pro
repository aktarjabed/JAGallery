# Advanced Gallery ProGuard Rules

# Keep Room database classes and generated implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Hilt / Dagger generated components
-keep class * extends dagger.hilt.internal.UnsafeCasts
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Media3 PlayerView required for AndroidView integration
-keep class androidx.media3.ui.PlayerView { *; }

# Keep Kotlin annotations and signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
