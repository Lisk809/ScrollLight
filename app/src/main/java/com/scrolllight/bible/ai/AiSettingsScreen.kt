package com.scrolllight.bible.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: AiSettingsViewModel = hiltViewModel()
) {
    val uiState by vm.state.collectAsState()
    val draft    = uiState.draft
    val colors   = MaterialTheme.colorScheme
    val isDark   = colors.background.luminance() < 0.15f

    var showKey      by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showModelList by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.isSaved)     { if (uiState.isSaved)     { vm.resetSaved(); onBack() } }
    LaunchedEffect(uiState.modelError)  { uiState.modelError?.let { snackbar.showSnackbar(it); vm.clearModelError() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✦", color = colors.primary)
                        Text("AI 助读设置", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                actions = {
                    TextButton(onClick = { vm.save() }, enabled = draft.isConfigured) {
                        Text("保存", fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Status banner ─────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (draft.isConfigured) colors.primaryContainer.copy(0.5f)
                        else colors.errorContainer.copy(0.4f)
            ) {
                Row(Modifier.padding(14.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                    Icon(
                        if (draft.isConfigured) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        null,
                        tint = if (draft.isConfigured) colors.primary else colors.error
                    )
                    Text(
                        if (draft.isConfigured) "已配置 · ${draft.platform.displayName} · ${draft.model}"
                        else "请填写 API Key 后保存",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Platform Picker ───────────────────────────────────────────
            SettingsSection("AI 平台") {
                val platforms = AiPlatform.values().toList()
                // 4-column grid
                platforms.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { platform ->
                            val selected = draft.platform == platform
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) colors.primaryContainer.copy(0.7f)
                                        else if (isDark) colors.surfaceVariant.copy(0.5f)
                                        else Color.White.copy(0.6f)
                                    )
                                    .border(
                                        if (selected) 1.5.dp else 0.dp,
                                        if (selected) colors.primary.copy(0.6f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { vm.setPlatform(platform) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(platform.icon, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    platform.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                // Platform notes
                if (draft.platform.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.secondaryContainer.copy(0.4f)
                    ) {
                        Row(Modifier.padding(10.dp), Arrangement.spacedBy(6.dp), Alignment.Top) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(14.dp), colors.secondary)
                            Text(draft.platform.notes, style = MaterialTheme.typography.labelSmall,
                                color = colors.onSecondaryContainer)
                        }
                    }
                }
            }

            // ── Base URL ──────────────────────────────────────────────────
            SettingsSection("接口地址") {
                OutlinedTextField(
                    value = draft.baseUrl, onValueChange = { vm.setBaseUrl(it) },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Link, null) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }

            // ── API Key ───────────────────────────────────────────────────
            SettingsSection("API Key") {
                OutlinedTextField(
                    value = draft.apiKey, onValueChange = { vm.setApiKey(it) },
                    label = { Text("API Key") }, placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Outlined.Key, null) },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        }
                    },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            // ── Model ─────────────────────────────────────────────────────
            SettingsSection("模型") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft.model, onValueChange = { vm.setModel(it) },
                        label = { Text("模型名称") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.SmartToy, null) }, singleLine = true
                    )
                    FilledTonalButton(
                        onClick = { vm.fetchModels(); showModelList = true },
                        shape = RoundedCornerShape(12.dp), enabled = !uiState.isLoadingModels
                    ) {
                        if (uiState.isLoadingModels)
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.List, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("获取列表", style = MaterialTheme.typography.labelMedium)
                    }
                }
                // Model list dropdown
                AnimatedVisibility(showModelList && uiState.models.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = if (isDark) colors.surfaceVariant else Color.White.copy(0.9f),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                Arrangement.SpaceBetween, Alignment.CenterVertically
                            ) {
                                Text("API 返回模型（${uiState.models.size}个）",
                                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                IconButton(onClick = { showModelList = false }, Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Close, null, Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider()
                            uiState.models.take(40).forEach { model ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { vm.setModel(model.id); showModelList = false }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    Arrangement.SpaceBetween, Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(model.id, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (model.id == draft.model) FontWeight.Bold else FontWeight.Normal,
                                            color = if (model.id == draft.model) colors.primary else colors.onSurface)
                                        if (model.owned.isNotBlank()) {
                                            Text(model.owned, style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant)
                                        }
                                    }
                                    if (model.id == draft.model) {
                                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), colors.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Stream toggle ─────────────────────────────────────────────
            ToggleRow(
                title    = "流式输出",
                subtitle = "逐字显示回复（推荐开启）",
                checked  = draft.streamEnabled,
                onChange = { vm.setStream(it) }
            )

            // ── Tool Calling toggle ───────────────────────────────────────
            ToggleRow(
                title    = "工具调用 (Tool Calling)",
                subtitle = if (!draft.platform.supportsTools)
                    "当前平台 (${draft.platform.displayName}) 不支持工具调用"
                else "让AI直接高亮经文、跳转章节等",
                checked  = draft.toolCallingEnabled && draft.platform.supportsTools,
                onChange = { vm.setToolCalling(it) },
                enabled  = draft.platform.supportsTools
            )
            if (draft.platform.requiresStreamFalseForTools && draft.toolCallingEnabled) {
                Surface(shape = RoundedCornerShape(8.dp), color = colors.tertiaryContainer.copy(0.4f)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(14.dp), colors.tertiary)
                        Text("${draft.platform.displayName} 使用工具调用时将自动切换为非流式模式",
                            style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer)
                    }
                }
            }

            // ── Advanced ──────────────────────────────────────────────────
            TextButton(
                onClick  = { showAdvanced = !showAdvanced },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                Spacer(Modifier.width(4.dp))
                Text(if (showAdvanced) "收起高级设置" else "高级设置")
            }

            AnimatedVisibility(showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSection("最大 Token 数：${draft.maxTokens}") {
                        Slider(value = draft.maxTokens.toFloat(), onValueChange = { vm.setMaxTokens(it.toInt()) },
                            valueRange = 256f..8192f, steps = 30)
                    }
                    SettingsSection("创意度 (temperature)：${"%.1f".format(draft.temperature)}") {
                        Slider(value = draft.temperature, onValueChange = { vm.setTemperature(it) },
                            valueRange = 0f..2f, steps = 19)
                        Text(
                            when {
                                draft.temperature < 0.5f -> "更严谨一致"
                                draft.temperature < 1.2f -> "均衡（推荐）"
                                else -> "更具创意随机"
                            },
                            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant
                        )
                    }
                    SettingsSection("系统提示词") {
                        OutlinedTextField(
                            value = draft.systemPrompt, onValueChange = { vm.setSystemPrompt(it) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 15
                        )
                        TextButton(onClick = { vm.resetSystemPrompt() }) {
                            Icon(Icons.Outlined.RestartAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("恢复默认")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { vm.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                enabled  = draft.isConfigured
            ) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("保存配置", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String, subtitle: String,
    checked: Boolean, onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) colors.surfaceVariant.copy(0.5f) else Color.White.copy(0.7f),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    color = if (enabled) colors.onSurface else colors.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}
