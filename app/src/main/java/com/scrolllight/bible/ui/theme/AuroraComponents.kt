package com.scrolllight.bible.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ══════════════════════════════════════════════════════════════════════════════
//  Modifier Extensions
// ══════════════════════════════════════════════════════════════════════════════

/**
 * 毛玻璃卡片背景：半透明白色 + 渐变 + 柔和边框 + 阴影
 */
fun Modifier.glassBackground(
    shape: Shape = RoundedCornerShape(24.dp),
    glassColor: Color = Color.White,
    alpha: Float = 0.72f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 8.dp,
    spotColor: Color = Color.Black.copy(alpha = 0.04f),
): Modifier = this
    .shadow(elevation = elevation, shape = shape, spotColor = spotColor, ambientColor = spotColor)
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to glassColor.copy(alpha = (alpha + 0.08f).coerceAtMost(1f)),
                1f to glassColor.copy(alpha = (alpha - 0.08f).coerceAtLeast(0f))
            )
        )
    )
    .border(
        width = 0.8.dp,
        brush = Brush.verticalGradient(
            listOf(
                glassColor.copy(alpha = borderAlpha + 0.15f),
                glassColor.copy(alpha = borderAlpha)
            )
        ),
        shape = shape
    )

/**
 * 光晕装饰 —— 在组件背后绘制径向渐变圆
 */
fun Modifier.ambientGlow(
    color: Color,
    radius: Float = 200f,
    alpha: Float = 0.18f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
): Modifier = this.drawBehind {
    drawGlowCircle(color.copy(alpha = alpha), radius, Offset(size.width * 0.5f + offsetX, size.height * 0.5f + offsetY))
}

private fun DrawScope.drawGlowCircle(color: Color, radius: Float, center: Offset) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Background  —  根背景：莫兰迪渐变 + 三颗光晕
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass  = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                // Glow 1 — top-left (sage)
                drawGlowCircle(
                    glass.glowColor1.copy(alpha = glass.glowAlpha),
                    size.width * 0.85f,
                    Offset(-size.width * 0.1f, -size.height * 0.05f)
                )
                // Glow 2 — bottom-right (rose)
                drawGlowCircle(
                    glass.glowColor2.copy(alpha = glass.glowAlpha * 0.8f),
                    size.width * 0.80f,
                    Offset(size.width * 1.05f, size.height * 1.1f)
                )
                // Glow 3 — center-right (blue/mist)
                drawGlowCircle(
                    glass.glowColor3.copy(alpha = glass.glowAlpha * 0.6f),
                    size.width * 0.60f,
                    Offset(size.width * 0.9f, size.height * 0.35f)
                )
            },
        content = content
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  AuroraCard
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass  = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f
    val baseColor = if (isDark) colors.surfaceVariant else Color.White

    var pressed by remember { mutableStateOf(false) }
    val elevationAnim by animateDpAsState(
        targetValue   = if (pressed) (glass.shadowElevation * 0.4f).dp else glass.shadowElevation.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "cardElevation"
    )
    val alphaAnim by animateFloatAsState(
        targetValue   = if (pressed) (glass.cardAlpha + 0.10f).coerceAtMost(0.95f) else glass.cardAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "cardAlpha"
    )

    val mod = modifier
        .then(
            if (glowColor != null)
                Modifier.drawBehind {
                    drawGlowCircle(glowColor.copy(alpha = glass.glowAlpha * 0.7f),
                        size.width * 0.65f, Offset(size.width * 0.5f, size.height * 0.5f))
                }
            else Modifier
        )
        .glassBackground(shape, baseColor, alphaAnim, glass.borderAlpha, elevationAnim, Color.Black.copy(0.04f))
        .then(
            if (onClick != null)
                Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onClick() }
            else Modifier
        )

    Column(modifier = mod, content = content)
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Surface — lightweight version for smaller items
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0f, // 0 = use theme default
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass  = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f
    val baseColor = if (isDark) colors.surfaceVariant else Color.White
    val effectiveAlpha = if (alpha == 0f) glass.cardAlpha * 0.8f else alpha

    Box(
        modifier = modifier
            .glassBackground(shape, baseColor, effectiveAlpha, glass.borderAlpha * 0.6f, (glass.shadowElevation * 0.5f).dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Section Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp).height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
            Text(
                title,
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Feature Button
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraFeatureButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val glass = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f

    Column(
        modifier = modifier
            .glassBackground(
                shape     = RoundedCornerShape(20.dp),
                glassColor = if (isDark) colors.surfaceVariant else Color.White,
                alpha     = glass.cardAlpha,
                elevation = (glass.shadowElevation * 0.6f).dp
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.radialGradient(listOf(tint.copy(alpha = 0.18f), tint.copy(alpha = 0.08f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurface,
            maxLines  = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Plan Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraPlanCard(
    title: String,
    days: Int,
    progress: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f

    Row(
        modifier = modifier
            .glassBackground(
                shape     = RoundedCornerShape(20.dp),
                glassColor = if (isDark) colors.surfaceVariant else Color.White,
                alpha     = glass.cardAlpha,
                elevation = glass.shadowElevation.dp
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accent strip
        Box(
            modifier = Modifier
                .width(4.dp).height(52.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(colors.primary, colors.secondary))
                )
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = colors.onSurface, maxLines = 2)
            Text("$days 天", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color    = colors.primary.copy(alpha = 0.7f),
                    trackColor = colors.primary.copy(alpha = 0.15f)
                )
            }
        }
        AuroraSurface(
            shape = RoundedCornerShape(12.dp),
            alpha = 0.0f,
            onClick = onClick,
            modifier = Modifier
        ) {
            Text(
                text = if (progress > 0f) "继续" else "开始",
                modifier = Modifier
                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                style  = MaterialTheme.typography.labelMedium,
                color  = colors.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Daily Verse Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraDailyVerseCard(
    theme: String,
    subTheme: String,
    text: String,
    reference: String,
    date: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val isDark  = colors.background.luminance() < 0.15f

    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = primary.copy(0.08f))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (isDark) primary.copy(0.30f) else primary.copy(0.12f),
                        if (isDark) colors.secondaryContainer.copy(0.25f) else colors.secondaryContainer.copy(0.40f),
                        if (isDark) colors.tertiaryContainer.copy(0.20f) else colors.tertiaryContainer.copy(0.30f),
                    )
                )
            )
            .border(0.8.dp, primary.copy(alpha = if (isDark) 0.2f else 0.15f), RoundedCornerShape(28.dp))
            .drawBehind {
                drawGlowCircle(primary.copy(0.10f), size.width * 0.6f,
                    Offset(size.width * -0.1f, size.height * -0.1f))
                drawGlowCircle(colors.secondary.copy(0.08f), size.width * 0.5f,
                    Offset(size.width * 1.05f, size.height * 1.05f))
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(theme, style = MaterialTheme.typography.labelMedium,
                            color = primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Text("·", color = colors.onSurfaceVariant)
                    Text(subTheme, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Text(date, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Text(
                text  = text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                lineHeight = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(primary.copy(0.6f)))
                Text("— $reference", style = MaterialTheme.typography.labelLarge,
                    color = primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Aurora Verse Row
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraVerseRow(
    verseNumber: Int,
    text: String,
    isHighlighted: Boolean,
    highlightColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = when {
            isSelected    -> MaterialTheme.colorScheme.primaryContainer.copy(0.50f)
            isHighlighted -> highlightColor.copy(0.40f)
            else          -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "verseRowBg"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "$verseNumber",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.primary.copy(0.75f),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.width(22.dp).paddingFromBaseline(top = 20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  AI Floating Button (Aurora style)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuroraAiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassParams.current
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(50), spotColor = colors.primary.copy(0.15f))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(listOf(colors.primary.copy(0.9f), colors.secondary.copy(0.8f)))
            )
            .border(0.8.dp, Color.White.copy(0.25f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = Color.White, fontSize = 15.dp.value.let {
                androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp)
            })
            Text("AI 助读", color = Color.White, style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
    }
}
