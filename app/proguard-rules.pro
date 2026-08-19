# Advanced Gallery ProGuard Rules

# Keep Room database classes and generated implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Hilt / Dagger generated components
-keep class * extends dagger.hilt.internal.UnsafeCasts
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper$ThreadUtil { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Media3 / ExoPlayer reflection used by UI views
-keep class androidx.media3.ui.PlayerView { *; }
-keep class androidx.media3.exoplayer.** { *; }

# Keep Kotlin serialization and reflection data if needed
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Compose primitives
-keep class androidx.compose.material3.** { *; }
