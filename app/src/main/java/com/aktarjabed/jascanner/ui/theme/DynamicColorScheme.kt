package com.aktarjabed.jascanner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun dynamicColorScheme(
    theme: AppTheme = ThemeEngine.currentTheme,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColors: Boolean = true
): ColorScheme {
    val context = LocalContext.current

    return when (theme) {
        AppTheme.SYSTEM -> {
            if (useDynamicColors && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (isSystemInDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (isSystemInDarkTheme) darkColorScheme()
                else lightColorScheme()
            }
        }
        AppTheme.LIGHT -> lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
        AppTheme.DARK -> darkColorScheme(
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            tertiary = Color(0xFFEFB8C8)
        )
        AppTheme.SUNSET -> lightColorScheme(
            primary = Color(0xFFFF8A65),
            secondary = Color(0xFFFFB74D),
            tertiary = Color(0xFFFFD54F),
            background = Color(0xFFFFF3E0)
        )
        AppTheme.AMOLED -> darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF000000),
            surface = Color(0xFF121212)
        )
        AppTheme.OCEAN -> lightColorScheme(
            primary = Color(0xFF0097A7),
            secondary = Color(0xFF00BCD4),
            tertiary = Color(0xFFB2EBF2),
            background = Color(0xFFE0F7FA)
        )
        AppTheme.FOREST -> lightColorScheme(
            primary = Color(0xFF388E3C),
            secondary = Color(0xFF4CAF50),
            tertiary = Color(0xFFC8E6C9),
            background = Color(0xFFE8F5E8)
        )
    }.let { baseScheme ->
        // Apply dominant color from wallpaper if available
        if (ThemeEngine.dominantColor != Color.Unspecified) {
            baseScheme.copy(
                primary = ThemeEngine.blendColors(
                    baseScheme.primary,
                    ThemeEngine.dominantColor,
                    0.3f
                ),
                secondary = ThemeEngine.blendColors(
                    baseScheme.secondary,
                    ThemeEngine.dominantColor,
                    0.2f
                )
            )
        } else {
            baseScheme
        }
    }
}
