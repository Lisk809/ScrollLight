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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
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

    Box(Modifier.fillMaxSize()) {
        content()

        // Scrim
        AnimatedVisibility(state.panelVisible, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(0.38f))
                    .clickable(indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { vm.hidePanel() }
            )
        }

        // Panel
        AnimatedVisibility(
            visible  = state.panelVisible,
            enter    = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(140)),
            exit     = slideOutVertically({ it }, spring(stiffness = Spring.StiffnessMedium)) + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AiChatPanel(state, readingContext,
                onClose            = { vm.hidePanel() },
                onSend             = { vm.send(it) },
                onInputChange      = { vm.setInputText(it) },
                onClear            = { vm.clearHistory() },
                onNavigateSettings = { vm.hidePanel(); onNavigateToSettings() })
        }

        // Bubble
        AnimatedVisibility(
            visible  = !state.panelVisible,
            enter    = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150)),
            exit     = scaleOut(tween(120)) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            DraggableAiBubble(state.isLoading, state.bubbles.isNotEmpty()) { vm.showPanel() }
        }
    }
}

// ── Bubble ────────────────────────────────────────────────────────────────────

@Composable
private fun DraggableAiBubble(isLoading: Boolean, hasHistory: Boolean, onClick: () -> Unit) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val animOffset  = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging  by remember { mutableStateOf(false) }
    var screenW     by remember { mutableFloatStateOf(1f) }
    val scope       = rememberCoroutineScope()
    val colors      = MaterialTheme.colorScheme
    val glass       = LocalGlassParams.current

    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.22f, 0.60f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse), "glowA"
    )
    val scale by animateFloatAsState(
        if (isDragging) 1.14f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "bs"
    )

    Box(Modifier.fillMaxSize().onGloballyPositioned { screenW = it.size.width.toFloat() }) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .offset {
                    IntOffset((animOffset.value.x + dragOffset.x).toInt(),
                        (animOffset.value.y + dragOffset.y).toInt())
                }
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart  = { isDragging = true },
                        onDragEnd    = {
                            isDragging = false
                            scope.launch {
                                val tx = if (dragOffset.x < -screenW / 3.5f) -screenW + 230f else 0f
                                animOffset.animateTo(
                                    Offset(tx, animOffset.value.y + dragOffset.y),
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
            // Glow ring
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(colors.primary.copy(glow), Color.Transparent)))
            )
            // Button body
            Box(
                Modifier.size(56.dp)
                    .shadow(10.dp, CircleShape, spotColor = colors.primary.copy(0.25f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary.copy(0.92f), colors.secondary.copy(0.80f))))
                    .border(0.8.dp, Color.White.copy(0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator(Color.White, Modifier.size(22.dp), strokeWidth = 2.5.dp)
                else Text("✦", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            // Badge
            if (hasHistory && !isLoading) {
                Box(
                    Modifier.size(13.dp).align(Alignment.TopEnd).offset((-8).dp, 8.dp)
                        .clip(CircleShape).background(Color(0xFF4CAF88)).border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

// ── Panel ─────────────────────────────────────────────────────────────────────

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
        if (state.bubbles.isNotEmpty()) scope.launch { listState.animateScrollToItem(state.bubbles.size - 1) }
    }

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f)
            .glassBackground(
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                baseColor, (glass.cardAlpha + 0.12f).coerceAtMost(0.96f), glass.borderAlpha,
                elevation = 20.dp, spotColor = colors.primary.copy(0.06f)
            )
    ) {
        Column(Modifier.fillMaxSize()) {

            // Header
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(colors.primary.copy(0.85f), colors.secondary.copy(0.75f))),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.18f))
                                .border(0.8.dp, Color.White.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("✦", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        Column {
                            Text("AI 助读", color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium)
                            if (ctx.bookName.isNotBlank())
                                Text("${ctx.bookName} ${ctx.chapter}章", color = Color.White.copy(0.75f),
                                    style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(state.isLoading) {
                            CircularProgressIndicator(Color.White, Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteOutline, "清空", tint = Color.White.copy(0.9f)) }
                        IconButton(onClick = onNavigateSettings) { Icon(Icons.Outlined.Settings, "设置", tint = Color.White.copy(0.9f)) }
                        IconButton(onClick = onClose) { Icon(Icons.Outlined.KeyboardArrowDown, "收起", tint = Color.White) }
                    }
                }
            }

            // Config warning
            AnimatedVisibility(!state.config.isConfigured) {
                Row(
                    Modifier.fillMaxWidth().background(colors.errorContainer.copy(0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    Arrangement.spacedBy(8.dp), Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Warning, null, tint = colors.error, modifier = Modifier.size(15.dp))
                    Text("未配置 API Key", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onNavigateSettings, contentPadding = PaddingValues(0.dp)) {
                        Text("去设置", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                }
            }

            // Messages
            LazyColumn(
                state   = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.bubbles.isEmpty()) {
                    item { EmptyState(ctx) { onSend(it) } }
                }
                items(state.bubbles.size) { idx ->
                    val bubble = state.bubbles[idx]
                    AnimatedVisibility(true, enter = fadeIn(tween(200)) + slideInVertically { it / 2 }) {
                        when (bubble) {
                            is ChatBubble.User         -> UserBubble(bubble)
                            is ChatBubble.Assistant    -> AssistantBubble(bubble)
                            is ChatBubble.ToolExecution -> ToolCard(bubble)
                            is ChatBubble.Error        -> ErrorBubble(bubble)
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.outline.copy(0.12f))

            // Input
            Row(
                Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
                Arrangement.spacedBy(8.dp), Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.inputText, onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(if (ctx.bookName.isNotBlank()) "询问关于${ctx.bookName}的问题…" else "问我任何圣经问题…",
                            style = MaterialTheme.typography.bodyMedium)
                    },
                    shape = RoundedCornerShape(20.dp), maxLines = 4, enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (state.inputText.isNotBlank() && !state.isLoading) onSend(state.inputText)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colors.primary.copy(0.45f),
                        unfocusedBorderColor = colors.outline.copy(0.3f),
                        focusedContainerColor   = if (isDark) colors.surfaceVariant.copy(0.5f) else Color.White.copy(0.65f),
                        unfocusedContainerColor = if (isDark) colors.surfaceVariant.copy(0.3f) else Color.White.copy(0.45f),
                    )
                )
                val canSend = state.inputText.isNotBlank() && !state.isLoading && state.config.isConfigured
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(if (canSend) Brush.linearGradient(listOf(colors.primary.copy(0.9f), colors.secondary.copy(0.8f)))
                                    else Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceVariant)))
                        .then(if (canSend) Modifier.border(0.8.dp, Color.White.copy(0.22f), CircleShape) else Modifier)
                        .clickable(enabled = canSend) { onSend(state.inputText) },
                    Alignment.Center
                ) {
                    Icon(Icons.Outlined.Send, "发送", Modifier.size(20.dp),
                        if (canSend) Color.White else colors.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(ctx: AiReadingContext, onSuggestion: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(20.dp), Alignment.CenterHorizontally, Arrangement.spacedBy(12.dp)) {
        Text("✦", fontSize = 34.sp, color = colors.primary)
        Text("光言 AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("解释经文、高亮节次、查找交叉参考，\n直接操控阅读界面。",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (ctx.bookName.isNotBlank()) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(colors.primary.copy(0.10f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("当前：${ctx.bookName} 第${ctx.chapter}章",
                    style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("这章的主要主题是什么？", "帮我高亮关键经文", "有哪些交叉参考？", "解释本章历史背景").chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                        row.forEach { s ->
                            SuggestionChip(onClick = { onSuggestion(s) },
                                label = { Text(s, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) },
                                modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Bubbles ───────────────────────────────────────────────────────────────────

@Composable
private fun UserBubble(b: ChatBubble.User) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), Arrangement.End) {
        Box(
            Modifier.widthIn(max = 280.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp), spotColor = colors.primary.copy(0.1f))
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(Brush.linearGradient(listOf(colors.primary.copy(0.88f), colors.secondary.copy(0.78f))))
                .border(0.6.dp, Color.White.copy(0.2f), RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text(b.text, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun AssistantBubble(b: ChatBubble.Assistant) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    Row(Modifier.fillMaxWidth(), Arrangement.Start, Alignment.Top) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary.copy(0.8f), colors.secondary.copy(0.7f))))
                .border(0.6.dp, Color.White.copy(0.2f), CircleShape),
            Alignment.Center
        ) { Text("✦", color = Color.White, fontSize = 12.sp) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.widthIn(max = 280.dp)
                .glassBackground(
                    RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    if (isDark) colors.surfaceVariant else Color.White,
                    if (isDark) 0.65f else 0.80f, elevation = 3.dp
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (b.text.isBlank() && b.isStreaming) TypingIndicator()
                else {
                    Text(b.text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    if (b.isStreaming) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                            color = colors.primary.copy(0.6f), trackColor = colors.primary.copy(0.12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val t = rememberInfiniteTransition("typing")
    Row(Arrangement.spacedBy(5.dp), Alignment.CenterVertically) {
        val colors = MaterialTheme.colorScheme
        repeat(3) { i ->
            val a by t.animateFloat(0.22f, 1f,
                infiniteRepeatable(tween(400, i * 130, EaseInOutSine), RepeatMode.Reverse), "d$i")
            Box(Modifier.size(7.dp).clip(CircleShape).background(colors.primary.copy(a)))
        }
    }
}

@Composable
private fun ToolCard(b: ChatBubble.ToolExecution) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    val bAlpha by if (b.isRunning) {
        rememberInfiniteTransition("border").animateFloat(0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "ba")
    } else remember { mutableFloatStateOf(0.3f) }

    var expanded by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxWidth()
            .glassBackground(RoundedCornerShape(14.dp), if (isDark) colors.surfaceVariant else Color.White,
                0.80f, b.run { if (isError) colors.error else colors.primary }.let { 0.3f },
                elevation = 4.dp)
            .border(0.8.dp,
                (if (b.isError) colors.error else colors.primary).copy(bAlpha), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, Alignment.CenterVertically, Arrangement.SpaceBetween) {
                Row(Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                    if (b.isRunning) CircularProgressIndicator(Modifier.size(14.dp), colors.primary, strokeWidth = 2.dp)
                    else Icon(if (b.isError) Icons.Outlined.Error else Icons.Outlined.CheckCircle,
                        null, Modifier.size(14.dp), if (b.isError) colors.error else Color(0xFF4CAF88))
                    Text(b.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                }
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null,
                    Modifier.size(16.dp), colors.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val prettyArgs = remember(b.params) {
                        try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(b.params)) }
                        catch (_: Exception) { b.params }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant.copy(0.5f)).padding(8.dp)
                    ) {
                        Text(prettyArgs, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = colors.onSurfaceVariant)
                    }
                    b.result?.let { res ->
                        val pr = remember(res) {
                            try { GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(res)) }
                            catch (_: Exception) { res }
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (b.isError) colors.errorContainer.copy(0.4f) else Color(0xFF4CAF88).copy(0.08f))
                                .padding(8.dp)
                        ) {
                            Text(pr, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (b.isError) colors.error else colors.onSurface)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(colors.errorContainer.copy(0.5f)).padding(12.dp),
        Arrangement.spacedBy(8.dp), Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Error, null, Modifier.size(16.dp), colors.error)
        Text(b.message, style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer)
    }
}

// expose AiChatPanel for ReadingScreen
@Composable
fun AiChatPanel(
    messages: List<AiMessage>, input: String, onInputChange: (String) -> Unit,
    onSend: () -> Unit, context: String, modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary.copy(0.8f), colors.secondary.copy(0.7f)))),
                Alignment.Center
            ) { Text("✦", color = Color.White) }
            Column {
                Text("AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(context, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.outline.copy(0.15f))
        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), reverseLayout = true) {
            items(messages.reversed().size) { i ->
                val msg = messages.reversed()[i]
                val isUser = msg.role == AiMessage.Role.USER
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    if (isUser) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.widthIn(max = 260.dp).clip(
                            if (isUser) RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp)
                            else RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)
                        ).background(if (isUser) colors.primary else colors.surfaceVariant).padding(10.dp)
                    ) {
                        Text(msg.content,
                            color = if (isUser) colors.onPrimary else colors.onSurface,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = onInputChange, modifier = Modifier.weight(1f),
                placeholder = { Text("询问…") }, shape = RoundedCornerShape(16.dp), singleLine = true
            )
            IconButton(onClick = onSend, enabled = input.isNotBlank()) {
                Icon(Icons.Outlined.Send, "发送", tint = if (input.isNotBlank()) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(count: Int, content: @Composable (Int) -> Unit) {
    repeat(count) { item { content(it) } }
}
