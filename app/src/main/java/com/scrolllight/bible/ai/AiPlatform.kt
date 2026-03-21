package com.scrolllight.bible.ai

/**
 * 支持的 AI 平台列表，每个平台有各自的 API 接口差异配置。
 * 添加新平台只需在此文件新增枚举值。
 */
enum class AiPlatform(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val icon: String,
    val notes: String,
    // API 行为差异
    val toolChoiceFormat: ToolChoiceFormat = ToolChoiceFormat.STRING_AUTO,
    val supportsTools: Boolean = true,
    val requiresStreamFalseForTools: Boolean = false,
    val extraHeaders: Map<String, String> = emptyMap()
) {
    OPENAI(
        displayName   = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel  = "gpt-4o-mini",
        icon          = "🟢",
        notes         = "官方接口，完整支持 Tool Calling 和流式输出",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    ANTHROPIC(
        displayName   = "Anthropic",
        defaultBaseUrl = "https://api.anthropic.com/v1",
        defaultModel  = "claude-sonnet-4-5",
        icon          = "🟠",
        notes         = "Claude 系列，需要额外请求头。建议关闭工具调用使用纯对话模式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO,
        supportsTools = false,   // Anthropic API 格式与 OpenAI 不同，不兼容本客户端 tools
        extraHeaders  = mapOf("anthropic-version" to "2023-06-01", "anthropic-beta" to "messages-2023-12-15")
    ),
    ALIBABA_BAILIAN(
        displayName   = "阿里百炼",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        defaultModel  = "qwen-turbo",
        icon          = "🔵",
        notes         = "通义千问系列。tool_choice 只支持字符串格式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    DEEPSEEK(
        displayName   = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel  = "deepseek-chat",
        icon          = "🐋",
        notes         = "深度求索，高性价比。完整支持 OpenAI 格式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    GROQ(
        displayName   = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel  = "llama-3.3-70b-versatile",
        icon          = "⚡",
        notes         = "超快推理速度，工具调用需非流式模式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO,
        requiresStreamFalseForTools = true
    ),
    NVIDIA_NIM(
        displayName   = "NVIDIA NIM",
        defaultBaseUrl = "https://integrate.api.nvidia.com/v1",
        defaultModel  = "meta/llama-3.1-70b-instruct",
        icon          = "🟩",
        notes         = "NVIDIA NIM 托管模型，标准 OpenAI 兼容",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    SILICONFLOW(
        displayName   = "硅基流动",
        defaultBaseUrl = "https://api.siliconflow.cn/v1",
        defaultModel  = "Qwen/Qwen2.5-7B-Instruct",
        icon          = "🌊",
        notes         = "国内平台，多模型支持，标准 OpenAI 格式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    ZHIPU(
        displayName   = "智谱 GLM",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModel  = "glm-4-flash",
        icon          = "🦋",
        notes         = "清华智谱 GLM 系列，兼容 OpenAI 格式",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    GEMINI(
        displayName   = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel  = "gemini-2.0-flash",
        icon          = "✨",
        notes         = "Google Gemini，通过 OpenAI 兼容层访问",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    MOONSHOT(
        displayName   = "月之暗面 Kimi",
        defaultBaseUrl = "https://api.moonshot.cn/v1",
        defaultModel  = "moonshot-v1-8k",
        icon          = "🌙",
        notes         = "Kimi 系列，上下文窗口超长",
        toolChoiceFormat = ToolChoiceFormat.STRING_AUTO
    ),
    OLLAMA(
        displayName   = "Ollama (本地)",
        defaultBaseUrl = "http://localhost:11434/v1",
        defaultModel  = "llama3.2",
        icon          = "🦙",
        notes         = "本地运行，免费无限制。工具调用支持取决于具体模型",
        toolChoiceFormat = ToolChoiceFormat.NONE,  // 大多数本地模型不支持
        supportsTools = false
    ),
    CUSTOM(
        displayName   = "自定义",
        defaultBaseUrl = "",
        defaultModel  = "",
        icon          = "⚙️",
        notes         = "手动填写接口地址和模型名"
    );

    companion object {
        fun fromBaseUrl(url: String): AiPlatform =
            values().firstOrNull { url.startsWith(it.defaultBaseUrl.substringBefore("/v")) }
                ?: CUSTOM
    }
}

/**
 * tool_choice 字段的发送格式
 * 不同平台支持不同形式，错误的格式会导致 400。
 */
enum class ToolChoiceFormat {
    STRING_AUTO,    // "tool_choice": "auto"  ← 最广泛兼容（OpenAI, Bailian, DeepSeek...）
    OBJECT_AUTO,    // "tool_choice": {"type": "auto"}  ← 较新 OpenAI spec，兼容性差
    NONE            // 不发送 tool_choice 字段
}
