# Tasks

- [x] Task 1: 扩展 GrowthViewModel 状态管理
  - [x] SubTask 1.1: 新增 `TaskCategory` 枚举（ALL/STUDY/CREATION/ENGLISH/FITNESS），放在 GrowthViewModel.kt 文件顶部
  - [x] SubTask 1.2: 新增 `_selectedCategory: MutableStateFlow<TaskCategory>` 和 `selectedCategory: StateFlow<TaskCategory>`
  - [x] SubTask 1.3: 新增 `filteredRecords: StateFlow<List<GrowthRecord>>`，通过 `combine(_records, _selectedCategory)` 按 category 过滤
  - [x] SubTask 1.4: 新增 `_selectedRecordId: MutableStateFlow<String?>` 和 `selectedRecordId: StateFlow<String?>`
  - [x] SubTask 1.5: 新增 `selectedRecord: StateFlow<GrowthRecord?>`，通过 `combine(filteredRecords, _selectedRecordId)` 查找当前记录，找不到时回退到 `firstOrNull()`
  - [x] SubTask 1.6: 新增 `selectRecord(id: String)` 方法设置 `_selectedRecordId`
  - [x] SubTask 1.7: 新增 `filterByCategory(category: TaskCategory)` 方法设置 `_selectedCategory` 并重置 `_selectedRecordId` 为 null（触发 selectedRecord 自动回退到第一条）
  - [x] SubTask 1.8: 新增 `GrowthRecord.matchesCategory(category: TaskCategory)` 私有扩展函数，通过 aiSummary 关键词匹配分类

- [x] Task 2: 重构 GrowthScreen 为左右双栏布局
  - [x] SubTask 2.1: 创建 `CategoryFilterBar` Composable — 水平滚动的分类标签栏（全部/学习/创作/英语/健身），选中态高亮
  - [x] SubTask 2.2: 创建 `CompactRecordItem` Composable — 左侧列表项（日期、学习时长、任务数、掌握度，选中态左侧竖条指示器+背景高亮）
  - [x] SubTask 2.3: 创建 `TaskListPanel` Composable — 左侧面板（宽度140dp），顶部 CategoryFilterBar + 下方 LazyColumn 任务列表
  - [x] SubTask 2.4: 创建 `RecordDetailPanel` Composable — 右侧面板（fillMaxWidth），展示选中记录详情
  - [x] SubTask 2.5: 在 `GrowthScreen` 中用 `Row` 组合：`TaskListPanel(modifier.width(140.dp))` + `RecordDetailPanel(modifier.weight(1f))`
  - [x] SubTask 2.6: 左侧空列表显示空状态（图标+文案+"去添加目标"按钮）

- [x] Task 3: 实现默认选中与平滑交互
  - [x] SubTask 3.1: `LaunchedEffect(filteredRecords)` 监听列表变化，当 `selectedRecordId` 为 null 或不在列表中时，自动 `selectRecord(first.id)`
  - [x] SubTask 3.2: 右侧详情面板用 `AnimatedContent` 包裹，`targetState = selectedRecord`，切换时 fadeIn/fadeOut
  - [x] SubTask 3.3: 左侧列表项选中态：背景色 `primaryContainer` + 左侧3dp竖条 `primary` 色

- [x] Task 4: 右侧详情面板内容
  - [x] SubTask 4.1: 记录头部信息（日期、掌握度标签）
  - [x] SubTask 4.2: 成长指标卡片行（4个 StatCard：学习时长/完成任务/知识卡片/掌握度）
  - [x] SubTask 4.3: AI总结区域（显示 `record.aiSummary`，无总结时显示"暂无AI总结"）
  - [x] SubTask 4.4: 成长趋势迷你图表 — 使用 Compose Canvas 绘制最近7天折线图，Y值 = learningMinutes + tasksCompleted * 10；无数据显示"暂无趋势数据"
  - [x] SubTask 4.5: 底部快捷操作栏 — 三个按钮："开始考核"(onEvaluationClick)、"查看卡片"(onKnowledgeCardClick)、"AI复盘"(触发 viewModel.generateGrowthReview)

- [x] Task 5: 空状态与未选中处理
  - [x] SubTask 5.1: 左侧无记录时显示空状态（图标+文案+"去添加目标"按钮，点击导航到目标页）
  - [x] SubTask 5.2: 右侧 `selectedRecord` 为 null 时显示占位提示（图标+"选择左侧记录查看详情"）
  - [x] SubTask 5.3: 右侧筛选分类无记录时显示"该分类暂无记录"文案

- [x] Task 6: 更新导航传参
  - [x] SubTask 6.1: AIGrowthOSApp.kt 中 GrowthScreen 添加 `onEvaluationClick` 和 `onKnowledgeCardClick` 参数，导航到对应路由
  - [x] SubTask 6.2: GrowthScreen 函数签名从 `GrowthScreen()` 改为 `GrowthScreen(onEvaluationClick: (String) -> Unit, onKnowledgeCardClick: (String) -> Unit)`

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
- [Task 4] depends on [Task 2]
- [Task 5] depends on [Task 2]
- [Task 6] depends on [Task 2]
