# Checklist

- [x] 首页已重构为极简「工作台首页」：自上而下为 今日打卡英雄卡 → 核心入口区（待办/记录）→ 「应用」区 5 个入口卡
- [x] 首页顶栏仅保留标题 + 菜单按钮（onOpenDrawer），AI记忆 / 设置 顶栏图标已删除
- [x] 首页金句卡、成长模块进度区、今日学习计划区、记账月支出卡、AI记忆卡均已移除，且相关私有 Composable（DailyQuoteCard、ModuleProgressSection、TodayPlanSection、AccountingCard、MemoryQuickCard）已删除、无用 import 已清理
- [x] DashboardScreen 函数签名已按 spec 调整：新增 onGrowthClick/onCreatorClick/onFitnessClick/onEnglishClick/onProgrammingClick，保留 onOpenDrawer/onGoalListClick/onTaskClick/onAccountingClick，移除 onMemoryClick/onSettingsClick/monthlyExpense
- [x] 今日打卡核心能力保留：成长值英雄卡与今日任务摘要可见，打卡数据未丢失
- [x] 5 个应用入口接通：记账 → 独立记账 App（startActivity 方式）；自媒体 → creator 创作工作台；健身 → growth 记录页预选 FITNESS 分类；英语 / 编程 → goal_list 学习目标页
- [x] GrowthScreen 支持 initialCategory 参数（默认 ALL），健身直达时按 FITNESS 过滤且行为正确
- [x] AIGrowthOSApp.kt 调用处已按新签名传参并接线，SideNavigationDrawer 打卡/待办/记录/设置 四项导航保持正常
- [x] SettingsScreen 已补齐 onOpenDrawer 参数与顶栏菜单按钮（此前交接改动），编译无缺参错误
- [x] 5 个应用入口点击均打印 AppLogger 入口/出口日志（requestId + node），无裸 Log
- [x] 宪法合规：无 fallbackToDestructiveMigration（仅 core/database 既有一处，非本次新增）、无直写 DAO 绕过 PlanExecutor（本次无 DB 变更，代码走查确认）
- [x] build.gradle.kts 版本号升级：versionCode 6、versionName 1.5
- [x] `./gradlew.bat assembleDebug` 编译通过，APK 产出 `工作台_v1.5_debug.apk`
- [ ] 手动验收：打开工作台首屏简洁、侧边栏菜单可开、五个应用入口均可达正确目标（需安装 APK 真机确认）