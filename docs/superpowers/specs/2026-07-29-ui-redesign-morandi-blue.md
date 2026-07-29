# UI 美化设计：莫兰迪雾蓝配色 + 精致卡片质感

## 日期
2026-07-29

## 背景

当前记账 App 使用微信绿主色调（`#07C160`），整体视觉偏扁平、实用。三个主页面（AI 记账聊天、统计看板、设置）功能完整但视觉层次单薄，缺乏质感。

本次美化目标：在不改变任何功能和布局结构的前提下，通过配色系统升级 + 卡片质感提升，让整体观感更精致、更现代、更有设计感。

## 设计决策

经可视化对比讨论，确定以下方案：

**风格方向**：莫兰迪冷调配色 + A 方案的精致卡片质感（柔和阴影、渐变、立体圆角）

**配色角色分配（方案 B · 雾蓝主导）**：雾蓝同时承担品牌色和支出语义色，松绿承担收入语义色，整体统一在冷灰色调中，冷静、有数据感。

## 色彩系统

### 旧 → 新色值映射

| 角色 | 旧色值 | 新色值 | 变量名（保持不变） |
|------|--------|--------|---------------------|
| 品牌主色 | `#07C160` 微信绿 | `#7C81A4` 雾蓝 | `WeChatGreen` |
| 品牌浅色 | `#E8F8EE` | `#E8E8EE` 浅雾蓝 | `WeChatGreenLight` |
| 品牌深色 | `#06AD56` | `#626788` 深雾蓝 | `WeChatGreenDark` |
| 背景 | `#F5F5F5` 灰 | `#F0F0F4` 冷灰 | `BackgroundGray` |
| 卡片底 | `#FFFFFF` | `#FFFFFF` 不变 | `CardWhite` |
| 主文字 | `#333333` | `#5A5F7A` 深雾蓝灰 | `TextPrimary` |
| 次要文字 | `#999999` | `#BFBCCB` 浅雾紫 | `TextSecondary` |
| 支出金额 | `#1677FF` 蓝 | `#7C81A4` 雾蓝 | `TextAmount` |
| 收入金额 | `#07C160` 绿 | `#527A72` 松绿 | `TextIncome` |
| 删除/错误 | `#F5222D` 红 | `#C97A7A` 暖珊瑚红 | `TextDelete` |
| 用户气泡 | `#95EC69` 微信绿 | `#CEC7D9` 浅薰衣草 | `BubbleUser` |
| AI 气泡 | `#F5F5F5` | `#F5F4F8` 微冷白 | `BubbleAi` |
| 错误气泡底 | `#FFF2F0` | `#F5EEEE` 暖浅红灰 | `BubbleError` |
| 置信度高 | `#07C160` | `#527A72` 松绿 | `ConfidenceHigh` |
| 置信度中 | `#FFA500` 橙 | `#C49A5C` 暖金 | `ConfidenceMedium` |
| 置信度低 | `#F5222D` 红 | `#C97A7A` 暖珊瑚红 | `ConfidenceLow` |
| 导航选中 | `#07C160` 绿 | `#7C81A4` 雾蓝 | `NavActive` |
| 导航未选中 | `#999999` | `#BFBCCB` 浅雾紫 | `NavInactive` |

### 新增辅助色

| 角色 | 色值 | 用途 |
|------|------|------|
| 强调/点缀 | `#99BFB2` 鼠尾草绿 | 进度条浅色端、渐变中间色 |
| 分割线 | `#E8E8EE` | 卡片内分割线、输入框底线 |

### 渐变定义

| 场景 | 渐变 |
|------|------|
| 支出总览卡 | `linearGradient(135°, #8B91B0 → #7C81A4 → #626788)` |
| 收入总览卡 | `linearGradient(135°, #99BFB2 → #527A72 → #496B61)` |
| 支出进度条 | `linearGradient(90°, #7C81A4 → #9CA1BE)` |
| 收入进度条 | `linearGradient(90°, #527A72 → #99BFB2)` |
| 发送按钮 | `linearGradient(135°, #8B91B0 → #7C81A4)` |

## 卡片质感规范

### 通用卡片（分类占比、最近记录、设置分组）

- 背景：`#FFFFFF`
- 圆角：`16dp`
- 阴影：`elevation 2dp`（Compose Card 默认）或手动 `shadow(color: rgba(124,129,164,.06), blur: 14dp, y: 4dp)`
- 内边距：`16dp`

### 总览卡（SummaryCard）

- 背景：渐变（见上）
- 圆角：`20dp`
- 阴影：更深层级 `rgba(124,129,164,.24)`，blur `28dp`，y `10dp`
- 内边距：`22dp × 20dp`

### 记账卡（ExpenseCard）

- 背景：`#FFFFFF`
- 圆角：`16dp`
- 左侧色条：`3dp` 宽，支出用 `#7C81A4`，收入用 `#527A72`
- 阴影：`rgba(124,129,164,.08)`，blur `16dp`，y `4dp`

### Tab 切换按钮

- 容器：白底 + `14dp` 圆角 + 轻阴影
- 选中态：渐变背景（`#8B91B0 → #7C81A4`）+ 白字
- 未选中态：透明背景 + `#BFBCCB` 字

### 进度条

- 高度：`5dp`
- 圆角：`3dp`
- 轨道色：`#E8E8EE`
- 填充：对应色系渐变

### 分割线

- 颜色：`#E8E8EE`
- 高度：`1dp`
- 左右内缩：`16dp`

## 受影响文件清单

以下文件包含硬编码色值或引用了需改色的主题变量，需逐一修改：

### 主题层（核心，改这里全局生效）

1. `app/src/main/java/com/accounting/app/ui/theme/Color.kt` — 全部色值定义
2. `app/src/main/java/com/accounting/app/ui/theme/Theme.kt` — LightColorScheme 引用

> `Type.kt` 不涉及色值，本次不改。

### 屏幕（含硬编码色值需同步）

4. `DashboardScreen.kt` — 渐变色硬编码（`#5BECA3`、`#DBEAFE`、`#93C5FD`）、进度条轨道色 `#E5E5E5`
5. `ChatScreen.kt` — 输入栏分隔线 `#E7E5E4`、TextField 指示线 `#E5E5E5`
6. `SettingsScreen.kt` — 分割线 `#EFEFEF`、TextField 指示线 `#E5E5E5`
7. `MemoryManageScreen.kt` — 引用 `WeChatGreen`、`BackgroundGray`、`TextDelete`
8. `categorymanagescreen.kt` — 引用 `WeChatGreen`、`WeChatGreenLight`、`BackgroundGray`、`TextDelete`
9. `MappingManageScreen.kt` — 引用主题色

### 组件（含硬编码色值需同步）

10. `ExpenseCard.kt` — 引用主题色，无硬编码
11. `CategoryPicker.kt` — 引用 `WeChatGreen`、`BackgroundGray`
12. `CategorySelector.kt` — 硬编码 `Color(0xFF07C160)` × 2 处
13. `ManualEntryDialog.kt` — 引用 `WeChatGreen`、`BackgroundGray`
14. `PlanPreviewCard.kt` — 硬编码 `#F5F5F5`、`#EFEFEF`、`#F5A623`、`#E53935`，引用 `WeChatGreen`、`TextAmount`
15. `RuleSuggestionDialog.kt` — 引用主题色

### 入口

16. `MainActivity.kt` — 引用 `NavActive`、`NavInactive`

## 改色策略

### 策略：变量名不变，改值不改名

所有主题变量名保持不变（`WeChatGreen`、`TextAmount` 等），只改色值。这样：

- 引用主题变量的文件**无需改动**，自动继承新色值
- 只有**硬编码色值**（如 `Color(0xFF07C160)`、`Color(0xFFE5E5E5)`）的文件需要逐一修改
- 改动最小化，回归风险最低

### 硬编码色值替换清单

| 硬编码值 | 出现位置 | 替换为 |
|----------|----------|--------|
| `Color(0xFF07C160)` | CategorySelector.kt ×2 | `WeChatGreen`（引用变量） |
| `Color(0xFFE5E5E5)` | ChatScreen、SettingsScreen、categorymanagescreen | `#E8E8EE` |
| `Color(0xFFE7E5E4)` | ChatScreen 输入栏分隔线 | `#E8E8EE` |
| `Color(0xFFEFEFEF)` | SettingsScreen、PlanPreviewCard 分割线 | `#E8E8EE` |
| `Color(0xFFF5F5F5)` | PlanPreviewCard 按钮底色 | `BackgroundGray`（引用变量） |
| `Color(0xFF5BECA3)` | DashboardScreen 支出渐变中间色 | `#99BFB2` |
| `Color(0xFFDBEAFE)` | DashboardScreen 收入渐变浅色 | `#E8F0ED` |
| `Color(0xFF93C5FD)` | DashboardScreen 收入渐变中间色 | `#99BFB2` |
| `Color(0xFFF5A623)` | PlanPreviewCard UPDATE 色 | `#C49A5C` |
| `Color(0xFFE53935)` | PlanPreviewCard DELETE 色 | `#C97A7A` |
| `Color(0xFF666666)` | DashboardScreen Tab 未选中字色 | `#BFBCCB`（或引用 `TextSecondary`） |

## 不改动范围

- 功能逻辑：不改
- 布局结构：不改（间距、排列、组件层级保持不变）
- 字体大小/字重：不改（`Type.kt` 保持不变，新色调下现有字重依然合适）
- 圆角值：现有 `8dp`/`12dp`/`14dp`/`16dp` 保持不变，仅总览卡从 `16dp` 提升到 `20dp`
- `themes.xml`（XML 主题）：不改动，仅 Compose 层

## 验证标准

1. 三个主页面（记账、统计、设置）视觉与新配色一致
2. 所有硬编码的微信绿 `#07C160` 已消除
3. 所有硬编码的冷蓝 `#1677FF` 已消除
4. 渐变、阴影、圆角符合规范
5. 弹窗（手动记账、分类选择、API Key、删除确认）配色一致
6. 无功能回归（所有交互正常工作）
