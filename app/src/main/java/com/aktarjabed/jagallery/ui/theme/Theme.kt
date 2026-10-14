package com.aktarjabed.jagallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val PrimaryPurple = Color(0xFFA855F7)
val PrimaryPurpleLight = Color(0xFFC084FC)
val PrimaryContainerDark = Color(0xFF34154E)
val BackgroundDark = Color(0xFF0E0C12)
val SurfaceDark = Color(0xFF16141D)
val SurfaceVariantDark = Color(0xFF1E1A27)
val OnSurfaceDark = Color(0xFFF5F3FA)
val OnSurfaceVariantDark = Color(0xFFA39EAF)
val OutlineDark = Color(0xFF2E293A)
val TrashRed = Color(0xFFF43F5E)
val TrashRedContainer = Color(0xFF3A141D)

private val CustomDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.Black,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = PrimaryPurpleLight,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = TrashRed,
    errorContainer = TrashRedContainer
)

private val CustomLightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = PrimaryPurple,
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

@Composable
fun AdvancedGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CustomDarkColorScheme
        else -> CustomLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
