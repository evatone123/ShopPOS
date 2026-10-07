package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Retail POS Brand Palette - Modern, vibrant & clean
val BrandIndigo = Color(0xFF4F46E5)
val BrandIndigoDark = Color(0xFF3730A3)
val BrandIndigoLight = Color(0xFF818CF8)
val BrandIndigoContainer = Color(0xFFEEF2FF)

val BrandEmerald = Color(0xFF10B981)
val BrandEmeraldDark = Color(0xFF059669)
val BrandEmeraldContainer = Color(0xFFD1FAE5)

val BrandAmber = Color(0xFFF59E0B)
val BrandAmberDark = Color(0xFFD97706)
val BrandAmberContainer = Color(0xFFFEF3C7)

val BrandRose = Color(0xFFF43F5E)
val BrandRoseDark = Color(0xFFE11D48)
val BrandRoseContainer = Color(0xFFFFE4E6)

val BrandCyan = Color(0xFF06B6D4)
val BrandCyanDark = Color(0xFF0891B2)
val BrandCyanContainer = Color(0xFFCFFAFE)

val BrandPurple = Color(0xFF8B5CF6)
val BrandPurpleDark = Color(0xFF7C3AED)
val BrandPurpleContainer = Color(0xFFEDE9FE)

val BrandOrange = Color(0xFFF97316)
val BrandOrangeContainer = Color(0xFFFFEDD5)

// Backward compatible aliases
val PrimaryBlue = BrandIndigo
val PrimaryBlueLight = BrandIndigoLight
val PrimaryBlueContainer = BrandIndigoContainer
val SecondaryTeal = BrandCyanDark
val SecondaryTealContainer = BrandCyanContainer
val AccentGold = BrandAmber
val AccentGoldContainer = BrandAmberContainer
val SuccessGreen = BrandEmerald
val ErrorRed = BrandRoseDark
val ErrorRedContainer = BrandRoseContainer

// Modern Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
)
val SalesCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF6366F1))
)
val EmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF10B981))
)
val AmberGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
)
val RoseGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFBE123C), Color(0xFFF43F5E))
)
val CyanGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0E7490), Color(0xFF06B6D4))
)
val PurpleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6))
)

// Background & Neutral tokens
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFF1F5F9)

val DarkSurface = Color(0xFF0F172A)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkBackground = Color(0xFF090D16)

object AppThemeColors {
    // Vibrant success/emerald green for prices & in-stock text
    val successText: Color
        @androidx.compose.runtime.Composable get() = if (LocalIsDarkTheme.current) Color(0xFF34D399) else Color(0xFF047857)

    // Warning amber text
    val warningText: Color
        @androidx.compose.runtime.Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFBBF24) else Color(0xFFB45309)

    // Error rose/red text
    val errorText: Color
        @androidx.compose.runtime.Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFB7185) else Color(0xFFBE123C)

    // Alias for danger/alert text
    val dangerText: Color
        @androidx.compose.runtime.Composable get() = errorText

    // Info/indigo text
    val infoText: Color
        @androidx.compose.runtime.Composable get() = if (LocalIsDarkTheme.current) Color(0xFF818CF8) else Color(0xFF4F46E5)

    // Subtle background for badges/icons
    @androidx.compose.runtime.Composable
    fun subtleBackground(tint: Color): Color =
        if (LocalIsDarkTheme.current) tint.copy(alpha = 0.2f) else tint.copy(alpha = 0.12f)
}


