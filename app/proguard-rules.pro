# Keep TFLite task entrypoints
-keep class org.tensorflow.lite.task.vision.classifier.** { *; }
-keepclassmembers class org.tensorflow.lite.task.vision.classifier.** { *; }
-keep class org.tensorflow.lite.support.image.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn org.tensorflow.lite.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Compose runtime lambdas
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * extends kotlin.jvm.internal.Lambda { *; }

# ML Kit (keep)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# General
-keepclassmembers class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
-keep class **.R$* { *; }
# =================================================================
# Minimal, safe ProGuard / R8 rules tailored for TensorFlow Lite Task,
# Coil, Kotlin coroutines, and Jetpack Compose. Keep as small as possible.
# =================================================================

# ---------- TensorFlow Lite Task (Task + Support) ----------
# Keep only Task API entry points + model support classes relied on via reflection.
-keep class org.tensorflow.lite.task.vision.classifier.** { *; }
-keepclassmembers class org.tensorflow.lite.task.vision.classifier.** { *; }
-keep class org.tensorflow.lite.support.image.** { *; }

# Keep native methods used by TFLite
-keepclasseswithmembernames class * {
    native <methods>;
}

# Prevent warnings about optional TFLite native classes
-dontwarn org.tensorflow.lite.**

# ---------- Coil ----------
-keep class coil.** { *; }
-dontwarn coil.**

# ---------- Kotlin Coroutines ----------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

# ---------- Jetpack Compose ----------
# Keep Compose runtime entrypoints and lambda subclasses used by Compose.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-keepclassmembers class * extends kotlin.jvm.internal.Lambda { *; }

# If you use Compose navigation or other compose libraries, add explicit keeps for them if needed.

# ---------- Android essentials ----------
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ---------- Resources ----------
# Keep resource class used by reflection-based libraries if any
-keep class **.R$* { *; }

# ---------- Keep model assets from being stripped by resource shrinker ----------
# Resource shrinker shouldn't strip assets, but keep raw references if necessary.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------- Keep warnings (safe suppressions) ----------
-dontwarn ai.onnxruntime.**
