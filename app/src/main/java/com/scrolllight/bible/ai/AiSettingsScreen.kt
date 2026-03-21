package com.scrolllight.bible.ai

import androidx.compose.animation.AnimatedVisibility
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: AiSettingsViewModel = hiltViewModel()
) {
    val draft by vm.draft.collectAsState()
    val saved  by vm.isSaved.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showPromptEditor by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) { vm.isSaved.value = false; onBack() }
    }

    // Preset model shortcuts
    val modelPresets = listOf(
        "gpt-4o-mini" to "GPT-4o Mini",
        "gpt-4o" to "GPT-4o",
        "claude-3-5-haiku-20241022" to "Claude 3.5 Haiku",
        "claude-sonnet-4-5" to "Claude Sonnet 4.5",
        "deepseek-chat" to "DeepSeek Chat",
        "qwen-turbo" to "Qwen Turbo",
        "gemini-2.0-flash" to "Gemini 2.0 Flash",
    )
    val baseUrlPresets = listOf(
        "https://api.openai.com/v1" to "OpenAI 官方",
        "https://api.anthropic.com/v1" to "Anthropic 官方",
        "https://api.deepseek.com/v1" to "DeepSeek",
        "https://dashscope.aliyuncs.com/compatible-mode/v1" to "阿里云百炼",
        "https://generativelanguage.googleapis.com/v1beta/openai" to "Google Gemini",
        "http://localhost:11434/v1" to "Ollama 本地",
    )

    Scaffold(
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
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Status card ───────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (draft.isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        else MaterialTheme.colorScheme.errorContainer.copy(0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (draft.isConfigured) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        null,
                        tint = if (draft.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (draft.isConfigured) "AI 已配置，点击保存后生效" else "请填写 API Key 后保存",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Base URL ──────────────────────────────────────────────────
            SettingsSection(title = "接口地址") {
                OutlinedTextField(
                    value = draft.baseUrl,
                    onValueChange = { vm.setBaseUrl(it) },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                // Presets
                Text("快速选择：", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                baseUrlPresets.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (url, label) ->
                            FilterChip(
                                selected = draft.baseUrl == url,
                                onClick  = { vm.setBaseUrl(url) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(8.dp)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── API Key ───────────────────────────────────────────────────
            SettingsSection(title = "API Key") {
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { vm.setApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
            SettingsSection(title = "模型") {
                OutlinedTextField(
                    value = draft.model,
                    onValueChange = { vm.setModel(it) },
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.SmartToy, null) },
                    singleLine = true
                )
                Text("快速选择：", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                modelPresets.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (model, label) ->
                            FilterChip(
                                selected = draft.model == model,
                                onClick  = { vm.setModel(model) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(8.dp)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ── Streaming toggle ──────────────────────────────────────────
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("流式输出", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("逐字显示AI回复（推荐开启）", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = draft.streamEnabled, onCheckedChange = { vm.setStream(it) })
                }
            }

            // ── Advanced ──────────────────────────────────────────────────
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                Spacer(Modifier.width(4.dp))
                Text(if (showAdvanced) "收起高级设置" else "高级设置")
            }

            AnimatedVisibility(showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Max tokens slider
                    SettingsSection(title = "最大 Token 数：${draft.maxTokens}") {
                        Slider(
                            value = draft.maxTokens.toFloat(),
                            onValueChange = { vm.setMaxTokens(it.toInt()) },
                            valueRange = 256f..8192f,
                            steps = 30
                        )
                    }
                    // Temperature slider
                    SettingsSection(title = "创意度 (temperature)：${"%.1f".format(draft.temperature)}") {
                        Slider(
                            value = draft.temperature,
                            onValueChange = { vm.setTemperature(it) },
                            valueRange = 0f..2f,
                            steps = 19
                        )
                        Text(
                            when {
                                draft.temperature < 0.5f -> "更严谨、一致"
                                draft.temperature < 1.2f -> "均衡（推荐）"
                                else -> "更具创意、随机"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // System prompt editor
                    SettingsSection(title = "系统提示词") {
                        OutlinedTextField(
                            value = draft.systemPrompt,
                            onValueChange = { vm.setSystemPrompt(it) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 12
                        )
                        TextButton(onClick = { vm.resetSystemPrompt() }) {
                            Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("恢复默认提示词")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = { vm.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = draft.isConfigured
            ) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("保存配置", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
