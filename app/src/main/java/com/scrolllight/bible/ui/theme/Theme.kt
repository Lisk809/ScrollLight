package com.scrolllight.bible.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Amber / Warm Gold palette ─────────────────────────────────────────────────
val AmberPrimary        = Color(0xFFB45309)
val AmberPrimaryLight   = Color(0xFFD97706)
val AmberOnPrimary      = Color(0xFFFFFFFF)
val AmberContainer      = Color(0xFFFEF3C7)
val AmberOnContainer    = Color(0xFF78350F)
val AmberSurface        = Color(0xFFFFFBF5)
val AmberSurfaceVariant = Color(0xFFF5F0E8)
val AmberOutline        = Color(0xFFD4C4A8)

private val LightColors = lightColorScheme(
    primary          = AmberPrimary,
    onPrimary        = AmberOnPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary        = Color(0xFF78716C),
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5F0E8),
    onSecondaryContainer = Color(0xFF44403C),
    surface          = AmberSurface,
    onSurface        = Color(0xFF1C1917),
    surfaceVariant   = AmberSurfaceVariant,
    onSurfaceVariant = Color(0xFF57534E),
    outline          = AmberOutline,
    background       = AmberSurface,
    onBackground     = Color(0xFF1C1917),
)

private val DarkColors = darkColorScheme(
    primary          = AmberPrimaryLight,
    onPrimary        = Color(0xFF451A03),
    primaryContainer = AmberOnContainer,
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary        = Color(0xFFD6D3D1),
    onSecondary      = Color(0xFF292524),
    secondaryContainer = Color(0xFF44403C),
    onSecondaryContainer = Color(0xFFD6D3D1),
    surface          = Color(0xFF1C1917),
    onSurface        = Color(0xFFE7E5E4),
    surfaceVariant   = Color(0xFF292524),
    onSurfaceVariant = Color(0xFFD6D3D1),
    background       = Color(0xFF1C1917),
    onBackground     = Color(0xFFE7E5E4),
)

val ScrollLightTypography = Typography(
    headlineLarge  = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 16.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)

@Composable
fun ScrollLightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = ScrollLightTypography,
        content     = content
    )
}
