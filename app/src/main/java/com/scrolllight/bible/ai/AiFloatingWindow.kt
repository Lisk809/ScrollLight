package com.scrolllight.bible.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

// ── Entry point: wrap your root Scaffold with this ────────────────────────────

@Composable
fun AiFloatingWindowHost(
    readingContext: AiReadingContext = AiReadingContext("", "", 0),
    onNavigateToSettings: () -> Unit,
    vm: AiChatViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val state by vm.state.collectAsState()

    // Sync reading context
    LaunchedEffect(readingContext) { vm.updateReadingContext(readingContext) }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // Dimmed scrim when panel is open
        AnimatedVisibility(
            visible = state.panelVisible,
            enter   = fadeIn(tween(200)),
            exit    = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        vm.hidePanel()
                    }
            )
        }

        // Chat panel (slides up from bottom)
        AnimatedVisibility(
            visible = state.panelVisible,
            enter   = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(180)),
            exit    = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AiChatPanel(
                state              = state,
                readingContext     = readingContext,
                onClose            = { vm.hidePanel() },
                onSend             = { vm.send(it) },
                onInputChange      = { vm.setInputText(it) },
                onClear            = { vm.clearHistory() },
                onNavigateSettings = { vm.hidePanel(); onNavigateToSettings() }
            )
        }

        // Floating bubble (only when panel is hidden)
        AnimatedVisibility(
            visible = !state.panelVisible,
            enter   = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit    = scaleOut(tween(150)) + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            DraggableAiBubble(
                isLoading = state.isLoading,
                hasHistory = state.bubbles.isNotEmpty(),
                onClick   = { vm.showPanel() }
            )
        }
    }
}

// ── Draggable floating bubble ─────────────────────────────────────────────────

@Composable
private fun DraggableAiBubble(
    isLoading: Boolean,
    hasHistory: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var screenSize by remember { mutableStateOf(Offset(0f, 0f)) }
    val animOffset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Pulsing glow animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "bubbleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                screenSize = Offset(coords.size.width.toFloat(), coords.size.height.toFloat())
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .offset {
                    IntOffset(
                        (animOffset.value.x + offset.x).toInt(),
                        (animOffset.value.y + offset.y).toInt()
                    )
                }
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart  = { isDragging = true },
                        onDragEnd    = {
                            isDragging = false
                            // Snap to nearest vertical edge with spring
                            scope.launch {
                                val targetX = if (offset.x < -screenSize.x / 4f) -screenSize.x + 220f else 0f
                                animOffset.animateTo(
                                    Offset(targetX, animOffset.value.y + offset.y),
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                )
                                offset = Offset(0f, 0f)
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onDrag       = { _, drag -> offset += drag }
                    )
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFB45309).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Bubble body
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color(0x40B45309))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF92400E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("✦", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Badge dot when has history
            if (hasHistory && !isLoading) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

// ── Chat panel ────────────────────────────────────────────────────────────────

@Composable
private fun AiChatPanel(
    state: AiChatUiState,
    readingContext: AiReadingContext,
    onClose: () -> Unit,
    onSend: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to latest message
    LaunchedEffect(state.bubbles.size) {
        if (state.bubbles.isNotEmpty()) {
            listState.animateScrollToItem(state.bubbles.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.70f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB45309), Color(0xFFD97706))
                        ),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // AI icon
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("AI 助读", color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium)
                            if (readingContext.bookName.isNotBlank()) {
                                Text(
                                    "${readingContext.bookName} ${readingContext.chapter}章",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Row {
                        // Loading indicator
                        AnimatedVisibility(state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White, strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp).padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = onClear) {
                            Icon(Icons.Outlined.DeleteOutline, "清空", tint = Color.White.copy(alpha = 0.85f))
                        }
                        IconButton(onClick = onNavigateSettings) {
                            Icon(Icons.Outlined.Settings, "设置", tint = Color.White.copy(alpha = 0.85f))
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "关闭", tint = Color.White)
                        }
                    }
                }
            }

            // ── Not configured warning ────────────────────────────────────
            AnimatedVisibility(!state.config.isConfigured) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                        Text("尚未配置 API Key，", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onNavigateSettings, contentPadding = PaddingValues(0.dp)) {
                            Text("点此设置", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── Message list ──────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Empty state
                if (state.bubbles.isEmpty()) {
                    item { EmptyState(readingContext) }
                }

                items(state.bubbles, key = { it.id }) { bubble ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)) + slideInVertically { it / 2 }
                    ) {
                        when (bubble) {
                            is ChatBubble.User       -> UserBubble(bubble)
                            is ChatBubble.Assistant  -> AssistantBubble(bubble)
                            is ChatBubble.ToolExecution -> ToolExecutionCard(bubble)
                            is ChatBubble.Error      -> ErrorBubble(bubble)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

            // ── Input ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = state.inputText,
                    onValueChange = onInputChange,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            if (readingContext.bookName.isNotBlank()) "询问关于${readingContext.bookName}的问题…"
                            else "问我任何关于圣经的问题…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (state.inputText.isNotBlank() && !state.isLoading) onSend(state.inputText)
                    }),
                    enabled = !state.isLoading
                )

                // Send button
                val canSend = state.inputText.isNotBlank() && !state.isLoading && state.config.isConfigured
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                            else Brush.linearGradient(listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            ))
                        )
                        .clickable(enabled = canSend) { onSend(state.inputText) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Send, "发送",
                        tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(ctx: AiReadingContext) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✦", fontSize = 40.sp, color = MaterialTheme.colorScheme.primary)
        Text("光言 AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "我可以解释经文、高亮关键节次、提供交叉参考，并直接操控阅读界面帮助你理解圣经。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (ctx.bookName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("当前阅读：${ctx.bookName} 第${ctx.chapter}章", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            // Suggestion chips
            val suggestions = listOf(
                "这章的主要主题是什么？",
                "帮我高亮关键经文",
                "有哪些交叉参考？",
                "解释一下背景历史"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { s ->
                            SuggestionChip(
                                onClick  = { /* handled by parent via callback */ },
                                label    = { Text(s, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Bubble composables ────────────────────────────────────────────────────────

@Composable
private fun UserBubble(bubble: ChatBubble.User) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(bubble.text, modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssistantBubble(bubble: ChatBubble.Assistant) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))),
            contentAlignment = Alignment.Center
        ) { Text("✦", color = Color.White, fontSize = 12.sp) }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (bubble.text.isBlank() && bubble.isStreaming) {
                    // Typing indicator
                    TypingIndicator()
                } else {
                    Text(bubble.text, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (bubble.isStreaming) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color    = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val dots = 3
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(dots) { idx ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(400, delayMillis = idx * 130, easing = EaseInOutSine),
                    RepeatMode.Reverse
                ),
                label = "dot$idx"
            )
            Box(
                modifier = Modifier.size(7.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun ToolExecutionCard(bubble: ChatBubble.ToolExecution) {
    // Animated border when running
    val borderAlpha by if (bubble.isRunning) {
        rememberInfiniteTransition(label = "border").animateFloat(
            0.4f, 1f,
            infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "borderAlpha"
        )
    } else { remember { mutableFloatStateOf(0.3f) } }

    val prettyArgs = remember(bubble.params) {
        try {
            val json = JsonParser.parseString(bubble.params).asJsonObject
            GsonBuilder().setPrettyPrinting().create().toJson(json)
        } catch (_: Exception) { bubble.params }
    }

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(
                1.dp,
                if (bubble.isError) MaterialTheme.colorScheme.error.copy(borderAlpha)
                else MaterialTheme.colorScheme.primary.copy(borderAlpha),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (bubble.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            if (bubble.isError) Icons.Outlined.Error else Icons.Outlined.CheckCircle,
                            null, modifier = Modifier.size(14.dp),
                            tint = if (bubble.isError) MaterialTheme.colorScheme.error else Color(0xFF22C55E)
                        )
                    }
                    Text(bubble.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Args
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                    ) {
                        Text(
                            prettyArgs,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Result
                    bubble.result?.let { res ->
                        val prettyResult = remember(res) {
                            try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(res)) }
                            catch (_: Exception) { res }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (bubble.isError) MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                                    else Color(0xFF22C55E).copy(alpha = 0.08f)
                        ) {
                            Text(
                                prettyResult,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (bubble.isError) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(bubble: ChatBubble.Error) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Text(bubble.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
