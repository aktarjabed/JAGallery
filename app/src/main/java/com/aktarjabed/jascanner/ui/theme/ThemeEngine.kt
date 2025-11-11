package com.aktarjabed.jascanner.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Advanced theme engine with dynamic colors, wallpapers, and auto-theming
 */
object ThemeEngine {

    // Current theme state
    var currentTheme by mutableStateOf(AppTheme.SYSTEM)
    var wallpaperUri by mutableStateOf<Uri?>(null)
    var dominantColor by mutableStateOf(Color.Unspecified)
    var isGlassEffectEnabled by mutableStateOf(true)
    var isParallaxEnabled by mutableStateOf(true)

    // Theme definitions
    val themes = mapOf(
        AppTheme.SYSTEM to ThemeConfig("System", Color(0xFF6750A4)),
        AppTheme.LIGHT to ThemeConfig("Light", Color(0xFF6750A4)),
        AppTheme.DARK to ThemeConfig("Dark", Color(0xFFD0BCFF)),
        AppTheme.SUNSET to ThemeConfig("Sunset", Color(0xFFFF8A65)),
        AppTheme.AMOLED to ThemeConfig("AMOLED", Color(0xFF000000)),
        AppTheme.OCEAN to ThemeConfig("Ocean", Color(0xFF0097A7)),
        AppTheme.FOREST to ThemeConfig("Forest", Color(0xFF388E3C))
    )

    data class ThemeConfig(
        val name: String,
        val primaryColor: Color,
        val isDynamic: Boolean = false
    )

    /**
     * Auto-detect theme based on time of day
     */
    fun autoThemeByTime() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        currentTheme = when {
            hour in 6..10 -> AppTheme.LIGHT
            hour in 11..16 -> AppTheme.OCEAN
            hour in 17..20 -> AppTheme.SUNSET
            else -> AppTheme.DARK
        }
    }

    /**
     * Extract dominant color from wallpaper for dynamic theming
     */
    suspend fun extractDominantColor(context: Context, uri: Uri): Color {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    val color = extractDominantColorFromBitmap(bitmap)
                    bitmap.recycle()
                    color
                } ?: Color(0xFF6750A4) // Fallback color
            } catch (e: Exception) {
                Color(0xFF6750A4) // Fallback color
            }
        }
    }

    private fun extractDominantColorFromBitmap(bitmap: Bitmap): Color {
        // Simple dominant color extraction (for production, use Palette API)
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var r = 0L
        var g = 0L
        var b = 0L

        for (x in 0 until smallBitmap.width) {
            for (y in 0 until smallBitmap.height) {
                val pixel = smallBitmap.getPixel(x, y)
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
            }
        }

        val pixelCount = smallBitmap.width * smallBitmap.height
        val color = android.graphics.Color.rgb(
            (r / pixelCount).toInt(),
            (g / pixelCount).toInt(),
            (b / pixelCount).toInt()
        )

        smallBitmap.recycle()
        return Color(color)
    }

    /**
     * Apply wallpaper and extract colors
     */
    suspend fun applyWallpaper(context: Context, uri: Uri) {
        wallpaperUri = uri
        dominantColor = extractDominantColor(context, uri)
    }

    /**
     * Get current theme configuration
     */
    fun getCurrentThemeConfig(): ThemeConfig {
        return themes[currentTheme] ?: themes[AppTheme.SYSTEM]!!
    }

    /**
     * Blend colors for smooth transitions
     */
    fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
        val blended = ColorUtils.blendARGB(
            color1.toArgb(),
            color2.toArgb(),
            ratio
        )
        return Color(blended)
    }
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK, SUNSET, AMOLED, OCEAN, FOREST
}
