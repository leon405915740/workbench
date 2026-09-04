# 工作台

一个本地优先的 Android 个人管理应用，把今日计划、习惯、运动、阅读、记账、随笔、剪报与专注工具集中在同一个工作台中。

> 当前处于开发阶段，仓库提供源码构建的 Debug 版本，不应视为正式发行版。

## 功能

- **首页概览**：聚合今日计划、习惯、阅读、运动、随笔与剪报，并展示近期趋势和快捷入口。
- **今日计划**：新增、编辑、搜索、置顶、完成与归档；支持日期、P0/P1/P2 优先级、图片、可编辑时间和本地到点提醒。
- **习惯与运动**：习惯管理、每日打卡、补录、连续与月度统计，以及独立运动计时和时长记录。
- **阅读**：书目、目标进度、增量日志、撤销、搜索、置顶、图片和完成归档。
- **记账**：收入/支出录入、编辑删除、附件、搜索、分类统计、七日趋势、CSV 导出和分类记忆。
- **自然语言记账**：本地规则优先，可选使用 DeepSeek 或 OpenCode Go 完成复杂解析、分类和统计问答。
- **付款通知快捷记账**：读取系统实际投递的支付通知，识别 BigText 等完整正文、提取金额并打开预填记账卡片。
- **随笔与剪报**：支持正文、标签、日期、图片、搜索、置顶，以及情绪、类型、来源或阅读状态。
- **专注与状态**：25/5 分钟番茄钟、结束通知、进程恢复，以及每日状态评分和七日趋势。
- **个人中心与设置**：头像、昵称、AI 服务配置、连接测试、快捷记账开关、系统权限入口、分类映射和数据导出。

## 当前状态

- applicationId：`com.aigrowth.os`
- 当前源码版本：`1.10.1`，versionCode `15`
- 最低系统：Android 8.0 / API 26
- targetSdk / compileSdk：34
- 最近一次全仓验证：49/49 JVM 测试通过，Lint 0 error，AndroidTest 编译和 Debug 构建通过
- Room 迁移测试：SM-S9110 / Android 15 模拟器上 4/4 通过

当前构建产物、设备验证和未完成事项以 [工作交接文档](./工作交接文档.md) 为准。

## 技术栈

- Kotlin 1.9.22、JDK 17
- Gradle Wrapper 8.4、Android Gradle Plugin 8.2.2
- Jetpack Compose、Material 3、Navigation Compose
- Hilt 2.48
- Room 2.6.1
- Kotlin Coroutines / Flow
- DataStore Preferences、SharedPreferences
- Retrofit 2、OkHttp 4、Gson
- JUnit 4、AndroidX Test、Espresso、Compose UI Test

## 项目结构

```text
app/
  Android 宿主、导航、首页和当前工作台功能

core/
  database/    工作台 Room 数据层与迁移
  design/      Compose 设计组件
  network/     网络配置
  ai-engine/   AI 客户端及遗留学习/创作 Agent

feature/
  accounting/  当前记账域、AI 解析、统计、分类记忆和通知快捷记账
  learning/    遗留学习模块，当前主导航未开放
  creator/     遗留创作模块，当前主导航未开放
  growth/      遗留占位模块

docs/
  任务、审查和项目说明
```

Gradle 模块清单见 [settings.gradle.kts](./settings.gradle.kts)。

## 环境要求

- Android Studio Hedgehog 或更新版本
- JDK 17
- Android SDK 34
- 可选：ADB 设备或模拟器

当前 `gradle.properties` 含维护者 Windows 环境使用的 JDK 与 AAPT2 路径覆盖。其他机器首次构建前，应删除、替换或通过命令行覆盖 `org.gradle.java.home` 和 `android.aapt2FromMavenOverride`，不要把个人 SDK 路径或凭据提交到仓库。

## 获取与构建

```bash
git clone https://github.com/leon405915740/workbench.git
cd workbench
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :app:assembleDebug
```

APK 输出路径：

```text
app/build/outputs/apk/debug/工作台-{versionName}.apk
```

保留设备现有应用数据进行覆盖安装：

```bash
adb install -r "app/build/outputs/apk/debug/工作台-{versionName}.apk"
```

## 测试与静态检查

完整 Debug 验证：

```powershell
.\gradlew.bat lintDebug testDebugUnitTest :app:compileDebugAndroidTestKotlin assembleDebug --no-daemon
```

单独执行：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

设备测试仅应在专用、可清空的模拟器上运行：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

> `connectedDebugAndroidTest` 的安装/清理流程可能卸载目标 Debug 包并清除其本地数据。不要在保存真实账单或其他重要数据的设备上运行；先备份或使用一次性模拟器。

## 可选 AI 配置

不配置 API Key 也可以使用计划、习惯、阅读、手动记账等本地功能。

需要自然语言记账或统计问答时，在应用的“个人中心 → 服务配置”中：

1. 选择 DeepSeek 或 OpenCode Go。
2. 填写自己的 API Key 和模型名。
3. 先执行连接测试，再保存配置。

输入给 AI 的内容会发送到所选第三方服务。不要把 API Key 写入源码、README、Git 提交或问题反馈。

## 权限与系统行为

| 能力 | 所需权限或系统设置 |
| --- | --- |
| AI 服务 | 网络权限 |
| 今日计划、番茄钟通知 | Android 13+ 通知权限 |
| 计划准点提醒 | Android 12+“闹钟和提醒”特殊访问；未授权时降级为非精确提醒 |
| 重启后恢复提醒 | 开机广播 |
| 付款通知快捷记账 | 系统“通知使用权” |
| 后台打开预填记账卡片 | 悬浮窗权限 |
| 通知监听保活 | 系统要求的前台服务及静默常驻通知 |

所有可选权限都应由用户在系统设置中主动授予。

## 数据与安全

- 工作台数据保存在本地 `workbench.db`，记账数据保存在本地 `accounting.db`。
- 数据库升级使用显式 Room Migration，不使用清库式迁移。
- 当前没有项目级云同步；卸载应用会删除未被系统备份的本地数据。
- API Key 保存在本地 DataStore，但当前只是 Base64 编码混淆，不等同于 Android Keystore 加密。
- Debug 日志会遮盖常见手机号、银行卡号和带货币单位的金额；分享日志前仍应人工检查商户、备注等业务内容。
- Release 构建尚未启用混淆，也没有公开可验证的正式签名发布流程。

## 已知限制

- 付款快捷记账只能处理真正进入 Android 系统通知栏的内容；应用内消息或服务号消息无法被监听器捕获。
- 精确提醒会受到系统授权、Doze 和厂商省电策略影响。
- 当前没有记账数据导入或完整应用数据导入流程。
- `feature:learning`、`feature:creator` 和 `feature:growth` 尚未接入当前主导航。
- GitHub 当前没有 Release；普通 Git push 不会上传被忽略的 APK 文件。

## 项目文档

- [当前工作交接与验收状态](./工作交接文档.md)
- [今日计划提醒与全仓审查任务](./docs/agent-tasks/today-plan-reminder-and-code-audit.md)

## 反馈

请通过 [GitHub Issues](https://github.com/leon405915740/workbench/issues) 提交可复现的问题，并附上 Android 版本、操作步骤及已脱敏日志。

## 许可证

本仓库目前没有 `LICENSE` 文件，也尚未声明开源许可证。仓库公开可见不代表自动获得复制、修改或再分发授权。
