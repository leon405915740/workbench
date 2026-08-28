# 成长模块交互重构 Spec

## Why
当前成长模块（GrowthScreen）是单一纵向滚动列表，所有成长记录平铺展示，缺少任务分类和快捷详情查看。用户需要上下滚动才能看到完整信息，交互效率低。需要重构为类似微信的主从布局，左侧竖向任务列表 + 右侧详情面板，并优化交互逻辑。

## What Changes
- **重构成长模块布局**：从单列滚动改为左右双栏主从布局（左140dp任务列表 + 右侧详情）
- **新增 TaskCategory 枚举**：新增 UI 层分类枚举，用于任务筛选
- **新增任务分类筛选**：左侧面板顶部添加分类筛选（全部/学习/创作/英语/健身），类似微信聊天分类
- **新增任务详情面板**：右侧展示选中任务的完整详情（成长值、学习时长、知识卡片数、掌握度）
- **新增快捷操作栏**：右侧详情底部添加快捷操作按钮（开始考核/查看卡片/AI复盘）
- **优化空状态引导**：无任务时显示引导文案和CTA按钮
- **优化选中状态交互**：默认选中第一个任务，切换分类时自动选中第一个
- **BREAKING**：GrowthScreen 签名变更，新增 `onEvaluationClick: (String) -> Unit`、`onKnowledgeCardClick: (String) -> Unit` 回调参数

## Impact
- Affected specs: ai-growth-os（整体产品PRD中成长模块部分）
- Affected code:
  - `feature/learning/src/main/java/.../presentation/ui/GrowthScreen.kt` — 主要重构文件
  - `feature/learning/src/main/java/.../presentation/GrowthViewModel.kt` — 新增选中状态、分类筛选
  - `app/src/main/java/.../AIGrowthOSApp.kt` — 更新导航传参

## Data Model Definition

### TaskCategory 枚举（新增）

位置：`feature/learning/src/main/java/com/aigrowth/os/feature/learning/presentation/GrowthViewModel.kt`

```kotlin
enum class TaskCategory(val label: String) {
    ALL("全部"),
    STUDY("学习"),
    CREATION("创作"),
    ENGLISH("英语"),
    FITNESS("健身")
}
```

**说明**：这是 UI 层的筛选分类，不复用 `TaskType`（LEARNING/PRACTICE/TEST/FEYNMAN/REVIEW）。`TaskType` 是任务的功能类型，`TaskCategory` 是成长方向分类。当前 `DailyTask` 没有 category 字段，筛选逻辑通过 `title`/`description` 关键词匹配实现（学习类含"学"/"理解"，创作类含"写"/"创"，英语类含"英语"/"English"，健身类含"健身"/"运动"），后续可扩展为数据库字段。

### 状态流数据流设计

```
_records: StateFlow<List<GrowthRecord>>     // 原始数据（已有）
    ↓ combine
selectedCategory: StateFlow<TaskCategory>   // 筛选状态（新增）
    ↓ filter
filteredRecords: StateFlow<List<GrowthRecord>>  // 过滤后数据（新增）
    ↓ combine
selectedRecordId: StateFlow<String?>        // 选中ID（新增）
    ↓ lookup
selectedRecord: StateFlow<GrowthRecord?>    // 选中详情（新增）
```

**具体实现**：

```kotlin
// filteredRecords：根据分类过滤成长记录
val filteredRecords: StateFlow<List<GrowthRecord>> =
    combine(_records, _selectedCategory) { records, category ->
        if (category == TaskCategory.ALL) records
        else records.filter { it.matchesCategory(category) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

// selectedRecord：根据选中ID查找记录
val selectedRecord: StateFlow<GrowthRecord?> =
    combine(filteredRecords, _selectedRecordId) { records, id ->
        if (id == null) records.firstOrNull()
        else records.find { it.id == id } ?: records.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
```

**边界情况处理**：
- 选择任务A → 切换分类 → A不属于新分类 → `selectedRecord` 自动回退到新分类的第一条记录（通过 `?: records.firstOrNull()` 实现）
- 空分类 → `selectedRecord` 为 null → 右侧面板显示空状态

## ADDED Requirements

### Requirement: 左侧竖向任务列表
系统 SHALL 在成长模块左侧展示一个竖向滚动的成长记录列表，宽度约140dp，从上到下排列。

#### Scenario: 正常显示记录列表
- **WHEN** 用户进入成长模块
- **THEN** 左侧面板显示成长记录列表，每条记录项包含日期、学习时长、完成任务数、掌握度，选中第一条高亮

#### Scenario: 空列表状态
- **WHEN** 没有任何成长记录
- **THEN** 左侧面板显示空状态插画和"去添加目标"CTA按钮

### Requirement: 任务分类筛选
系统 SHALL 在左侧记录列表顶部提供分类筛选标签（全部/学习/创作/英语/健身）。

#### Scenario: 切换分类
- **WHEN** 用户点击某个分类标签
- **THEN** 左侧记录列表仅显示该分类下的记录，选中状态自动切换到该分类的第一条记录

#### Scenario: 切换分类后原选中记录不在新分类中
- **WHEN** 当前选中记录A，用户切换到分类X，A不属于分类X
- **THEN** 选中状态自动切换到分类X的第一条记录；若分类X无记录，右侧面板显示空状态占位

### Requirement: 右侧详情面板
系统 SHALL 在右侧面板展示当前选中记录的详细信息。

#### Scenario: 查看记录详情
- **WHEN** 用户在左侧列表选中某条记录
- **THEN** 右侧面板展示该记录的完整详情：日期、学习时长、完成任务数、知识卡片数、掌握度、AI总结

#### Scenario: 快捷操作
- **WHEN** 用户点击右侧面板底部操作按钮
- **THEN** 触发对应操作：
  - "开始考核" → 导航到考核页面（`onEvaluationClick(recordId)`）
  - "查看卡片" → 导航到知识卡片页面（`onKnowledgeCardClick(recordId)`）
  - "AI复盘" → 触发 `viewModel.generateGrowthReview()`，在面板内展示结果

### Requirement: 默认选中与平滑切换
系统 SHALL 在进入成长模块时自动选中第一条记录，切换记录时右侧详情平滑过渡。

#### Scenario: 进入成长模块
- **WHEN** 用户导航到成长模块
- **THEN** 左侧列表自动选中第一条记录，右侧面板显示该记录详情

#### Scenario: 切换记录
- **WHEN** 用户在左侧列表点击另一条记录
- **THEN** 右侧详情面板通过 `AnimatedContent` 平滑过渡到新记录详情

### Requirement: 成长趋势迷你图表
系统 SHALL 在右侧详情面板底部展示最近7天的成长趋势图。

#### Scenario: 有趋势数据
- **WHEN** 右侧面板展示记录详情，且存在最近7天的成长记录
- **THEN** 显示折线图，X轴为日期，Y轴为成长值（learningMinutes + tasksCompleted * 10），使用 Compose Canvas 绘制

#### Scenario: 无趋势数据
- **WHEN** 最近7天无成长记录
- **THEN** 显示"暂无趋势数据"文案占位

### Requirement: GrowthScreen 导航回调接口
GrowthScreen 新增以下导航回调参数：

```kotlin
@Composable
fun GrowthScreen(
    onEvaluationClick: (String) -> Unit,    // taskId -> 导航到考核页面
    onKnowledgeCardClick: (String) -> Unit, // levelId -> 导航到知识卡片页面
    viewModel: GrowthViewModel = hiltViewModel()
)
```

## MODIFIED Requirements

### Requirement: GrowthScreen 布局
从单列 LazyColumn 滚动布局改为 Row 双栏布局：左侧 Column（固定宽度140dp）+ 右侧 Column（fillMaxWidth）。

### Requirement: GrowthViewModel 状态管理
新增以下状态流和方法：

| 状态/方法 | 类型 | 说明 |
|:---|:---|:---|
| `selectedCategory` | `StateFlow<TaskCategory>` | 当前筛选分类，默认 ALL |
| `filteredRecords` | `StateFlow<List<GrowthRecord>>` | 根据分类过滤后的记录 |
| `selectedRecordId` | `StateFlow<String?>` | 选中记录ID |
| `selectedRecord` | `StateFlow<GrowthRecord?>` | 当前选中的记录详情 |
| `selectRecord(id)` | `fun(String)` | 设置选中记录 |
| `filterByCategory(cat)` | `fun(TaskCategory)` | 切换分类筛选 |

## REMOVED Requirements

### Requirement: 旧版单列滚动布局
**Reason**: 改为左右双栏布局，旧布局不再使用
**Migration**: GrowthScreen 完全重写，GrowthViewModel 扩展状态管理
