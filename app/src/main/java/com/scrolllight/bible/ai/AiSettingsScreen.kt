package com.scrolllight.bible.ai

import androidx.compose.animation.AnimatedVisibility
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
    val draft = uiState.draft

    var showKey       by remember { mutableStateOf(false) }
    var showAdvanced  by remember { mutableStateOf(false) }
    var showModelList by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) { vm.resetSaved(); onBack() }
    }

    // Model error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.modelError) {
        uiState.modelError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearModelError()
        }
    }

    val modelPresets = listOf(
        "gpt-4o-mini", "gpt-4o", "o1-mini",
        "claude-3-5-haiku-20241022", "claude-sonnet-4-5",
        "deepseek-chat", "deepseek-reasoner",
        "qwen-turbo", "qwen-plus",
        "gemini-2.0-flash", "gemini-1.5-pro"
    )
    val baseUrlPresets = listOf(
        "https://api.openai.com/v1"                                     to "OpenAI",
        "https://api.anthropic.com/v1"                                  to "Anthropic",
        "https://api.deepseek.com/v1"                                   to "DeepSeek",
        "https://dashscope.aliyuncs.com/compatible-mode/v1"             to "阿里百炼",
        "https://generativelanguage.googleapis.com/v1beta/openai"       to "Gemini",
        "http://localhost:11434/v1"                                      to "Ollama",
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✦", color = MaterialTheme.colorScheme.primary)
                        Text("AI 助读设置", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                actions = {
                    TextButton(onClick = { vm.save() }) {
                        Text("保存", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            // Status
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (draft.isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        else MaterialTheme.colorScheme.errorContainer.copy(0.4f)
            ) {
                Row(modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (draft.isConfigured) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        null,
                        tint = if (draft.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (draft.isConfigured) "AI 已配置，点击右上角保存后生效" else "请填写 API Key 后保存",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    baseUrlPresets.forEach { (url, label) ->
                        FilterChip(
                            selected = draft.baseUrl == url, onClick = { vm.setBaseUrl(url) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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
                    // Fetch model list button
                    FilledTonalButton(
                        onClick = { vm.fetchModels(); showModelList = true },
                        shape   = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoadingModels
                    ) {
                        if (uiState.isLoadingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.List, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("获取列表", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Model list dropdown
                AnimatedVisibility(showModelList && uiState.models.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("从 API 获取的模型（${uiState.models.size}个）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { showModelList = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider()
                            uiState.models.take(30).forEach { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.setModel(model.id); showModelList = false }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model.id, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (model.id == draft.model) FontWeight.Bold else FontWeight.Normal,
                                            color = if (model.id == draft.model) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface)
                                        if (model.owned.isNotBlank()) {
                                            Text(model.owned, style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (model.id == draft.model) {
                                        Icon(Icons.Outlined.CheckCircle, null,
                                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick preset chips
                Text("快速选择预置模型：", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    modelPresets.forEach { model ->
                        FilterChip(
                            selected = draft.model == model, onClick = { vm.setModel(model) },
                            label = { Text(model, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // ── Stream toggle ─────────────────────────────────────────────
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("流式输出", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("逐字显示回复（部分API不支持可关闭）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = draft.streamEnabled, onCheckedChange = { vm.setStream(it) })
                }
            }

            // ── Tool Calling toggle ──────────────────────────────────────────
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("工具调用 (Tool Calling)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("让AI直接操控阅读界面（高亮、跳转等）", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draft.toolCallingEnabled, onCheckedChange = { vm.setToolCalling(it) })
                    }
                    AnimatedVisibility(visible = !draft.toolCallingEnabled) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.5f)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Info, null, modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary)
                                Text("关闭后AI只能对话，不能操控界面。\n建议：阿里百炼/部分模型如遇400错误，先关闭此项再试。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
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
                    SettingsSection("创意度（temperature）：${"%.1f".format(draft.temperature)}") {
                        Slider(value = draft.temperature, onValueChange = { vm.setTemperature(it) },
                            valueRange = 0f..2f, steps = 19)
                        Text(
                            when {
                                draft.temperature < 0.5f -> "更严谨一致"
                                draft.temperature < 1.2f -> "均衡（推荐）"
                                else -> "更具创意随机"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp))
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
