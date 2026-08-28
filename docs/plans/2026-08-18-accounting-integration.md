# 记账模块整合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将记账模块整合进 AI-Growth-OS 工作台，删除自动捕获功能，合并记忆/映射管理页，重建设置页，首页添加记账入口卡片。

**Architecture:** 通过 AccountingBridge 接口实现跨模块数据访问，工作台不直接访问记账模块 DAO/Repository。UI 使用 UiModel 解耦数据库 Entity。合并页通过独立 ViewModel + Bridge 获取数据。

**Tech Stack:** Kotlin + Jetpack Compose + Room + DataStore + Navigation Compose + Hilt

---

## 已完成步骤

- [x] Step 1: 修复 MainActivity.kt 编译错误（删除 capture 引用 + SETTINGS tab）
- [x] Step 2: 清理 app AndroidManifest.xml 权限（删除 FOREGROUND_SERVICE + POST_NOTIFICATIONS）

---

## Task 1: Capture 残留引用检查 + 首次编译验证

**Files:**
- 全项目 `.kt` 文件 grep 扫描
- 编译验证

- [ ] **Step 1: 全项目 grep 检查 capture 残留引用**

在 `d:\记账app\工作台app\AI-Growth-OS` 下执行：

```powershell
Select-String -Path "feature\accounting\src\main\java\com\accounting\app\**\*.kt" -Pattern "PaymentInfo|PaymentAccessibilityService|CapturePreferences|CaptureNotificationManager|autoCapture|EXTRA_PAYMENT_INFO" -Recurse | Select-Object Filename, LineNumber, Line
```

期望：无匹配结果（BillExecutePlan 除外，它仍在 AI Planner 中使用）。

- [ ] **Step 2: 编译验证**

```powershell
cd d:\记账app\工作台app\AI-Growth-OS
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
.\gradlew.bat assembleDebug --no-daemon 2>&1 | Out-File build_log3.txt
```

期望：BUILD SUCCESSFUL。如果失败，修复所有残留错误后重新编译。

- [ ] **Step 3: 如有编译错误，修复后重新验证**

常见问题：
- 未删除的 import 引用
- BuildConfig 字段缺失
- Room 版本不匹配

---

## Task 2: 创建 AccountingBridge 接口 + UI Models

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/AccountingBridge.kt`
- Create: `feature/accounting/src/main/java/com/accounting/app/ui/model/MemoryItemUi.kt`
- Create: `feature/accounting/src/main/java/com/accounting/app/ui/model/MappingItemUi.kt`

- [ ] **Step 1: 创建 MemoryItemUi 数据类**

```kotlin
// feature/accounting/src/main/java/com/accounting/app/ui/model/MemoryItemUi.kt
package com.accounting.app.ui.model

data class MemoryItemUi(
    val id: Long,
    val triggerWord: String,
    val category: String,
    val subcategory: String?,
    val type: String,
    val source: String
)
```

- [ ] **Step 2: 创建 MappingItemUi 数据类**

```kotlin
// feature/accounting/src/main/java/com/accounting/app/ui/model/MappingItemUi.kt
package com.accounting.app.ui.model

data class MappingItemUi(
    val id: Long,
    val keyword: String,
    val categoryName: String,
    val subcategoryName: String?,
    val type: String,
    val isManual: Boolean,
    val isEnabled: Boolean,
    val hitCount: Int
)
```

- [ ] **Step 3: 创建 AccountingBridge 接口**

```kotlin
// feature/accounting/src/main/java/com/accounting/app/AccountingBridge.kt
package com.accounting.app

import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.model.MappingItemUi
import kotlinx.coroutines.flow.Flow

interface AccountingBridge {
    // Dashboard
    fun getMonthlyExpense(): Flow<Double?>

    // 记账设置
    fun isAutoLearnEnabled(): Flow<Boolean>
    suspend fun setAutoLearnEnabled(enabled: Boolean)

    // 记忆管理
    fun getMemories(type: String): Flow<List<MemoryItemUi>>
    suspend fun addMemory(triggerWord: String, type: String, category: String)
    suspend fun deleteMemory(id: Long)
    suspend fun clearAllMemories()
    suspend fun restoreDefaultMemories()

    // 分类映射管理
    fun getMappingsBySource(source: String): Flow<List<MappingItemUi>>
    suspend fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?)
    suspend fun deleteMapping(id: Long)
    suspend fun toggleMappingEnabled(id: Long, enabled: Boolean)
    suspend fun promoteMappingToManual(id: Long)
    suspend fun cleanStaleAutoMappings()

    // 数据导出
    suspend fun prepareCsvExport(): String?
    suspend fun prepareLogExport(): String?

    // 分类列表（供映射管理页 UI 使用，id → 名称 对）
    fun getExpenseCategories(): List<Pair<String, Long>>
    fun getIncomeCategories(): List<Pair<String, Long>>
}
```

- [ ] **Step 4: 编译验证（模块级）**

```powershell
.\gradlew.bat :feature:accounting:compileDebugKotlin --no-daemon
```

期望：编译通过，接口和 UI Models 无语法错误。

---

## Task 3: 创建 AccountingBridgeImpl + 在 AccountingApp 中暴露

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/AccountingBridgeImpl.kt`
- Modify: `feature/accounting/src/main/java/com/accounting/app/AccountingApp.kt`

- [ ] **Step 1: 创建 AccountingBridgeImpl**

```kotlin
// feature/accounting/src/main/java/com/accounting/app/AccountingBridgeImpl.kt
package com.accounting.app

import android.content.Context
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.log.AppLogger
import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.util.CategoryConstants
import com.accounting.app.util.CsvUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class AccountingBridgeImpl(
    private val repo: AppRepository,
    private val context: Context
) : AccountingBridge {

    private val requestId = "bridge"

    // ===== Dashboard =====

    override fun getMonthlyExpense(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        val end = System.currentTimeMillis()

        return repo.getExpenseSum(start, end).map { cents ->
            cents?.let { it / 100.0 }
        }
    }

    // ===== 记账设置 =====

    override fun isAutoLearnEnabled(): Flow<Boolean> =
        repo.getUserPreferences().getAutoLearn()

    override suspend fun setAutoLearnEnabled(enabled: Boolean) {
        repo.getUserPreferences().setAutoLearn(enabled)
    }

    // ===== 记忆管理 =====

    override fun getMemories(type: String): Flow<List<MemoryItemUi>> =
        repo.getAllMemoriesByType(type).map { entities ->
            entities.map { it.toUiModel() }
        }

    override suspend fun addMemory(triggerWord: String, type: String, category: String) {
        val memory = CategoryMemoryEntity(
            triggerWord = triggerWord,
            type = type,
            category = category,
            subcategory = null,
            hitCount = 0,
            source = "user",
            confidence = 1.0f,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repo.upsertMemory(memory, requestId)
    }

    override suspend fun deleteMemory(id: Long) {
        repo.deleteMemory(id, requestId)
    }

    override suspend fun clearAllMemories() {
        repo.deleteAllMemories(requestId)
    }

    override suspend fun restoreDefaultMemories() {
        repo.reseedMemories(requestId)
    }

    // ===== 分类映射管理 =====

    override fun getMappingsBySource(source: String): Flow<List<MappingItemUi>> = flow {
        val entities = repo.getMappingsBySource(source)
        emit(entities.map { it.toUiModel() })
    }

    override suspend fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?) {
        val mapping = CategoryMappingEntity(
            keyword = keyword,
            type = type,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            source = "MANUAL",
            enabled = true,
            hitCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastHitAt = 0
        )
        repo.upsertMapping(mapping, requestId)
    }

    override suspend fun deleteMapping(id: Long) {
        repo.deleteMappingById(id, requestId)
    }

    override suspend fun toggleMappingEnabled(id: Long, enabled: Boolean) {
        repo.updateMappingEnabled(id, enabled, requestId)
    }

    override suspend fun promoteMappingToManual(id: Long) {
        repo.promoteMappingToManual(id, requestId)
    }

    override suspend fun cleanStaleAutoMappings() {
        repo.cleanStaleAutoMappings(System.currentTimeMillis(), requestId)
    }

    // ===== 数据导出 =====

    override suspend fun prepareCsvExport(): String? {
        val expenses = repo.getAllExpenses().first()
        val incomes = repo.getAllIncomes().first()
        return CsvUtils.generateCsv(expenses, incomes)
    }

    override suspend fun prepareLogExport(): String? {
        return AppLogger.getMergedLogFile()?.readText()
    }

    // ===== 分类列表 =====

    override fun getExpenseCategories(): List<Pair<String, Long>> {
        return CategoryConstants.expenseCategories.mapIndexed { index, name ->
            name to (index + 1).toLong()
        }
    }

    override fun getIncomeCategories(): List<Pair<String, Long>> {
        val offset = CategoryConstants.expenseCategories.size
        return CategoryConstants.incomeCategories.mapIndexed { index, name ->
            name to (offset + index + 1).toLong()
        }
    }

    // ===== Entity → UiModel 转换 =====

    private fun CategoryMemoryEntity.toUiModel() = MemoryItemUi(
        id = id,
        triggerWord = triggerWord,
        category = category,
        subcategory = subcategory,
        type = type,
        source = source
    )

    private fun CategoryMappingEntity.toUiModel() = MappingItemUi(
        id = id,
        keyword = keyword,
        categoryName = resolveCategoryName(categoryId),
        subcategoryName = null,
        type = type,
        isManual = source == "MANUAL",
        isEnabled = enabled,
        hitCount = hitCount
    )

    private fun resolveCategoryName(categoryId: Long): String {
        val expense = CategoryConstants.expenseCategories
        val income = CategoryConstants.incomeCategories
        return when {
            categoryId in 1..expense.size -> expense[(categoryId - 1).toInt()]
            categoryId in (expense.size + 1)..(expense.size + income.size) -> 
                income[(categoryId - expense.size - 1).toInt()]
            else -> "其他"
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.first(): T {
        var result: T? = null
        kotlinx.coroutines.flow.first(this).also { result = it }
        return result!!
    }
}
```

**注意：** 上面的 `first()` 扩展函数是冗余的 — 直接使用 `kotlinx.coroutines.flow.first()` 即可。实际实现中删除该扩展函数，直接 import `kotlinx.coroutines.flow.first`。

- [ ] **Step 2: 修改 AccountingApp.kt 暴露 Bridge**

在 `AccountingApp` companion object 中添加：

```kotlin
@Volatile
private var bridge: AccountingBridge? = null

fun getBridge(): AccountingBridge {
    return bridge ?: synchronized(this) {
        bridge ?: AccountingBridgeImpl(
            instance!!.appRepository,
            instance!!.applicationContext
        ).also { bridge = it }
    }
}
```

需要添加 import：
```kotlin
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.model.MappingItemUi
import kotlinx.coroutines.flow.Flow
```

- [ ] **Step 3: 编译验证**

```powershell
.\gradlew.bat :feature:accounting:compileDebugKotlin --no-daemon
```

期望：编译通过。

---

## Task 4: 合并 MemoryManageScreen + MappingManageScreen

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MemoryMappingManageScreen.kt`
- Delete: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt`
- Delete: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt`

这是工作量最大的步骤（~800-1000 行代码移植）。

- [ ] **Step 1: 读取原始文件完整内容**

读取以下两个文件的完整内容，提取所有 Composable 函数和 UI 逻辑：
- `MemoryManageScreen.kt`（约 500+ 行）
- `MappingManageScreen.kt`（约 500+ 行）

- [ ] **Step 2: 创建合并屏幕文件**

创建 `MemoryMappingManageScreen.kt`，包含：
- 外层 `Scaffold` + `TopAppBar`（标题："记忆与分类管理"）
- `TabRow` 两个 Tab：「分类记忆」「分类映射」
- Tab 1 移植 MemoryManageScreen 的核心 UI（搜索框、筛选 chips、记忆列表、添加/删除对话框）
- Tab 2 移植 MappingManageScreen 的核心 UI（手动/自动子 Tab、映射列表、添加/编辑/删除对话框）
- 参数从 `uiState.xxx` 改为独立参数（`memories: List<MemoryItemUi>`, `mappings: List<MappingItemUi>` 等）

函数签名：
```kotlin
@Composable
fun MemoryMappingManageScreen(
    memories: List<MemoryItemUi>,
    mappings: List<MappingItemUi>,
    expenseCategories: List<Pair<String, Long>>,
    incomeCategories: List<Pair<String, Long>>,
    onAddMemory: (triggerWord: String, type: String, category: String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAllMemories: () -> Unit,
    onRestoreDefaultMemories: () -> Unit,
    onAddMapping: (keyword: String, type: String, categoryId: Long, subcategoryId: Long?) -> Unit,
    onDeleteMapping: (Long) -> Unit,
    onToggleMappingEnabled: (Long, Boolean) -> Unit,
    onPromoteMappingToManual: (Long) -> Unit,
    onCleanStaleAutoMappings: () -> Unit,
    onBack: () -> Unit
)
```

- [ ] **Step 3: 删除原始文件**

确认无其他引用后删除：
- `MemoryManageScreen.kt`
- `MappingManageScreen.kt`

- [ ] **Step 4: grep 检查残留引用**

```powershell
Select-String -Path "feature\accounting\**\*.kt" -Pattern "MemoryManageScreen|MappingManageScreen" -Recurse
```

期望：只有 `MemoryMappingManageScreen` 中的引用，无旧文件名引用。

- [ ] **Step 5: 编译验证**

```powershell
.\gradlew.bat :feature:accounting:compileDebugKotlin --no-daemon
```

---

## Task 5: 重建工作台 SettingsScreen + 创建 ViewModels

**Files:**
- Modify: `app/src/main/java/com/aigrowth/os/feature/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/aigrowth/os/feature/settings/MemoryMappingViewModel.kt`
- Create: `app/src/main/java/com/aigrowth/os/DashboardAccountingViewModel.kt`

- [ ] **Step 1: 创建 MemoryMappingViewModel**

```kotlin
// app/src/main/java/com/aigrowth/os/feature/settings/MemoryMappingViewModel.kt
package com.aigrowth.os.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.ui.model.MemoryItemUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MemoryMappingViewModel : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()

    private val _memories = MutableStateFlow<List<MemoryItemUi>>(emptyList())
    val memories: StateFlow<List<MemoryItemUi>> = _memories.asStateFlow()

    private val _mappings = MutableStateFlow<List<MappingItemUi>>(emptyList())
    val mappings: StateFlow<List<MappingItemUi>> = _mappings.asStateFlow()

    private val _activeType = MutableStateFlow("expense")
    val activeType: StateFlow<String> = _activeType.asStateFlow()

    private val _mappingSource = MutableStateFlow("MANUAL")
    val mappingSource: StateFlow<String> = _mappingSource.asStateFlow()

    val expenseCategories = bridge.getExpenseCategories()
    val incomeCategories = bridge.getIncomeCategories()

    init {
        loadMemories()
        loadMappings()
    }

    fun setActiveType(type: String) {
        _activeType.value = type
        loadMemories()
    }

    fun setMappingSource(source: String) {
        _mappingSource.value = source
        loadMappings()
    }

    private fun loadMemories() {
        viewModelScope.launch {
            bridge.getMemories(_activeType.value).collect { items ->
                _memories.value = items
            }
        }
    }

    private fun loadMappings() {
        viewModelScope.launch {
            bridge.getMappingsBySource(_mappingSource.value).collect { items ->
                _mappings.value = items
            }
        }
    }

    fun addMemory(triggerWord: String, type: String, category: String) {
        viewModelScope.launch { bridge.addMemory(triggerWord, type, category) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { bridge.deleteMemory(id) }
    }

    fun clearAllMemories() {
        viewModelScope.launch { bridge.clearAllMemories() }
    }

    fun restoreDefaultMemories() {
        viewModelScope.launch { bridge.restoreDefaultMemories() }
    }

    fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?) {
        viewModelScope.launch { bridge.addMapping(keyword, type, categoryId, subcategoryId) }
    }

    fun deleteMapping(id: Long) {
        viewModelScope.launch { bridge.deleteMapping(id) }
    }

    fun toggleMappingEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { bridge.toggleMappingEnabled(id, enabled) }
    }

    fun promoteMappingToManual(id: Long) {
        viewModelScope.launch { bridge.promoteMappingToManual(id) }
    }

    fun cleanStaleAutoMappings() {
        viewModelScope.launch { bridge.cleanStaleAutoMappings() }
    }
}
```

- [ ] **Step 2: 创建 DashboardAccountingViewModel**

```kotlin
// app/src/main/java/com/aigrowth/os/DashboardAccountingViewModel.kt
package com.aigrowth.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardAccountingViewModel : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()

    private val _monthlyExpense = MutableStateFlow<Double?>(null)
    val monthlyExpense: StateFlow<Double?> = _monthlyExpense.asStateFlow()

    init {
        viewModelScope.launch {
            bridge.getMonthlyExpense().collect { amount ->
                _monthlyExpense.value = amount
            }
        }
    }
}
```

- [ ] **Step 3: 重建 SettingsScreen 为三组卡片**

修改 `SettingsScreen.kt`，签名变更为：

```kotlin
@Composable
fun SettingsScreen(
    onNavigateToMemoryMapping: () -> Unit,
    onApiKeySaved: () -> Unit = {}
)
```

三组卡片结构：

**卡片 1 - AI 设置（保留现有逻辑）：**
- API Key 输入框
- 模型选择按钮（DeepSeek / OpenCode Zen）
- 保存按钮

**卡片 2 - 记账设置：**
- 自动学习开关（Switch，通过 Bridge 读写）
- 记忆与分类管理入口（点击跳转 MemoryMapping 页）

**卡片 3 - 数据管理：**
- CSV 导出按钮（通过 SAF 创建文件）
- 日志导出按钮（通过 SAF 创建文件）

SAF launcher 代码：
```kotlin
val csvLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/csv")
) { uri ->
    uri?.let {
        viewModelScope.launch {
            val csvData = bridge.prepareCsvExport()
            csvData?.let { data ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(data.toByteArray())
                }
            }
        }
    }
}
```

- [ ] **Step 4: 编译验证**

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

---

## Task 6: DashboardScreen 添加双列数据网格

**Files:**
- Modify: `feature/learning/src/main/java/com/aigrowth/os/feature/learning/presentation/ui/DashboardScreen.kt`

- [ ] **Step 1: 修改函数签名新增参数**

```kotlin
@Composable
fun DashboardScreen(
    onGoalListClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onTaskListClick: () -> Unit,
    onAccountingClick: () -> Unit = {},           // 新增
    monthlyExpense: StateFlow<Double?> = MutableStateFlow(null),  // 新增
    viewModel: DashboardViewModel = hiltViewModel()
)
```

需要添加 import：
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
```

- [ ] **Step 2: 在 LazyColumn 中插入双列网格**

在 `TodayStatsRow` 之后、`MorandiSectionHeader("成长模块")` 之前插入：

```kotlin
// 3.5 记账 + 记忆 双列网格
item {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccountingCard(
            monthlyExpense = monthlyExpense.collectAsState().value,
            onClick = onAccountingClick,
            modifier = Modifier.weight(1f)
        )
        MemoryQuickCard(
            onClick = onMemoryClick,
            modifier = Modifier.weight(1f)
        )
    }
}
```

- [ ] **Step 3: 新增 AccountingCard composable**

```kotlin
@Composable
private fun AccountingCard(
    monthlyExpense: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MorandiCard(
        onClick = onClick,
        modifier = modifier,
        radius = 12.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "本月支出",
                style = MaterialTheme.typography.labelMedium,
                color = Morandi.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = monthlyExpense?.let { "¥%.2f".format(it) } ?: "暂无数据",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "记一笔 →",
                style = MaterialTheme.typography.labelSmall,
                color = Morandi.BrandPrimary
            )
        }
    }
}
```

- [ ] **Step 4: 新增 MemoryQuickCard composable**

```kotlin
@Composable
private fun MemoryQuickCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MorandiCard(
        onClick = onClick,
        modifier = modifier,
        radius = 12.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "AI记忆",
                style = MaterialTheme.typography.labelMedium,
                color = Morandi.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "查看记忆",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "管理 →",
                style = MaterialTheme.typography.labelSmall,
                color = Morandi.BrandPrimary
            )
        }
    }
}
```

- [ ] **Step 5: 编译验证**

```powershell
.\gradlew.bat :feature:learning:compileDebugKotlin --no-daemon
```

---

## Task 7: AIGrowthOSApp 接线回调 + NavHost 路由

**Files:**
- Modify: `app/src/main/java/com/aigrowth/os/AIGrowthOSApp.kt`

- [ ] **Step 1: Screen sealed class 添加 MemoryMapping 路由**

在 `sealed class Screen` 中添加：

```kotlin
object MemoryMapping : Screen("memory_mapping")
```

- [ ] **Step 2: Dashboard composable 传入新参数**

修改 Dashboard composable：

```kotlin
composable(Screen.Dashboard.route) {
    val accountingVm: DashboardAccountingViewModel = viewModel()
    DashboardScreen(
        onGoalListClick = {
            navController.navigate(Screen.GoalList.route)
        },
        onMemoryClick = {
            navController.navigate(Screen.Memory.route)
        },
        onSettingsClick = {
            navController.navigate(Screen.Settings.route)
        },
        onTaskClick = { taskId ->
            navController.navigate("${Screen.Evaluation.route}/$taskId")
        },
        onTaskListClick = {
            navController.navigate("daily_task/local")
        },
        onAccountingClick = {
            context.startActivity(
                Intent(context, com.accounting.app.MainActivity::class.java)
            )
        },
        monthlyExpense = accountingVm.monthlyExpense
    )
}
```

需要添加 import：
```kotlin
import android.content.Intent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aigrowth.os.DashboardAccountingViewModel
```

- [ ] **Step 3: Settings composable 传入导航回调**

修改 Settings composable：

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen(
        onNavigateToMemoryMapping = { navController.navigate(Screen.MemoryMapping.route) },
        onApiKeySaved = {}
    )
}
```

- [ ] **Step 4: 新增 MemoryMapping composable**

在 NavHost 中添加：

```kotlin
composable(Screen.MemoryMapping.route) {
    val vm: MemoryMappingViewModel = viewModel()
    MemoryMappingManageScreen(
        memories = vm.memories.collectAsState().value,
        mappings = vm.mappings.collectAsState().value,
        expenseCategories = vm.expenseCategories,
        incomeCategories = vm.incomeCategories,
        onAddMemory = vm::addMemory,
        onDeleteMemory = vm::deleteMemory,
        onClearAllMemories = vm::clearAllMemories,
        onRestoreDefaultMemories = vm::restoreDefaultMemories,
        onAddMapping = vm::addMapping,
        onDeleteMapping = vm::deleteMapping,
        onToggleMappingEnabled = vm::toggleMappingEnabled,
        onPromoteMappingToManual = vm::promoteMappingToManual,
        onCleanStaleAutoMappings = vm::cleanStaleAutoMappings,
        onBack = { navController.popBackStack() }
    )
}
```

需要添加 import：
```kotlin
import com.aigrowth.os.feature.settings.MemoryMappingViewModel
import com.accounting.app.ui.screens.MemoryMappingManageScreen
```

- [ ] **Step 5: 编译验证**

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

---

## Task 8: 版本号更新 + 最终编译验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts` 中：
```kotlin
versionCode = 2
versionName = "1.1"
```

- [ ] **Step 2: 全项目编译**

```powershell
cd d:\记账app\工作台app\AI-Growth-OS
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
.\gradlew.bat assembleDebug --no-daemon 2>&1 | Out-File build_log_final.txt
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 3: 全项目 grep 验证**

```powershell
Select-String -Path "feature\accounting\**\*.kt" -Pattern "PaymentInfo|autoCapture|CapturePreferences|EXTRA_PAYMENT_INFO" -Recurse
```

期望：无匹配结果。

- [ ] **Step 4: UI 回归验收清单**

对照设计文档逐项检查：
- [ ] 记账 Tab 和统计 Tab 正常切换（无 SETTINGS）
- [ ] 工作台首页双列网格显示月支出
- [ ] 点击记账卡片跳转记账模块
- [ ] 工作台设置页三组卡片显示正常
- [ ] 自动学习开关可切换
- [ ] 记忆与分类管理页 Tab 切换正常
- [ ] 添加/删除记忆正常
- [ ] 添加/删除映射正常
- [ ] CSV 导出正常
- [ ] 日志导出正常

---

## 风险与注意事项

1. **Room 版本：** accounting 模块使用 Room 2.6.1，但项目记忆中记录"Room 2.6.1 导致启动崩溃"。如果编译通过但运行时崩溃，需降级到 2.5.2。

2. **CategoryMappingEntity.categoryId 解析：** Entity 存的是 Long 型 categoryId，不是 String 分类名。BridgeImpl 中 `resolveCategoryName()` 需要与实际数据库中的 categoryId 值匹配。实施时需验证 categoryId 的实际编码规则。

3. **映射 DAO 无 Flow 方法：** CategoryMappingDao 所有方法都是 suspend，`getMappingsBySource()` 用 `flow { emit() }` 包装，不会自动更新。用户操作后需手动刷新（重新 collect）。

4. **SAF launcher 在 SettingsScreen 中：** 原 MainActivity 中的 SAF launcher 代码已随 SETTINGS tab 删除一起清理。导出功能迁移到工作台 SettingsScreen，需在工作台侧新建 SAF launcher。

5. **BridgeImpl 的 first() 调用：** `prepareCsvExport()` 中需要 `getAllExpenses().first()` 和 `getAllIncomes().first()`，直接使用 `kotlinx.coroutines.flow.first` 包导入即可，不要自己写扩展函数。
