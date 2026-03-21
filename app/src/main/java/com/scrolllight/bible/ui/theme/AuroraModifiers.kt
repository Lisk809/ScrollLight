package com.scrolllight.bible.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── Shape tokens ─────────────────────────────────────────────────────────────

object AuroraShapes {
    val CardLarge  = RoundedCornerShape(24.dp)
    val CardMedium = RoundedCornerShape(20.dp)
    val CardSmall  = RoundedCornerShape(16.dp)
    val Chip       = RoundedCornerShape(12.dp)
    val Button     = RoundedCornerShape(14.dp)
    val Full       = RoundedCornerShape(50)
}

// ─── Core Modifier extensions ─────────────────────────────────────────────────

/**
 * Glass morphism card surface.
 * Uses a semi-transparent gradient + top highlight border to simulate frosted glass.
 * The blur() on a decorative under-layer creates the frosted effect without
 * requiring RenderEffect (API 31+).
 */
fun Modifier.auroraGlass(
    shape: Shape = AuroraShapes.CardLarge,
    glassAlpha: Float = 0.78f,
    blurRadius: Dp = 22.dp,
    elevation: Dp = 8.dp,
    tint: Color = Color.White,
    spotColor: Color = Color.Black.copy(alpha = 0.04f),
    ambientColor: Color = Color.Black.copy(alpha = 0.03f),
): Modifier = this
    .shadow(
        elevation    = elevation,
        shape        = shape,
        spotColor    = spotColor,
        ambientColor = ambientColor,
    )
    .clip(shape)
    // Blurred matte under-layer — gives the frosted appearance
    .drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    tint.copy(alpha = glassAlpha),
                    tint.copy(alpha = glassAlpha * 0.85f),
                )
            )
        )
    }
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = glassAlpha),
                tint.copy(alpha = glassAlpha * 0.85f),
            )
        )
    )
    // Top highlight — the key detail that sells glass
    .border(
        width = 0.7.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.10f),
            )
        ),
        shape = shape,
    )

/** Dark-mode glass variant */
fun Modifier.auroraDarkGlass(
    shape: Shape = AuroraShapes.CardLarge,
    elevation: Dp = 6.dp,
): Modifier = this
    .shadow(elevation = elevation, shape = shape,
        spotColor = Color.Black.copy(0.3f), ambientColor = Color.Black.copy(0.2f))
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(0.10f), Color.White.copy(0.05f))
        )
    )
    .border(
        width = 0.5.dp,
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(0.20f), Color.White.copy(0.04f))
        ),
        shape = shape,
    )

/**
 * Draws a large radial glow behind the component.
 * Perfect for buttons, FABs, and section headers.
 */
fun Modifier.ambientGlow(
    color: Color = AuroraColors.GlowSage,
    radius: Dp = 120.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
): Modifier = this.drawBehind {
    val px = radius.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(size.width / 2 + offsetX.toPx(), size.height / 2 + offsetY.toPx()),
            radius = px,
        ),
        radius = px,
        center = Offset(size.width / 2 + offsetX.toPx(), size.height / 2 + offsetY.toPx()),
    )
}

/** Draws a subtle top-edge shimmer that evokes an overhead light source. */
fun Modifier.topShimmer(alpha: Float = 0.35f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.Transparent,
            ),
            endY = size.height * 0.35f,
        )
    )
}

// ─── Press animation ──────────────────────────────────────────────────────────

/**
 * Scale + shadow press feedback — gives the card a satisfying "sink" feeling.
 */
@Composable
fun Modifier.auroraClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication        = null,
            enabled           = enabled,
            onClick           = onClick,
        )
}

// ─── Background canvas helpers ────────────────────────────────────────────────

/**
 * Full-screen Aurora background: a static gradient + three decorative ambient orbs.
 * Place this as the outermost background in your Scaffold / root Box.
 */
fun Modifier.auroraBackground(isDark: Boolean = false): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = if (isDark) listOf(
                AuroraColors.DarkBg,
                Color(0xFF1E2220),
                Color(0xFF1C1E22),
            ) else listOf(
                AuroraColors.BgOat,
                Color(0xFFF3F5F2),
                AuroraColors.BgMistBlue,
            )
        )
    )
    .drawBehind {
        if (!isDark) {
            // Top-left sage orb
            drawOrb(
                color  = AuroraColors.GlowSage,
                cx     = size.width * 0.15f,
                cy     = size.height * 0.08f,
                radius = size.width * 0.55f,
            )
            // Bottom-right rose orb
            drawOrb(
                color  = AuroraColors.GlowRose,
                cx     = size.width * 0.88f,
                cy     = size.height * 0.82f,
                radius = size.width * 0.50f,
            )
            // Center-left blue orb
            drawOrb(
                color  = AuroraColors.GlowBlue,
                cx     = size.width * 0.05f,
                cy     = size.height * 0.52f,
                radius = size.width * 0.40f,
            )
        } else {
            drawOrb(
                color  = Color(0xFF3A5A4A).copy(0.18f),
                cx     = size.width * 0.15f,
                cy     = size.height * 0.1f,
                radius = size.width * 0.55f,
            )
            drawOrb(
                color  = Color(0xFF5A3A40).copy(0.12f),
                cx     = size.width * 0.85f,
                cy     = size.height * 0.85f,
                radius = size.width * 0.50f,
            )
        }
    }

private fun DrawScope.drawOrb(color: Color, cx: Float, cy: Float, radius: Float) {
    drawCircle(
        brush  = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(cx, cy),
            radius = radius,
        ),
        radius = radius,
        center = Offset(cx, cy),
    )
}
