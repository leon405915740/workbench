# Tasks

- [x] Task 1: DashboardScreen 重构为「工作台首页」（feature/learning）
  - [x] SubTask 1.1: 调整函数签名——新增 `onGrowthClick`、`onCreatorClick`、`onFitnessClick`、`onEnglishClick`、`onProgrammingClick`；保留 `onOpenDrawer`、`onGoalListClick`、`onTaskClick`、`onAccountingClick`；移除不再使用的 `onMemoryClick`、`onSettingsClick`、`monthlyExpense`
  - [x] SubTask 1.2: 顶栏只保留标题 + 菜单按钮（`onOpenDrawer`），删除 AI记忆 / 设置 顶栏图标
  - [x] SubTask 1.3: 新布局自上而下：今日打卡英雄卡（成长值环 + 日期 + 今日任务摘要一行）→ 核心入口区（待办 / 记录 两张大卡）→ 「应用」区 5 张入口卡（记账 / 健身 / 自媒体 / 英语 / 编程）
  - [x] SubTask 1.4: 删除废弃私有 Composable（DailyQuoteCard、ModuleProgressSection、TodayPlanSection、AccountingCard、MemoryQuickCard 等）与不再使用的 import；`DashboardViewModel` 数据层不动，仅 UI 不再读取 `dailyQuote` / `moduleProgresses` 等字段

- [x] Task 2: GrowthScreen 支持健身分类直达（feature/learning）
  - [x] SubTask 2.1: `GrowthScreen` 增加 `initialCategory: TaskCategory = TaskCategory.ALL` 参数
  - [x] SubTask 2.2: 页面初始化时按 `initialCategory` 调用 `filterByCategory`（LaunchedEffect 只执行一次；默认 ALL 行为不变）

- [x] Task 3: AIGrowthOSApp.kt 接线 5 个应用入口（app）
  - [x] SubTask 3.1: Dashboard 调用处按新签名传参，移除 `onMemoryClick` / `onSettingsClick` / `monthlyExpense` 相关注入
  - [x] SubTask 3.2: 接线：记账 → `context.startActivity(Intent(com.accounting.app.MainActivity))`；自媒体 → `creator` 路由；健身 → `growth` 路由携带 FITNESS 分类；英语 / 编程 → `goal_list` 路由
  - [x] SubTask 3.3: 5 个入口回调各打印 AppLogger 入口/出口日志（`requestId` + `node`，参照 SettingsScreen 现有 `com.accounting.app.log.AppLogger` 用法，禁止裸 `Log`）

- [x] Task 4: 版本号升级与编译验证
  - [x] SubTask 4.1: `app/build.gradle.kts` `versionCode` +1（5→6）、`versionName` 升级（1.4→1.5）
  - [x] SubTask 4.2: `./gradlew.bat assembleDebug` 编译通过，产出 `工作台_v1.5_debug.apk`

- [x] Task 5: 验收走查（对照 checklist 逐项验证）
  - [x] SubTask 5.1: 侧边栏 打卡/待办/记录/设置 四项导航正常、首页顶栏菜单按钮可用
  - [x] SubTask 5.2: 首页 3 核心 + 5 应用入口点击直达正确目标
  - [x] SubTask 5.3: 宪法审查：无 `fallbackToDestructiveMigration`、无裸 `Log`、无直写 DAO 绕过 PlanExecutor（本次无 DB 变更）——代码走查确认

# Task Dependencies
- [Task 2] 独立于 [Task 1]（不同文件）
- [Task 3] 依赖 [Task 1]、[Task 2]（签名就绪后才能接线）
- [Task 4] 依赖 [Task 3]
- [Task 5] 依赖 [Task 4]