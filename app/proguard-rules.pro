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
