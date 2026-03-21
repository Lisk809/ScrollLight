# 📖 光言圣经 (ScrollLight)

> 一款以 Material 3 设计语言构建的 Android 圣经阅读应用，内置 AI 助读接口，支持双语对照、经文高亮、读经计划等功能。

[![Beta Build](https://github.com/YOUR_USERNAME/ScrollLight/actions/workflows/beta.yml/badge.svg)](https://github.com/YOUR_USERNAME/ScrollLight/actions/workflows/beta.yml)
[![Release](https://github.com/YOUR_USERNAME/ScrollLight/actions/workflows/release.yml/badge.svg)](https://github.com/YOUR_USERNAME/ScrollLight/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ 功能

| 功能 | 说明 |
|------|------|
| 📅 每日经文 | 首页展示当日主题经文，金色卡片设计 |
| 📚 圣经目录 | 旧约 / 新约全书目录，点击直达章节 |
| 📖 阅读界面 | 大字体显示、经文高亮（6色）、笔记、书签 |
| 🔄 双语对照 | 和合本 + NIV 并排显示 |
| 🔍 全文搜索 | 支持按范围搜索（整本 / 旧约 / 新约 / 四福音） |
| 📋 读经计划 | 13 个预置计划，含大斋期、365天全本等 |
| 🤖 AI 助读 | 预留标准化 AI 接口，可接入 Claude / GPT 等 |
| 🌙 深色模式 | 跟随系统自动切换 |

---

## 🛠 技术栈

```
Kotlin + Jetpack Compose (Material 3)
├── Navigation Compose   — 屏幕导航
├── Hilt                 — 依赖注入
├── Room                 — 本地数据库（高亮、笔记、书签）
├── DataStore            — 用户偏好
├── Coroutines / Flow    — 异步数据流
└── Coil                 — 图片加载
```

**最低 Android 版本**：Android 8.0 (API 26)  
**目标版本**：Android 15 (API 35)

---

## 🚀 快速开始

### 环境要求

- Android Studio Ladybug (2024.2) 或更新版本
- JDK 17
- Android SDK 35

### 克隆并构建

```bash
git clone https://github.com/YOUR_USERNAME/ScrollLight.git
cd ScrollLight

# 初始化 Gradle Wrapper（首次必须执行）
gradle wrapper --gradle-version 8.11.1

# 构建 Debug APK
./gradlew assembleDebug

# 安装到连接的设备
./gradlew installDebug
```

构建产物位于：`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 接入 AI 助读

AI 接口已完全解耦，只需实现 `AiService` 接口并通过 Hilt 绑定即可。

### 1. 实现接口

```kotlin
// 在 app/src/main/java/.../ai/ 目录新建文件
class ClaudeAiService @Inject constructor() : AiService {
    override suspend fun chat(
        messages: List<AiMessage>,
        context: AiReadingContext,   // 当前阅读章节、选中经文等
        controller: AiController     // 调用此对象控制 UI
    ): AiMessage {
        // 调用 Anthropic API（或任意 LLM）
        val response = callAnthropicApi(messages, context)

        // AI 可以主动控制 UI：
        controller.highlightVerses(listOf(3, 5, 7), "YELLOW")  // 高亮经文
        controller.scrollToVerse(3)                             // 滚动到指定节
        controller.showAnnotation(3, "这里指耶稣变像事件…")    // 显示注解气泡
        controller.showCrossReferences(listOf("出 3:6", "王上 19:8")) // 交叉参考

        return AiMessage(AiMessage.Role.ASSISTANT, response)
    }
}
```

### 2. 替换绑定

在 `di/AppModule.kt` 中替换 `StubAiService`：

```kotlin
@Provides
@Singleton
fun provideAiService(): AiService = ClaudeAiService()
```

### AI 可调用的 UI 控制 API

```kotlin
controller.navigateToChapter("mat", 5)          // 导航到指定章节
controller.highlightVerses(listOf(1,2,3), "GREEN") // 高亮经文（6种颜色）
controller.scrollToVerse(14)                     // 滚动到指定节
controller.showAnnotation(14, "注释文字")        // 经文旁注解气泡
controller.showCrossReferences(listOf("…"))      // 交叉参考面板
controller.triggerSearch("以利亚", SearchScope.NEW_TESTAMENT) // 触发搜索
controller.toggleAiPanel(true)                   // 开/关 AI 聊天面板
controller.readAloud(1, 5)                       // 朗读经文范围
controller.clearAiOverlay()                      // 清除所有 AI 标注
```

---

## 📦 CI/CD 工作流

本项目包含两套独立工作流：

### 🧪 测试版 (`beta.yml`)

| 触发条件 | 行为 |
|----------|------|
| 推送到 `develop` / `feature/**` | 运行 Lint + 单元测试 → 构建 Debug APK |
| Pull Request | 额外运行模拟器仪器测试，并在 PR 评论中附 APK 链接 |
| 手动触发 | 同上 |

APK 作为 **GitHub Actions Artifact** 上传，保留 14 天。

### 🚀 稳定版 (`release.yml`)

| 触发条件 | 行为 |
|----------|------|
| 推送 `v*.*.*` Tag | 运行完整测试 → 构建签名 APK + AAB → 发布 GitHub Release |
| 手动触发 | 指定版本号和发布说明 |

#### 发布流程

```bash
# 1. 确保代码已合并到 main
git checkout main

# 2. 打 Tag（触发自动构建和发布）
git tag v1.0.1
git push origin v1.0.1

# 3. 等待 Actions 完成后，GitHub Release 自动创建
```

#### 配置签名 Secrets

在 GitHub 仓库 Settings → Secrets → Actions 中添加：

| Secret 名称 | 值 |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i release.jks` 的输出 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

---

## 📁 项目结构

```
app/src/main/java/com/scrolllight/bible/
├── ai/
│   └── AiController.kt      # AI 命令总线 + AiService 接口
├── data/
│   ├── db/                  # Room DAOs + Database
│   ├── model/               # 数据模型（BibleBook, Verse, Plan…）
│   └── repository/          # BibleRepository + UserRepository
├── di/
│   └── AppModule.kt         # Hilt 依赖注入
└── ui/
    ├── components/          # 共用 Compose 组件
    ├── home/                # 首页
    ├── reading/             # 阅读 + 目录
    ├── search/              # 搜索
    ├── plans/               # 读经计划
    ├── profile/             # 我的
    ├── navigation/          # NavHost + 路由定义
    └── theme/               # Material 3 主题（琥珀色系）
```

---

## 📄 License

```
MIT License — Copyright (c) 2026 Lisk
```

本项目开源，欢迎贡献。圣经文本版权归各译本版权方所有，商业使用前请确认版权许可。

---

## 🙏 致谢

- [Material 3 Design System](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/compose)
- 和合本圣经（公共领域）
