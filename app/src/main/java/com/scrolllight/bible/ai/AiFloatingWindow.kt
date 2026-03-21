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
import kotlinx.coroutines.launch

// ── Entry point ───────────────────────────────────────────────────────────────

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

        // Dim scrim
        AnimatedVisibility(
            visible = state.panelVisible,
            enter = fadeIn(tween(200)),
            exit  = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { vm.hidePanel() }
            )
        }

        // Chat panel — bottom sheet style
        AnimatedVisibility(
            visible  = state.panelVisible,
            enter    = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(150)),
            exit     = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(tween(120)),
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

        // Draggable bubble
        AnimatedVisibility(
            visible  = !state.panelVisible,
            enter    = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150)),
            exit     = scaleOut(tween(120)) + fadeOut(tween(100)),
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

    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowA"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.15f else 1f,
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
                        onDragStart = { isDragging = true },
                        onDragEnd   = {
                            isDragging = false
                            scope.launch {
                                // snap to nearest edge
                                val targetX = if (dragOffset.x < -screenW / 3.5f) -screenW + 230f else 0f
                                animOffset.animateTo(
                                    Offset(targetX, animOffset.value.y + dragOffset.y),
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
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
            // glow ring
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFB45309).copy(alpha = glow), Color.Transparent)))
            )
            // main button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color(0x44B45309))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF92400E)))),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text("✦", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            // unread badge
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

@OptIn(ExperimentalLayoutApi::class)
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
    val scope     = rememberCoroutineScope()

    // Auto-scroll to bottom when new bubbles arrive
    LaunchedEffect(state.bubbles.size) {
        if (state.bubbles.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.bubbles.size - 1) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // Use windowInsetsBottomHeight so panel sits above nav bar
            .fillMaxHeight(fraction = 0.78f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFB45309), Color(0xFFD97706))),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Drag handle hint + title
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text("✦", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        Column {
                            Text("AI 助读", color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium)
                            if (readingContext.bookName.isNotBlank()) {
                                Text("${readingContext.bookName} ${readingContext.chapter}章",
                                    color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(state.isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onClear) {
                            Icon(Icons.Outlined.DeleteOutline, "清空对话", tint = Color.White.copy(0.9f))
                        }
                        IconButton(onClick = onNavigateSettings) {
                            Icon(Icons.Outlined.Settings, "AI设置", tint = Color.White.copy(0.9f))
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "收起", tint = Color.White)
                        }
                    }
                }
            }

            // ── Config warning ─────────────────────────────────────────────
            AnimatedVisibility(visible = !state.config.isConfigured) {
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                        Text("未配置 API Key", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onNavigateSettings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("去设置", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            // ── Messages ───────────────────────────────────────────────────
            LazyColumn(
                state   = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.bubbles.isEmpty()) {
                    item {
                        EmptyState(
                            readingContext = readingContext,
                            onSuggestion   = { onSend(it) }
                        )
                    }
                }
                items(state.bubbles.size) { idx ->
                    val bubble = state.bubbles[idx]
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)) + slideInVertically { it / 2 }
                    ) {
                        when (bubble) {
                            is ChatBubble.User        -> UserBubble(bubble)
                            is ChatBubble.Assistant   -> AssistantBubble(bubble)
                            is ChatBubble.ToolExecution -> ToolExecutionCard(bubble)
                            is ChatBubble.Error       -> ErrorBubble(bubble)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

            // ── Input row ─────────────────────────────────────────────────
            // No navigationBarsPadding() here — Scaffold already handles it via paddingValues
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()                               // push up when keyboard appears
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
                            if (readingContext.bookName.isNotBlank())
                                "询问关于${readingContext.bookName}的问题…"
                            else "问我任何圣经相关的问题…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape   = RoundedCornerShape(20.dp),
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
                        tint     = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Empty state with clickable suggestion chips ───────────────────────────────

@Composable
private fun EmptyState(
    readingContext: AiReadingContext,
    onSuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✦", fontSize = 36.sp, color = MaterialTheme.colorScheme.primary)
        Text("光言 AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "我可以解释经文、高亮关键节次、查找交叉参考，\n并直接操控阅读界面帮助你理解圣经。",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (readingContext.bookName.isNotBlank()) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "当前：${readingContext.bookName} 第${readingContext.chapter}章",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.primary
                )
            }

            val suggestions = listOf(
                "这章的主要主题是什么？",
                "帮我高亮关键经文",
                "有哪些交叉参考经文？",
                "解释一下本章的历史背景"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { suggestion ->
                            SuggestionChip(
                                onClick  = { onSuggestion(suggestion) },   // ← 真正触发发送
                                label    = {
                                    Text(
                                        suggestion,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center
                                    )
                                },
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
            Text(bubble.text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssistantBubble(bubble: ChatBubble.Assistant) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
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
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (bubble.text.isBlank() && bubble.isStreaming) {
                    TypingIndicator()
                } else {
                    Text(bubble.text, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (bubble.isStreaming) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier   = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color      = MaterialTheme.colorScheme.primary,
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
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { idx ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(400, delayMillis = idx * 130, easing = EaseInOutSine), RepeatMode.Reverse
                ), label = "dot$idx"
            )
            Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun ToolExecutionCard(bubble: ChatBubble.ToolExecution) {
    val borderAlpha by if (bubble.isRunning) {
        rememberInfiniteTransition(label = "border").animateFloat(
            0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "borderA"
        )
    } else remember { mutableFloatStateOf(0.3f) }

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (bubble.isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary)
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
                    val prettyArgs = remember(bubble.params) {
                        try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(bubble.params)) }
                        catch (_: Exception) { bubble.params }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.6f)) {
                        Text(prettyArgs, modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    bubble.result?.let { res ->
                        val prettyResult = remember(res) {
                            try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(res)) }
                            catch (_: Exception) { res }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (bubble.isError) MaterialTheme.colorScheme.errorContainer.copy(0.4f)
                                    else Color(0xFF22C55E).copy(alpha = 0.08f)
                        ) {
                            Text(prettyResult, modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (bubble.isError) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface)
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
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Text(bubble.message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
