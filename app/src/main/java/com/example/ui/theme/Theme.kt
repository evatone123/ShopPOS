package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // BrandIndigoLight - bright, vivid indigo on dark
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF06B6D4), // BrandCyan
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFFF59E0B), // BrandAmber
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF090D16), // Deep crisp dark
    onBackground = Color(0xFFF8FAFC), // Pure readable high-contrast white text
    surface = Color(0xFF0F172A), // Dark slate surface
    onSurface = Color(0xFFF8FAFC), // Pure readable high-contrast white text
    surfaceVariant = Color(0xFF1E293B), // Elevated slate container
    onSurfaceVariant = Color(0xFF94A3B8), // Readable silver-slate secondary text
    surfaceContainerHighest = Color(0xFF334155),
    outline = Color(0xFF475569), // Visible slate border
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFB7185), // Radiant soft rose
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // BrandIndigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0891B2), // BrandCyanDark
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFFD97706), // BrandAmberDark
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC), // Soft clean off-white
    onBackground = Color(0xFF0F172A), // Sharp high-contrast dark slate text
    surface = Color(0xFFFFFFFF), // Pure white card surface
    onSurface = Color(0xFF0F172A), // Sharp high-contrast dark slate text
    surfaceVariant = Color(0xFFF1F5F9), // Soft contrast container
    onSurfaceVariant = Color(0xFF475569), // Clean secondary text
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1), // Clean border
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48), // BrandRoseDark
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
