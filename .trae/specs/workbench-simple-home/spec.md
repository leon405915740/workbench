# 工作台极简首页 Spec

## Why
用户反馈"工作台太复杂、不会用"：首页是信息密集的"今日成长驾驶舱"，大量功能混杂在一起。诉求只有三点：
1. 导航用侧边栏（此项已完成，保持不变）；
2. 核心功能只保留 **打卡 / 待办 / 记录**；
3. 记账、健身、自媒体、英语、编程 **各自独立入口**，不再埋在工作台深处。

本 Spec 只做"表面极简"，不改数据层：不新增功能模块、不删数据库、不动 Room。

## What Changes
- **首页重构**：`DashboardScreen`（今日成长驾驶舱）→ 「工作台首页」简洁版：
  - 保留：顶栏标题 + 菜单按钮（开侧边栏）、今日打卡英雄卡（成长值 + 日期 + 今日任务摘要一行）
  - 新增：核心功能入口（待办 / 记录 两张大卡）、「应用」区 5 个入口卡（记账 / 健身 / 自媒体 / 英语 / 编程）
  - 移除：金句卡、成长模块进度区、今日学习计划区、记账月支出卡、AI记忆卡
- **侧边栏不变**：打卡 / 待办 / 记录 / 设置（已实现，仅保持）
- **入口接线**：`AIGrowthOSApp.kt` 按新签名传参并接线 5 个应用入口
- **健身直达**：`GrowthScreen` / `GrowthViewModel` 支持 `initialCategory`，健身入口进入记录页并预选 FITNESS 分类
- **日志合规**：5 个应用入口点击时按 Agent 宪法第 4/5/10 条打印 AppLogger 入口/出口日志（`requestId` + `node`）
- **版本号升级**：`versionCode` +1、`versionName` 升级，APK 命名遵循 `工作台_v{versionName}_debug.apk`

## Impact
- 受影响代码：
  - `feature/learning/.../ui/DashboardScreen.kt`（重构为首页）
  - `feature/learning/.../ui/GrowthScreen.kt`、`.../GrowthViewModel.kt`（`initialCategory`）
  - `app/.../AIGrowthOSApp.kt`（导航接线）
- 受影响模块：`feature/learning`、`app`
- 无数据库变更；`DashboardViewModel` 数据层保持不动（仅 UI 不再读取部分字段）

## ADDED Requirements

### Requirement: 极简首页布局
工作台首页 SHALL 自上而下展示：今日打卡英雄卡（成长值 + 日期 + 今日任务摘要一行）→ 核心入口区（待办、记录）→ 「应用」区 5 个入口卡。

#### Scenario: 打开工作台
- **WHEN** 用户打开 App 进入首页
- **THEN** 首屏只呈现上述三个区块，不再出现金句、成长模块进度、今日学习计划、记账月支出卡、AI记忆卡

#### Scenario: 顶栏
- **WHEN** 用户查看首页顶栏
- **THEN** 顶栏仅显示标题与菜单按钮，点击菜单按钮可打开侧边栏

### Requirement: 5 个独立应用入口
工作台首页「应用」区 SHALL 提供 记账 / 健身 / 自媒体 / 英语 / 编程 五个入口卡，点击直达对应目标。

#### Scenario: 记账入口
- **WHEN** 用户点击「记账」
- **THEN** 拉起独立记账 App（`com.accounting.app.MainActivity`，沿用现有 `context.startActivity` 方式）

#### Scenario: 健身入口
- **WHEN** 用户点击「健身」
- **THEN** 进入「记录」页并以 FITNESS 分类展示（`GrowthScreen(initialCategory = FITNESS)`）

#### Scenario: 自媒体入口
- **WHEN** 用户点击「自媒体」
- **THEN** 进入创作工作台（`creator` 路由，现有 `CreatorWorkbenchScreen`）

#### Scenario: 英语 / 编程入口
- **WHEN** 用户点击「英语」或「编程」
- **THEN** 进入学习目标页（`goal_list` 路由）。现阶段为直达现有学习模块入口，独立内容页/独立 App 属后续迭代，不在本次范围。

#### Scenario: 入口日志
- **WHEN** 用户点击任意应用入口
- **THEN** `AIGrowthOSApp.kt` 记录 AppLogger 入口/出口日志（`requestId` + `node`），不使用裸 `Log`

### Requirement: 入口回调签名
`DashboardScreen` 的函数签名 SHALL 调整：新增 `onGrowthClick`、`onCreatorClick`、`onFitnessClick`、`onEnglishClick`、`onProgrammingClick`；保留 `onOpenDrawer`、`onGoalListClick`、`onTaskClick`、`onAccountingClick`；移除不再使用的 `onMemoryClick`、`onSettingsClick`、`monthlyExpense`。

## MODIFIED Requirements

### Requirement: 今日打卡核心能力（原 Dashboard）
首页仍 SHALL 承担"今日打卡"职能：保留成长值英雄卡与今日数据摘要；删除入口中的冗余区块后，今日任务明细改由「待办」页承载，打卡数据不丢失。

## REMOVED Requirements

### Requirement: 首页金句 / 成长模块进度 / 今日学习计划 / 记账月支出卡 / AI记忆卡
**Reason**: 用户明确"工作台太复杂"，需极简首页；相关功能仍可分别从 待办（学习计划）、记录（成长模块）、应用入口（记账）到达，数据与页面均未删除。
**Migration**: 仅移除首页 UI 区块与不再使用的私有 Composable，数据层零改动；`DashboardViewModel` 保持原样避免连锁改动。