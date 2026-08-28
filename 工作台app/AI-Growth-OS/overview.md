# AI-Growth-OS 修复 + 底部导航栏

## 阶段一：闪退修复 (7/29)

### 关键修复 (4 项闪退根因)
1. **Application.onCreate 未捕获协程异常** — `AIGrowthOSApplication.kt` 添加 try-catch
2. **Room 缺少 fallbackToDestructiveMigration** — `DatabaseModule.kt` 添加降级迁移
3. **ApiKeyService Hilt 重复绑定** — 移除 `@Provides`，添加 `@ApplicationContext`
4. **DatabaseInitializer 数据完整性** — `KnowledgeCard(learningLevelId="")` 修正为有效 ID

### 废弃 API 修复 (~20 警告 → 0)
- `RequestBody.create()` → `toRequestBody()`
- `Divider()` → `HorizontalDivider()`
- `LinearProgressIndicator(progress=Float)` → lambda 形式
- `ArrowBack/KeyboardArrowRight/MenuBook` → AutoMirrored 版本
- `outlinedTextFieldColors()` → `OutlinedTextFieldDefaults.colors()`

---

## 阶段二：底部导航栏 (7/30)

### 新增功能
在 `AIGrowthOSApp.kt` 中添加 Material 3 `NavigationBar`，包含 5 个 Tab：

| Tab | 图标 | 路由 | 说明 |
|-----|------|------|------|
| 首页 | Home | dashboard | 今日成长驾驶舱 |
| 目标 | Flag | goal_list | 学习目标列表 |
| 成长 | TrendingUp (AutoMirrored) | growth | 成长数据中心 |
| 创作 | Create | creator | 创作工作台 |
| 设置 | Settings | settings | API Key 配置 |

### 导航行为
- **Tab 切换**：使用 `popUpTo(startDestination) { saveState = true }` + `launchSingleTop` + `restoreState = true`，保留各 Tab 页面状态
- **底栏显示**：仅在 5 个主 Tab 路由上显示，详情页（LearningPath/DailyTask/Evaluation/Memory/Feynman/GoalEdit/ResourceRecommend/WeeklyPlan）隐藏底栏
- **详情页**：保留返回按钮，push 到导航栈

### 修改文件 (7 个文件)

| 文件 | 改动 |
|------|------|
| `AIGrowthOSApp.kt` | 添加 Scaffold + NavigationBar + MainNavigationBar，Tab 导航 saveState/restoreState |
| `DashboardScreen.kt` | 移除 QuickNavSection（底栏替代），清理未用参数和函数 |
| `GoalListScreen.kt` | 移除返回键，移除多余顶栏 action（Growth/Settings 已是 Tab） |
| `GrowthScreen.kt` | 移除返回键 |
| `CreatorWorkbenchScreen.kt` | 移除返回键 |
| `SettingsScreen.kt` | 移除返回键 |

### 构建验证
```
./gradlew.bat clean assembleDebug
BUILD SUCCESSFUL
警告: 0 | 错误: 0
```
