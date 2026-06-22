package com.example.aituner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = Typography(
    // Large screen titles
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    // Section headers
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    // Card titles / emphasized text
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp
    ),
    // Body text
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    // Captions / secondary info
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),
    // Labels / buttons
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentCyan,
    background = DeepBlack,
    surface = DarkSurface,
    surfaceVariant = CardDark,
    onPrimary = DeepBlack,
    onSecondary = DeepBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = DeepBlack
)

@Composable
fun AiTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
