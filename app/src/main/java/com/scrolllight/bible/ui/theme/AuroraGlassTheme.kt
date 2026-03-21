package com.scrolllight.bible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
//  ThemeMode  —  系统跟随 / 手动亮色 / 手动暗色 / 温暖羊皮
// ══════════════════════════════════════════════════════════════════════════════

enum class ThemeMode(val label: String, val icon: String) {
    SYSTEM("跟随系统", "🌐"),
    LIGHT ("浅色",     "☀️"),
    DARK  ("深色",     "🌙"),
    SEPIA ("暖色羊皮", "📜")
}

// ══════════════════════════════════════════════════════════════════════════════
//  Morandi Palette  —  所有颜色在此处统一定义
// ══════════════════════════════════════════════════════════════════════════════

object MorandiPalette {
    // ── Light ──────────────────────────────────────────────────────────────
    val SageGreen          = Color(0xFF7A9E7E)
    val SageGreenLight     = Color(0xFF9EBD9E)
    val SageGreenContainer = Color(0xFFCAE0CB)
    val OnSageGreen        = Color(0xFFFFFFFF)
    val OnSageContainer    = Color(0xFF1A3D1E)

    val DustyRose          = Color(0xFFB08C88)
    val DustyRoseContainer = Color(0xFFEDD9D6)
    val OnDustyRose        = Color(0xFFFFFFFF)
    val OnDustyContainer   = Color(0xFF3B1916)

    val MistBlue           = Color(0xFF8FA3B1)
    val MistBlueContainer  = Color(0xFFD4E4EF)

    // Background surfaces
    val OatWhite           = Color(0xFFF9F7F4)
    val GraySage           = Color(0xFFF0F2F1)
    val LightSage          = Color(0xFFE8ECE9)
    val SoftSage           = Color(0xFFDDE3DE)

    val TextPrimary        = Color(0xFF2C2C2C)
    val TextSecondary      = Color(0xFF5A6360)
    val Outline            = Color(0xFFC4C9C6)
    val OutlineVariant     = Color(0xFFDDE3DE)

    // Glow tints (low saturation, low alpha — used as-is with .copy(alpha))
    val GlowSage           = Color(0xFF9BBAA4)   // use .copy(alpha = 0.18f)
    val GlowRose           = Color(0xFFC4A09A)   // use .copy(alpha = 0.14f)
    val GlowBlue           = Color(0xFF8FA3B1)   // use .copy(alpha = 0.12f)

    // ── Dark ───────────────────────────────────────────────────────────────
    val DarkBackground     = Color(0xFF181C1B)
    val DarkSurface        = Color(0xFF1E2422)
    val DarkSurfaceVar     = Color(0xFF2A302E)
    val DarkSurfaceHigh    = Color(0xFF313836)
    val DarkPrimary        = Color(0xFF9EBD9E)
    val DarkPrimaryContainer = Color(0xFF3D5C40)
    val DarkSecondary      = Color(0xFFC4A89E)
    val DarkSecContainer   = Color(0xFF5B3D3A)
    val DarkOnSurface      = Color(0xFFE2E8E4)
    val DarkOnSurfaceVar   = Color(0xFFA8B3AF)
    val DarkOutline        = Color(0xFF4A514E)

    // ── Sepia ──────────────────────────────────────────────────────────────
    val SepiaBackground    = Color(0xFFF5EFE6)
    val SepiaSurface       = Color(0xFFFAF6F0)
    val SepiaPrimary       = Color(0xFF8B7355)
    val SepiaPrimaryC      = Color(0xFFDED0BF)
    val SepiaSecondary     = Color(0xFFA89070)
    val SepiaSecondaryC    = Color(0xFFEDE0CF)
    val SepiaOutline       = Color(0xFFCFC3B2)
    val SepiaText          = Color(0xFF3A2E22)
    val SepiaTextSec       = Color(0xFF7A6A58)
}

// ══════════════════════════════════════════════════════════════════════════════
//  Color Schemes
// ══════════════════════════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary              = MorandiPalette.SageGreen,
    onPrimary            = MorandiPalette.OnSageGreen,
    primaryContainer     = MorandiPalette.SageGreenContainer,
    onPrimaryContainer   = MorandiPalette.OnSageContainer,
    secondary            = MorandiPalette.DustyRose,
    onSecondary          = MorandiPalette.OnDustyRose,
    secondaryContainer   = MorandiPalette.DustyRoseContainer,
    onSecondaryContainer = MorandiPalette.OnDustyContainer,
    tertiary             = MorandiPalette.MistBlue,
    tertiaryContainer    = MorandiPalette.MistBlueContainer,
    background           = MorandiPalette.GraySage,
    onBackground         = MorandiPalette.TextPrimary,
    surface              = MorandiPalette.OatWhite,
    onSurface            = MorandiPalette.TextPrimary,
    surfaceVariant       = MorandiPalette.LightSage,
    onSurfaceVariant     = MorandiPalette.TextSecondary,
    outline              = MorandiPalette.Outline,
    outlineVariant       = MorandiPalette.OutlineVariant,
    inverseSurface       = Color(0xFF2C312F),
    inverseOnSurface     = MorandiPalette.GraySage,
    inversePrimary       = MorandiPalette.SageGreenLight,
)

private val DarkColorScheme = darkColorScheme(
    primary              = MorandiPalette.DarkPrimary,
    onPrimary            = Color(0xFF1A3D1E),
    primaryContainer     = MorandiPalette.DarkPrimaryContainer,
    onPrimaryContainer   = MorandiPalette.SageGreenContainer,
    secondary            = MorandiPalette.DarkSecondary,
    onSecondary          = Color(0xFF3B1916),
    secondaryContainer   = MorandiPalette.DarkSecContainer,
    onSecondaryContainer = MorandiPalette.DustyRoseContainer,
    tertiary             = Color(0xFFABC3D1),
    background           = MorandiPalette.DarkBackground,
    onBackground         = MorandiPalette.DarkOnSurface,
    surface              = MorandiPalette.DarkSurface,
    onSurface            = MorandiPalette.DarkOnSurface,
    surfaceVariant       = MorandiPalette.DarkSurfaceVar,
    onSurfaceVariant     = MorandiPalette.DarkOnSurfaceVar,
    outline              = MorandiPalette.DarkOutline,
    outlineVariant       = Color(0xFF353C39),
)

private val SepiaColorScheme = lightColorScheme(
    primary              = MorandiPalette.SepiaPrimary,
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = MorandiPalette.SepiaPrimaryC,
    onPrimaryContainer   = Color(0xFF3A2E22),
    secondary            = MorandiPalette.SepiaSecondary,
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = MorandiPalette.SepiaSecondaryC,
    onSecondaryContainer = Color(0xFF3A2E22),
    background           = MorandiPalette.SepiaBackground,
    onBackground         = MorandiPalette.SepiaText,
    surface              = MorandiPalette.SepiaSurface,
    onSurface            = MorandiPalette.SepiaText,
    surfaceVariant       = Color(0xFFEDE4D8),
    onSurfaceVariant     = MorandiPalette.SepiaTextSec,
    outline              = MorandiPalette.SepiaOutline,
    outlineVariant       = Color(0xFFE5D9CA),
)

// ══════════════════════════════════════════════════════════════════════════════
//  Glass Parameters  —  毛玻璃参数，主题内注入
// ══════════════════════════════════════════════════════════════════════════════

data class GlassParams(
    val cardAlpha:      Float,   // 卡片背景透明度
    val borderAlpha:    Float,   // 卡片边框透明度
    val glowAlpha:      Float,   // 光晕强度
    val shadowElevation: Float,  // 卡片阴影强度 (dp)
    val glowColor1:     Color,
    val glowColor2:     Color,
    val glowColor3:     Color,
)

val LocalGlassParams = staticCompositionLocalOf {
    GlassParams(
        cardAlpha       = 0.72f,
        borderAlpha     = 0.35f,
        glowAlpha       = 0.18f,
        shadowElevation = 8f,
        glowColor1      = MorandiPalette.GlowSage,
        glowColor2      = MorandiPalette.GlowRose,
        glowColor3      = MorandiPalette.GlowBlue,
    )
}

val lightGlass = GlassParams(
    cardAlpha       = 0.72f,
    borderAlpha     = 0.35f,
    glowAlpha       = 0.18f,
    shadowElevation = 6f,
    glowColor1      = MorandiPalette.GlowSage,
    glowColor2      = MorandiPalette.GlowRose,
    glowColor3      = MorandiPalette.GlowBlue,
)

val darkGlass = GlassParams(
    cardAlpha       = 0.60f,
    borderAlpha     = 0.20f,
    glowAlpha       = 0.12f,
    shadowElevation = 4f,
    glowColor1      = Color(0xFF4A6B4E),
    glowColor2      = Color(0xFF5C3D3A),
    glowColor3      = Color(0xFF3D5060),
)

val sepiaGlass = GlassParams(
    cardAlpha       = 0.78f,
    borderAlpha     = 0.30f,
    glowAlpha       = 0.15f,
    shadowElevation = 5f,
    glowColor1      = Color(0xFFC4A882),
    glowColor2      = Color(0xFFD4B896),
    glowColor3      = Color(0xFFB8A090),
)

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Glass Theme  —  主入口
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ScrollLightTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT, ThemeMode.SEPIA -> false
    }
    val colorScheme = when (themeMode) {
        ThemeMode.SEPIA  -> SepiaColorScheme
        ThemeMode.DARK   -> DarkColorScheme
        ThemeMode.LIGHT  -> LightColorScheme
        ThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }
    val glass = when {
        themeMode == ThemeMode.SEPIA             -> sepiaGlass
        themeMode == ThemeMode.DARK              -> darkGlass
        themeMode == ThemeMode.SYSTEM && systemDark -> darkGlass
        else                                     -> lightGlass
    }

    CompositionLocalProvider(LocalGlassParams provides glass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = ScrollLightTypography,
            content     = content
        )
    }
}
