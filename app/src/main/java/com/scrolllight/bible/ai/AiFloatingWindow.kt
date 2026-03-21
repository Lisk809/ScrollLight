package com.scrolllight.bible.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.scrolllight.bible.ui.theme.*
import kotlinx.coroutines.launch

// ── Host ──────────────────────────────────────────────────────────────────────

@Composable
fun AiFloatingWindowHost(
    readingContext: AiReadingContext = AiReadingContext("", "", 0),
    onNavigateToSettings: () -> Unit,
    vm: AiChatViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(readingContext) { vm.updateReadingContext(readingContext) }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Scrim
        AnimatedVisibility(
            visible = state.panelVisible,
            enter   = fadeIn(tween(200)),
            exit    = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { vm.hidePanel() }
            )
        }

        // Panel — slides up with spring
        AnimatedVisibility(
            visible  = state.panelVisible,
            enter    = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec  = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(140)),
            exit     = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AiChatPanel(
                state              = state,
                ctx                = readingContext,
                onClose            = { vm.hidePanel() },
                onSend             = { vm.send(it) },
                onInputChange      = { vm.setInputText(it) },
                onClear            = { vm.clearHistory() },
                onNavigateSettings = { vm.hidePanel(); onNavigateToSettings() }
            )
        }

        // Draggable bubble
        AnimatedVisibility(
            visible  = !state.panelVisible,
            enter    = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                       fadeIn(animationSpec = tween(150)),
            exit     = scaleOut(animationSpec = tween(120)) +
                       fadeOut(animationSpec = tween(100)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            DraggableAiBubble(
                isLoading  = state.isLoading,
                hasHistory = state.bubbles.isNotEmpty(),
                onClick    = { vm.showPanel() }
            )
        }
    }
}

// ── Draggable bubble ──────────────────────────────────────────────────────────

@Composable
private fun DraggableAiBubble(
    isLoading: Boolean,
    hasHistory: Boolean,
    onClick: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val animOffset  = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging  by remember { mutableStateOf(false) }
    var screenW     by remember { mutableFloatStateOf(1f) }
    val scope       = rememberCoroutineScope()
    val colors      = MaterialTheme.colorScheme

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue  = 0.22f,
        targetValue   = 0.60f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "glowA"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.14f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "bubbleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenW = it.size.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .offset {
                    IntOffset(
                        (animOffset.value.x + dragOffset.x).toInt(),
                        (animOffset.value.y + dragOffset.y).toInt()
                    )
                }
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart  = { isDragging = true },
                        onDragEnd    = {
                            isDragging = false
                            scope.launch {
                                val targetX = if (dragOffset.x < -screenW / 3.5f) -screenW + 230f else 0f
                                animOffset.animateTo(
                                    Offset(targetX, animOffset.value.y + dragOffset.y),
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessMediumLow
                                    )
                                )
                                dragOffset = Offset.Zero
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onDrag       = { _, d -> dragOffset += d }
                    )
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(colors.primary.copy(alpha = glowAlpha), Color.Transparent))
                    )
            )
            // Button body
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary.copy(alpha = 0.92f), colors.secondary.copy(alpha = 0.80f))
                        )
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        color       = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("✦", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Unread badge
            if (hasHistory && !isLoading) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF88))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

// ── Chat Panel ────────────────────────────────────────────────────────────────

@Composable
private fun AiChatPanel(
    state: AiChatUiState,
    ctx: AiReadingContext,
    onClose: () -> Unit,
    onSend: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val colors    = MaterialTheme.colorScheme
    val glass     = LocalGlassParams.current
    val isDark    = colors.background.luminance() < 0.15f
    val baseColor = if (isDark) colors.surfaceVariant else Color.White

    LaunchedEffect(state.bubbles.size) {
        if (state.bubbles.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.bubbles.size - 1) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.82f)
            .navigationBarsPadding()    // account for system nav bar
            .glassBackground(
                shape      = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                glassColor = baseColor,
                alpha      = (glass.cardAlpha + 0.12f).coerceAtMost(0.96f),
                borderAlpha = glass.borderAlpha,
                elevation  = 20.dp,
                spotColor  = colors.primary.copy(alpha = 0.06f)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(colors.primary.copy(alpha = 0.85f), colors.secondary.copy(alpha = 0.75f))
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(
                                text  = "AI 助读",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (ctx.bookName.isNotBlank()) {
                                Text(
                                    text  = "${ctx.bookName} ${ctx.chapter}章",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = state.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                        IconButton(onClick = onClear) {
                            Icon(Icons.Outlined.DeleteOutline, "清空", tint = Color.White.copy(alpha = 0.9f))
                        }
                        IconButton(onClick = onNavigateSettings) {
                            Icon(Icons.Outlined.Settings, "设置", tint = Color.White.copy(alpha = 0.9f))
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "收起", tint = Color.White)
                        }
                    }
                }
            }

            // ── Config warning ─────────────────────────────────────────────
            AnimatedVisibility(visible = !state.config.isConfigured) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .background(colors.errorContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Warning, null,
                        tint     = colors.error,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text     = "未配置 API Key",
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick         = onNavigateSettings,
                        contentPadding  = PaddingValues(all = 0.dp)
                    ) {
                        Text("去设置", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                }
            }

            // ── Messages ───────────────────────────────────────────────────
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.bubbles.isEmpty()) {
                    item {
                        EmptyState(ctx = ctx, onSuggestion = { onSend(it) })
                    }
                }
                items(count = state.bubbles.size) { idx ->
                    val bubble = state.bubbles[idx]
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(tween(200)) + slideInVertically { it / 2 }
                    ) {
                        when (bubble) {
                            is ChatBubble.User          -> UserBubble(bubble)
                            is ChatBubble.Assistant     -> AssistantBubble(bubble)
                            is ChatBubble.ToolExecution -> ToolCard(bubble)
                            is ChatBubble.Error         -> ErrorBubble(bubble)
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.outline.copy(alpha = 0.12f))

            // ── Input ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = state.inputText,
                    onValueChange = onInputChange,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            text  = if (ctx.bookName.isNotBlank())
                                "询问关于${ctx.bookName}的问题…" else "问我任何圣经问题…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape           = RoundedCornerShape(20.dp),
                    maxLines        = 4,
                    enabled         = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (state.inputText.isNotBlank() && !state.isLoading) onSend(state.inputText)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = colors.primary.copy(alpha = 0.45f),
                        unfocusedBorderColor    = colors.outline.copy(alpha = 0.3f),
                        focusedContainerColor   = if (isDark) colors.surfaceVariant.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.65f),
                        unfocusedContainerColor = if (isDark) colors.surfaceVariant.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.45f),
                    )
                )
                val canSend = state.inputText.isNotBlank() && !state.isLoading && state.config.isConfigured
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend)
                                Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.9f), colors.secondary.copy(alpha = 0.8f)))
                            else
                                Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceVariant))
                        )
                        .then(
                            if (canSend) Modifier.border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            else Modifier
                        )
                        .clickable(enabled = canSend) { onSend(state.inputText) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "发送",
                        tint     = if (canSend) Color.White else colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(ctx: AiReadingContext, onSuggestion: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier            = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✦", fontSize = 34.sp, color = colors.primary)
        Text(
            text  = "光言 AI 助读",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text      = "解释经文、高亮节次、查找交叉参考，\n直接操控阅读界面。",
            style     = MaterialTheme.typography.bodySmall,
            color     = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (ctx.bookName.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text  = "当前：${ctx.bookName} 第${ctx.chapter}章",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
            val suggestions = listOf("这章的主要主题是什么？", "帮我高亮关键经文", "有哪些交叉参考？", "解释本章历史背景")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { s ->
                            SuggestionChip(
                                onClick  = { onSuggestion(s) },
                                label    = {
                                    Text(s, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Bubbles ───────────────────────────────────────────────────────────────────


// ── Copy Button ───────────────────────────────────────────────────────────────

@Composable
private fun CopyButton(text: String, context: Context, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("AI回复", text))
                copied = true
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (copied) Icons.Outlined.CheckCircle else Icons.Outlined.ContentCopy,
            contentDescription = "复制",
            modifier = Modifier.size(12.dp),
            tint = if (copied) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text  = if (copied) "已复制" else "复制",
            style = MaterialTheme.typography.labelSmall,
            color = if (copied) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UserBubble(b: ChatBubble.User) {
    val colors  = MaterialTheme.colorScheme
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp), spotColor = colors.primary.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(
                    Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.88f), colors.secondary.copy(alpha = 0.78f)))
                )
                .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(b.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
        CopyButton(text = b.text, context = context)
    }
}

@Composable
private fun AssistantBubble(b: ChatBubble.Assistant) {
    val colors  = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment     = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.8f), colors.secondary.copy(alpha = 0.7f)))
                    )
                    .border(0.6.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .glassBackground(
                        shape      = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                        glassColor = if (isDark) colors.surfaceVariant else Color.White,
                        alpha      = if (isDark) 0.65f else 0.80f,
                        elevation  = 3.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (b.text.isBlank() && b.isStreaming) {
                        TypingIndicator()
                    } else {
                        Text(b.text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        if (b.isStreaming) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier   = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                                color      = colors.primary.copy(alpha = 0.6f),
                                trackColor = colors.primary.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
        // Copy button — only when text is available and not streaming
        if (b.text.isNotBlank() && !b.isStreaming) {
            CopyButton(text = b.text, context = context,
                modifier = Modifier.padding(start = 36.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val colors     = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue  = 0.22f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    tween(400, delayMillis = i * 130, easing = EaseInOutSine),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun ToolCard(b: ChatBubble.ToolExecution) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    val borderAlpha by if (b.isRunning) {
        rememberInfiniteTransition(label = "border").animateFloat(
            initialValue  = 0.35f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label         = "ba"
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(
                shape      = RoundedCornerShape(14.dp),
                glassColor = if (isDark) colors.surfaceVariant else Color.White,
                alpha      = 0.80f,
                elevation  = 4.dp
            )
            .border(
                width = 0.8.dp,
                color = (if (b.isError) colors.error else colors.primary).copy(alpha = borderAlpha),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (b.isRunning) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(14.dp),
                            color       = colors.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector        = if (b.isError) Icons.Outlined.Error else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier           = Modifier.size(14.dp),
                            tint               = if (b.isError) colors.error else Color(0xFF4CAF88)
                        )
                    }
                    Text(
                        text       = b.displayName,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector        = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp),
                    tint               = colors.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val prettyArgs = remember(b.params) {
                        try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(b.params)) }
                        catch (_: Exception) { b.params }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text  = prettyArgs,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = colors.onSurfaceVariant
                        )
                    }
                    b.result?.let { res ->
                        val prettyResult = remember(res) {
                            try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(res)) }
                            catch (_: Exception) { res }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (b.isError) colors.errorContainer.copy(alpha = 0.4f)
                                    else Color(0xFF4CAF88).copy(alpha = 0.08f)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text  = prettyResult,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (b.isError) colors.error else colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(b: ChatBubble.Error) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.errorContainer.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Error, null, modifier = Modifier.size(16.dp), tint = colors.error)
        Text(b.message, style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer)
    }
}

// ── Public AiChatPanel (used by ReadingScreen) ────────────────────────────────

@Composable
fun AiChatPanel(
    messages: List<AiMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    context: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.8f), colors.secondary.copy(alpha = 0.7f)))
                    ),
                contentAlignment = Alignment.Center
            ) { Text("✦", color = Color.White) }
            Column {
                Text("AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(context, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
        LazyColumn(
            modifier    = Modifier.weight(1f).padding(vertical = 8.dp),
            reverseLayout = true
        ) {
            items(count = messages.reversed().size) { i ->
                val msg    = messages.reversed()[i]
                val isUser = msg.role == AiMessage.Role.USER
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(
                                if (isUser) RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp)
                                else RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)
                            )
                            .background(if (isUser) colors.primary else colors.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text  = msg.content,
                            color = if (isUser) colors.onPrimary else colors.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = onInputChange,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("询问…") },
                shape         = RoundedCornerShape(16.dp),
                singleLine    = true
            )
            IconButton(onClick = onSend, enabled = input.isNotBlank()) {
                Icon(
                    Icons.Outlined.Send, "发送",
                    tint = if (input.isNotBlank()) colors.primary else colors.onSurfaceVariant
                )
            }
        }
    }
}

// ── LazyListScope helper ──────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    itemContent: @Composable (Int) -> Unit
) {
    repeat(count) { idx -> item { itemContent(idx) } }
}
