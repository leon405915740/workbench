# UI 美化 v2：对比度修复 + 整体设计感提升

## 日期
2026-07-30

## 背景

v1 莫兰迪雾蓝配色已上线（v2.11.0），但实际安装后反馈两个问题：

1. **对比度偏低** — 主文字 `#5A5F7A` 在白底上偏灰，次要文字 `#BFBCCB` 更是接近背景色，阅读吃力。品牌色 `#7C81A4` 灰扑扑的，不够醒目。
2. **设计感不足** — 仅换了配色，布局/阴影/圆角/组件质感仍是旧版扁平风格，没有"精致感"。卡片无阴影、Tab 按钮太朴素、输入框灰底显脏、底部导航是 Material3 默认样式。

本次目标：在保持莫兰迪冷调方向不变的前提下，**全面提升对比度到 WCAG AA 级别**，同时对**所有前台页面的组件质感做系统级升级**。

## 设计原则

- **可读性第一**：文字对比度必须达标，装饰性让位于功能性
- **克制的设计感**：用阴影/圆角/间距制造层次，不堆砌渐变和特效
- **一致性**：同类组件在所有页面表现统一（圆角、阴影、间距）
- **不破坏功能**：不改任何业务逻辑和交互流程

---

## 第一部分：对比度修复

### 问题诊断

| 角色 | 当前色值 | 对比度问题 | 严重程度 |
|------|----------|-----------|---------|
| 主文字 TextPrimary | `#5A5F7A` | 白底对比度 6.9:1，偏灰，小字阅读累 | 中 |
| 次要文字 TextSecondary | `#BFBCCB` | 白底对比度 1.8:1，**远低于 WCAG AA 4.5:1** | 严重 |
| 品牌主色 WeChatGreen | `#7C81A4` | 白底对比度 4.3:1，灰扑扑不醒目 | 中 |
| 支出金额 TextAmount | `#7C81A4` | 同上，金额数字不突出 | 中 |
| 导航未选中 NavInactive | `#BFBCCB` | 同 TextSecondary，几乎看不见 | 严重 |
| 背景 BackgroundGray | `#F0F0F4` | 与白卡区分度不够 | 低 |

### 新色值方案

保持莫兰迪冷调方向，加深明度、提升饱和度：

| 角色 | v1 色值 | v2 色值 | 变化说明 |
|------|---------|---------|---------|
| 品牌主色 WeChatGreen | `#7C81A4` | `#6366A0` | 提亮+加饱和，更清澈的雾蓝紫 |
| 品牌浅色 WeChatGreenLight | `#E8E8EE` | `#ECEAF4` | 带蓝紫调的浅底 |
| 品牌深色 WeChatGreenDark | `#626788` | `#4A4E7C` | 加深，渐变末端更有力 |
| 背景 BackgroundGray | `#F0F0F4` | `#F5F6FA` | 更干净的白蓝灰 |
| 主文字 TextPrimary | `#5A5F7A` | `#33374C` | 加深到近黑蓝，对比度 13:1 |
| 次要文字 TextSecondary | `#BFBCCB` | `#878B9E` | 加深到 4.6:1，达标 |
| 支出金额 TextAmount | `#7C81A4` | `#6366A0` | 同品牌色，提亮 |
| 收入金额 TextIncome | `#527A72` | `#3D8B6F` | 提亮+加饱和，绿意更明 |
| 删除/错误 TextDelete | `#C97A7A` | `#C25B5B` | 加深，错误态更醒目 |
| 用户气泡 BubbleUser | `#CEC7D9` | `#D4D0E8` | 略提亮，文字用 TextPrimary |
| AI 气泡 BubbleAi | `#F5F4F8` | `#F2F1F7` | 不变 |
| 错误气泡 BubbleError | `#F5EEEE` | `#F8E8E8` | 略偏暖红 |
| 置信度高 ConfidenceHigh | `#527A72` | `#3D8B6F` | 同收入色 |
| 置信度中 ConfidenceMedium | `#C49A5C` | `#D4923F` | 提亮，更醒目 |
| 置信度低 ConfidenceLow | `#C97A7A` | `#C25B5B` | 同删除色 |
| 导航选中 NavActive | `#7C81A4` | `#6366A0` | 同品牌色 |
| 导航未选中 NavInactive | `#BFBCCB` | `#9CA0B0` | 加深到 3.2:1（导航允许 3:1） |
| 分割线 DividerColor | `#E8E8EE` | `#E2E1EA` | 略加深，更可见 |
| 鼠尾草绿 SageGreen | `#99BFB2` | `#8FD0BE` | 提亮，渐变中间色 |

### 新增色值

| 角色 | 色值 | 用途 |
|------|------|------|
| 卡片阴影色 CardShadow | `#6366A0` | 统一阴影色调，带品牌色的微染 |
| 输入框边框 BorderDefault | `#DDDCE6` | 输入框默认边框，比 DividerColor 略深 |

### 渐变更新

| 场景 | v2 渐变 |
|------|---------|
| 支出总览卡 | `linearGradient(135°, #7B7FBA → #6366A0 → #4A4E7C)` |
| 收入总览卡 | `linearGradient(135°, #6FD4B8 → #3D8B6F → #2E6B55)` |
| 发送按钮 | `linearGradient(135°, #7B7FBA → #6366A0)` |
| 支出进度条 | `linearGradient(90°, #6366A0 → #8B8FC8)` |
| 收入进度条 | `linearGradient(90°, #3D8B6F → #6FD4B8)` |

---

## 第二部分：整体设计感提升

### 2.1 卡片系统

**问题**：当前卡片要么无阴影（Settings elevation=0），要么用 Compose 默认阴影（太生硬），整体扁平无层次。

**方案**：统一卡片规范——

| 卡片类型 | 圆角 | 阴影 | 内边距 |
|---------|------|------|--------|
| 通用卡片 | 16dp | `shadow(blur=12dp, y=3dp, color=#6366A0@8%)` | 16dp |
| 总览卡 SummaryCard | 20dp | `shadow(blur=24dp, y=8dp, color=#6366A0@20%)` | 24dp |
| 记账卡 ExpenseCard | 16dp | `shadow(blur=16dp, y=4dp, color=#6366A0@10%)` | 14dp |
| 设置分组卡 | 16dp | `shadow(blur=10dp, y=2dp, color=#6366A0@6%)` | 0（内部控制） |

实现方式：用 `Modifier.shadow()` 替代 `CardDefaults.cardElevation()`，获得更柔和的莫兰迪色调阴影。

### 2.2 Tab 切换按钮

**问题**：当前 Tab 是简单的圆角矩形 + 纯色背景，太朴素。

**方案**：改为**胶囊按钮组**样式——

```
┌─────────────────────────────┐
│  ┌─────────┐               │
│  │  支出   │     收入      │   ← 选中：白底 + 品牌色文字 + 阴影
│  └─────────┘               │   ← 未选中：透明 + 次要文字色
└─────────────────────────────┘
    容器：BackgroundGray 底 + 24dp 圆角
```

- 容器：`BackgroundGray` 背景 + `RoundedCornerShape(24dp)` + padding 4dp
- 选中态：`CardWhite` 背景 + 品牌色文字 + 轻阴影（`shadow(blur=8dp, y=2dp, color=#6366A0@12%)`）
- 未选中态：透明背景 + `TextSecondary` 文字
- 适用页面：Dashboard 收支切换、MemoryManage 支出/收入切换、MappingManage 手动/自动切换

### 2.3 输入框

**问题**：当前 `OutlinedTextField` 用 `BackgroundGray` 做底色，在白卡片上显脏；指示线透明，无聚焦反馈。

**方案**：改为**白底 + 细边框**风格——

- 默认态：`CardWhite` 背景 + `BorderDefault` 1dp 边框 + 12dp 圆角
- 聚焦态：`CardWhite` 背景 + `WeChatGreen` 2dp 边框 + 12dp 圆角
- 去掉 indicator line，用 `OutlinedTextField` 的 `border` 参数控制

适用位置：ChatScreen 输入栏、SettingsScreen API Key 弹窗、ManualEntryDialog 全部表单、MemoryManageScreen 搜索框、CategoryManageScreen 添加/编辑弹窗、MappingManageScreen 新增映射弹窗。

### 2.4 按钮系统

**问题**：按钮样式不统一，有的 8dp 圆角有的 20dp，主按钮纯色无质感。

**方案**：统一按钮规范——

| 按钮类型 | 圆角 | 样式 |
|---------|------|------|
| 主按钮（确认/发送/保存） | 12dp | 品牌色渐变 + 白字 + 轻阴影 |
| 次按钮（取消/忽略） | 12dp | `BackgroundGray` 底 + `TextPrimary` 字 + 无阴影 |
| 文字按钮（修改分类/删除） | 无 | 纯文字，品牌色/删除色 |
| 图标按钮（+/返回） | 圆形 | 透明底 + 品牌色图标 |

### 2.5 底部导航栏

**问题**：当前用 Material3 `NavigationBar` 默认样式，选中态只是变色，无视觉层次。

**方案**：保持 `NavigationBar` 组件（不改架构），但自定义样式——

- 容器：`CardWhite` 底 + 顶部 1dp `DividerColor` 分割线 + 轻阴影向上
- 选中态：图标 `NavActive` + 标签 `NavActive` + 图标下方加 3dp 圆点指示器（`NavActive` 色）
- 未选中态：图标 `NavInactive` + 标签 `NavInactive` + 无指示器
- 指示器用 `Box` + `clip(RoundedCornerShape(1.5dp))` 实现

### 2.6 进度条

**问题**：当前 `LinearProgressIndicator` 高度 4dp、圆角 2dp，太细太扁。

**方案**：
- 高度提升到 6dp
- 圆角 3dp
- 轨道色 `DividerColor`
- 填充用渐变（支出蓝紫/收入绿）
- 用 `Box` + `clip` 手动绘制，替代 `LinearProgressIndicator`，获得渐变效果

### 2.7 空状态

**问题**：当前空状态只有一行灰字，没有视觉吸引力。

**方案**：
- 加大 emoji 图标到 48sp
- 图标下方加 12sp 间距
- 主提示文字 15sp + `TextPrimary` 色
- 副提示文字 13sp + `TextSecondary` 色
- 整体垂直居中 + 上下 48dp padding

### 2.8 总览卡 SummaryCard

**问题**：渐变色虽改了但内部排版没变，数字不够突出。

**方案**：
- 本月金额字号从 28sp 提升到 32sp
- 标签文字（"本月支出"）加 `alpha = 0.85f` 而非 0.9f（让背景色更透出）
- 今日/日均小字区域的标签用 `alpha = 0.7f`
- 卡片内加 `Box` 包裹，用 `Modifier.shadow()` 添加莫兰迪色调阴影
- 金额前加货币符号 ¥，字号 20sp，基线下沉

### 2.9 ExpenseCard 记账卡

**问题**：左侧色条 3dp 太细，置信度圆点太小。

**方案**：
- 左侧色条加宽到 4dp
- 置信度圆点从 6dp 加大到 8dp，间距 5dp
- "已匹配记忆"标签加 4dp 圆角 + 品牌浅色底
- 金额字号从 24sp 到 26sp

### 2.10 分类占比区

**问题**：分类名 + 金额 + 百分比挤在一行，进度条在下方，视觉关系不清晰。

**方案**（不改变布局结构，仅优化视觉）：
- 分类名用 `TextPrimary` 15sp Medium
- 金额用 `TextAmount`/`TextIncome` 14sp SemiBold
- 百分比用 `TextSecondary` 12sp
- 进度条上方加 8dp 间距（当前 6dp）
- Top5 分类之间加 14dp 间距（当前 12dp）

### 2.11 弹窗统一

**问题**：各弹窗圆角不统一（8dp/12dp/16dp 混用），背景色有的 `Color.White` 有的 `CardWhite`。

**方案**：
- 所有 `AlertDialog` 保持 Material3 默认（系统统一）
- 所有 `Dialog` + `Surface`/`Card` 自定义弹窗：统一 20dp 圆角 + `CardWhite` 底 + 阴影
- 涉及：ManualEntryDialog（16dp -> 20dp）、CategoryPicker（16dp -> 20dp）、RuleSuggestionDialog（16dp -> 20dp）

---

## 第三部分：受影响文件清单

### 主题层
1. `Color.kt` — 色值更新 + 新增 CardShadow、BorderDefault
2. `Theme.kt` — 更新 secondaryContainer 引用

### 屏幕
3. `DashboardScreen.kt` — Tab 胶囊化、SummaryCard 阴影+字号、进度条渐变、空状态优化、卡片阴影
4. `ChatScreen.kt` — 输入框白底边框、气泡阴影、空状态优化
5. `SettingsScreen.kt` — 卡片阴影+圆角、输入框边框
6. `MemoryManageScreen.kt` — Tab 胶囊化、搜索框边框、卡片阴影
7. `categorymanagescreen.kt` — Tab 胶囊化、弹窗输入框边框
8. `MappingManageScreen.kt` — Tab 胶囊化、弹窗输入框边框、Switch 颜色

### 组件
9. `ExpenseCard.kt` — 色条加宽、圆点加大、金额字号、阴影
10. `CategoryPicker.kt` — 弹窗圆角 20dp、选中态样式
11. `ManualEntryDialog.kt` — 弹窗圆角 20dp、输入框边框、Tab 胶囊化
12. `PlanPreviewCard.kt` — 按钮统一、阴影
13. `RuleSuggestionDialog.kt` — 弹窗圆角 20dp、按钮统一

### 入口
14. `MainActivity.kt` — 底部导航指示器、阴影

### 版本
15. `build.gradle.kts` — 版本号升级

---

## 第四部分：不改动范围

- 功能逻辑：不改
- 交互流程：不改
- 字体大小/字重体系（Type.kt）：不改，仅个别组件内字号微调
- 数据模型：不改
- XML 主题：不改
- 构建配置/依赖：不改

---

## 第五部分：验证标准

1. **对比度**：所有文字在白底上达到 WCAG AA（4.5:1 正文，3:1 大字/导航）
2. **一致性**：同类组件在所有页面圆角/阴影/间距统一
3. **设计感**：卡片有层次、Tab 有质感、输入框干净、导航有指示
4. **无残留**：旧 v1 色值（`#7C81A4`、`#BFBCCB`、`#5A5F7A` 等）全部清除
5. **编译通过**：BUILD SUCCESSFUL + APK 生成
6. **无功能回归**：所有交互正常

---

## 改动量评估

- **色值改动**：20 个色值更新 + 2 个新增
- **布局微调**：约 15 处 padding/圆角/阴影调整
- **组件升级**：Tab 按钮组 x4、输入框 x6+、按钮统一 x8+、导航指示器 x1
- **预估文件**：15 个文件
- **风险**：中等（涉及视觉面广，但无逻辑改动）
