package com.scrolllight.bible.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Morandi Palette ──────────────────────────────────────────────────────────

object AuroraColors {

    // ── Backgrounds ──────────────────────────────────────────────────────────
    val BgOat        = Color(0xFFF9F7F4)   // 燕麦色（默认）
    val BgGrayGreen  = Color(0xFFF0F2F1)   // 灰绿
    val BgMistBlue   = Color(0xFFEFF2F5)   // 雾霾蓝

    // ── Glass surfaces ────────────────────────────────────────────────────────
    val GlassWhite75 = Color(0xBFFFFFFF)   // 75% white
    val GlassWhite60 = Color(0x99FFFFFF)   // 60% white
    val GlassWhite40 = Color(0x66FFFFFF)   // 40% white
    val GlassBorder  = Color(0x99FFFFFF)   // glass border highlight

    // ── Morandi accents (low-saturation) ──────────────────────────────────────
    val SageGreen    = Color(0xFF8FA897)   // 鼠尾草绿
    val DustyRose    = Color(0xFFC4A49A)   // 干枯玫瑰粉
    val SlateBlue    = Color(0xFF9AA5B1)   // 石板蓝
    val WarmGray     = Color(0xFFB8B0A8)   // 暖灰
    val DustyMauve   = Color(0xFFB5A4B0)   // 藕荷色
    val TaupeBrown   = Color(0xFFA89880)   // 暖驼色
    val MistGreen    = Color(0xFFA8B5AD)   // 淡烟绿

    // ── Dark variants ─────────────────────────────────────────────────────────
    val SageGreenDark   = Color(0xFF637A6B)
    val DustyRoseDark   = Color(0xFF9A7068)
    val SlateContainer  = Color(0xFFDDE4EA)
    val SageContainer   = Color(0xFFDDE8E2)

    // ── Glow / Ambient (10-30% alpha) ────────────────────────────────────────
    val GlowSage      = Color(0xFF8FA897).copy(alpha = 0.22f)
    val GlowRose      = Color(0xFFC4A49A).copy(alpha = 0.18f)
    val GlowBlue      = Color(0xFF9AA5B1).copy(alpha = 0.20f)
    val GlowAmbient   = Color(0xFFE3E8E6).copy(alpha = 0.30f)
    val GlowWarm      = Color(0xFFF0EBE3).copy(alpha = 0.25f)
    val GlowMauve     = Color(0xFFE8E0E8).copy(alpha = 0.20f)

    // ── Text ─────────────────────────────────────────────────────────────────
    val TextPrimary   = Color(0xFF2C2C2C)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextTertiary  = Color(0xFF9E9E9E)
    val TextOnDark    = Color(0xFFF5F5F5)

    // ── Dark theme backgrounds ────────────────────────────────────────────────
    val DarkBg        = Color(0xFF1A1C1A)
    val DarkSurface   = Color(0xFF252927)
    val DarkGlass     = Color(0x33FFFFFF)
    val DarkBorder    = Color(0x22FFFFFF)
}

// ─── Light color scheme ───────────────────────────────────────────────────────

val AuroraLightColorScheme = lightColorScheme(
    primary              = AuroraColors.SageGreen,
    onPrimary            = Color.White,
    primaryContainer     = AuroraColors.SageContainer,
    onPrimaryContainer   = AuroraColors.SageGreenDark,

    secondary            = AuroraColors.DustyRose,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFF0E6E3),
    onSecondaryContainer = AuroraColors.DustyRoseDark,

    tertiary             = AuroraColors.SlateBlue,
    onTertiary           = Color.White,
    tertiaryContainer    = AuroraColors.SlateContainer,
    onTertiaryContainer  = Color(0xFF3A4A55),

    background           = AuroraColors.BgOat,
    onBackground         = AuroraColors.TextPrimary,

    surface              = AuroraColors.GlassWhite75,
    onSurface            = AuroraColors.TextPrimary,
    surfaceVariant       = Color(0xFFECEEEC),
    onSurfaceVariant     = AuroraColors.TextSecondary,

    outline              = Color(0xFFCACACA),
    outlineVariant       = Color(0xFFE0E0E0),

    error                = Color(0xFFB5736A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDDD9),
    onErrorContainer     = Color(0xFF7A3329),

    inverseSurface       = Color(0xFF2F312F),
    inverseOnSurface     = Color(0xFFF0F1EF),
    inversePrimary       = AuroraColors.MistGreen,

    scrim                = Color(0xFF000000),
    surfaceTint          = AuroraColors.SageGreen.copy(alpha = 0.08f),
)

val AuroraDarkColorScheme = darkColorScheme(
    primary              = AuroraColors.MistGreen,
    onPrimary            = Color(0xFF1E3328),
    primaryContainer     = AuroraColors.SageGreenDark,
    onPrimaryContainer   = Color(0xFFBCEBD3),

    secondary            = Color(0xFFE5B8B0),
    onSecondary          = Color(0xFF4A201A),
    secondaryContainer   = AuroraColors.DustyRoseDark,
    onSecondaryContainer = Color(0xFFFFDAD5),

    tertiary             = Color(0xFFB5C8D8),
    onTertiary           = Color(0xFF1A3040),
    tertiaryContainer    = Color(0xFF3A5060),
    onTertiaryContainer  = Color(0xFFD5E8F5),

    background           = AuroraColors.DarkBg,
    onBackground         = AuroraColors.TextOnDark,

    surface              = AuroraColors.DarkSurface,
    onSurface            = AuroraColors.TextOnDark,
    surfaceVariant       = Color(0xFF363A36),
    onSurfaceVariant     = Color(0xFFBEC5BE),

    outline              = Color(0xFF6E756E),
    outlineVariant       = Color(0xFF414941),

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)
