# 记账模块整合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将记账模块整合进 AI-Growth-OS 工作台，删除自动捕获功能，合并记忆/映射管理页，整合设置页，添加首页记账入口卡片。

**Architecture:** AccountingBridge 接口隔离跨模块数据访问。工作台只依赖接口，不直接碰 DAO/Repository。记账模块通过 Intent 启动，设置整合到工作台 SettingsScreen。

**Tech Stack:** Kotlin + Jetpack Compose, Room, DataStore, Hilt, Navigation Compose

**当前状态：** capture/ 目录已删除，accounting SettingsScreen.kt 已删除，AccountingApp 已重构为非 Application 类，UiState 已移除 SETTINGS 和 autoCaptureEnabled。但 MainActivity.kt 和 AIGrowthOSApplication.kt 存在残留引用导致编译失败。

---

## File Structure

### 新建文件
| 文件 | 职责 |
|------|------|
| `feature/accounting/.../AccountingBridge.kt` | 跨模块数据访问接口 |
| `feature/accounting/.../AccountingBridgeImpl.kt` | 接口实现，Entity→UiModel 转换 |
| `feature/accounting/.../ui/model/MemoryItemUi.kt` | 记忆/映射 UI Model |
| `feature/accounting/.../ui/screens/MemoryMappingManageScreen.kt` | 合并后的记忆与分类管理页 |
| `app/.../DashboardAccountingViewModel.kt` | 首页月支出 ViewModel |
| `app/.../MemoryMappingViewModel.kt` | 记忆与分类管理 ViewModel |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `app/.../AIGrowthOSApplication.kt` | 改为 extends Application，调用 AccountingApp.init() |
| `feature/accounting/.../MainActivity.kt` | 移除 capture 引用 + SETTINGS tab |
| `feature/accounting/.../AccountingApp.kt` | 添加 getBridge() |
| `feature/accounting/.../data/local/pref/UserPreferences.kt` | 添加 autoLearn 持久化 |
| `feature/accounting/.../ui/MainViewModel.kt` | toggleAutoLearn 改为持久化 |
| `app/src/main/AndroidManifest.xml` | 移除 FOREGROUND_SERVICE + POST_NOTIFICATIONS |
| `app/.../feature/settings/SettingsScreen.kt` | 完全重建为三组卡片 |
| `feature/learning/.../DashboardScreen.kt` | 添加 onAccountingClick + 双列网格 |
| `app/.../AIGrowthOSApp.kt` | 接线回调 + NavHost 路由 |
| `app/build.gradle.kts` | versionCode +1 |

### 删除文件
| 文件 | 原因 |
|------|------|
| `feature/accounting/.../ui/screens/MemoryManageScreen.kt` | 合并到 MemoryMappingManageScreen |
| `feature/accounting/.../ui/screens/MappingManageScreen.kt` | 合并到 MemoryMappingManageScreen |

---

## Task 1: 修复 AIGrowthOSApplication 编译错误

**Files:**
- Modify: `app/src/main/java/com/aigrowth/os/AIGrowthOSApplication.kt`

AccountingApp 已重构为非 Application 类（有 `init()` 方法，private constructor），AIGrowthOSApplication 不能再 extends 它。

- [ ] **Step 1: 修改 AIGrowthOSApplication.kt**

将 `class AIGrowthOSApplication : AccountingApp()` 改为 `class AIGrowthOSApplication : Application()`，在 `onCreate()` 中调用 `AccountingApp.init(this)`：

```kotlin
package com.aigrowth.os

import android.app.Application
import com.accounting.app.AccountingApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIGrowthOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AccountingApp.init(this)
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/aigrowth/os/AIGrowthOSApplication.kt
git commit -m "fix: AIGrowthOSApplication 改为 extends Application 并调用 AccountingApp.init()"
```

---

## Task 2: 修复 MainActivity.kt 编译错误 — 移除 capture 引用和 SETTINGS tab

**Files:**
- Modify: `feature/accounting/src/main/java/com/accounting/app/MainActivity.kt`

MainActivity 仍引用已删除的 `PaymentInfo`、`PaymentAccessibilityService`、`SettingsScreen`，且 `AppTab.SETTINGS` 已从枚举中移除。需要清理所有残留引用。

- [ ] **Step 1: 移除 capture 相关 import 和代码**

删除以下 import 行：
```kotlin
import android.content.Intent        // 如果仅用于 capture 可删，但 SAF 也可能用，保留
import android.os.Build
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.capture.PaymentAccessibilityService
import com.accounting.app.ui.screens.SettingsScreen
import com.accounting.app.ui.screens.MemoryManageScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
```

删除 `onCreate` 中的两行：
```kotlin
viewModel.loadAutoCaptureEnabled()
handlePaymentIntent(intent)
```

删除整个 `onNewIntent` 方法和 `handlePaymentIntent` 方法。

删除 `companion object` 中的 `EXTRA_PAYMENT_INFO` 常量（如果 companion object 为空则删除整个 companion object）。

- [ ] **Step 2: 移除 SETTINGS tab 分支**

在 `MainScreen` composable 中，删除整个 `when` 分支 `AppTab.SETTINGS -> { ... }`（约 60 行，包含 SettingsScreen 调用、MemoryManageScreen 调用、无障碍服务检查逻辑）。

删除 `showMemoryManage` 状态变量：
```kotlin
var showMemoryManage by rememberSaveable { mutableStateOf(false) }  // 删除
```

删除 `onTabSelected` 中的 showMemoryManage 检查：
```kotlin
if (showMemoryManage) {
    showMemoryManage = false
}
```

- [ ] **Step 3: 移除底部导航栏的 SETTINGS 项**

在 `BottomNavBar` composable 中，从 `items` 列表删除 SETTINGS 项：
```kotlin
// 删除这行
BottomNavItem(AppTab.SETTINGS, "设置", Icons.Outlined.Settings)
```

同时删除不再使用的 import：
```kotlin
import androidx.compose.material.icons.outlined.Settings
```

- [ ] **Step 4: 提交**

```bash
git add feature/accounting/src/main/java/com/accounting/app/MainActivity.kt
git commit -m "fix: 移除 MainActivity 中 capture 残留引用和 SETTINGS tab"
```

---

## Task 3: 移除 app 模块多余权限

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 删除 FOREGROUND_SERVICE 和 POST_NOTIFICATIONS 权限**

删除这两行：
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "fix: 移除不再需要的 FOREGROUND_SERVICE 和 POST_NOTIFICATIONS 权限"
```

---

## Task 4: 全项目 capture 引用检查 + 编译验证

- [ ] **Step 1: 全项目 grep 检查**

在项目根目录执行：
```bash
grep -r "PaymentInfo" --include="*.kt" .
grep -r "PaymentAccessibilityService" --include="*.kt" --include="*.xml" .
grep -r "CapturePreferences" --include="*.kt" .
grep -r "CaptureNotificationManager" --include="*.kt" .
grep -r "autoCapture" --include="*.kt" .
grep -r "EXTRA_PAYMENT_INFO" --include="*.kt" .
grep -r "BillExecutePlan" --include="*.kt" .
grep -r "capture" --include="*.kt" . | grep -v "AccountingBridge"
```

期望：全部为空或仅出现在 AccountingBridge 相关代码中。

- [ ] **Step 2: 编译验证**

```bash
cd "d:\记账app\工作台app\AI-Growth-OS"
.\gradlew.bat assembleDebug
```

期望：BUILD SUCCESSFUL。如果失败，修复所有编译错误后重新编译。

- [ ] **Step 3: 提交（如有修复）**

```bash
git add -A
git commit -m "fix: 修复 capture 删除后的编译错误"
```

---

## Task 5: 创建 AccountingBridge 接口 + UI Models

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/AccountingBridge.kt`
- Create: `feature/accounting/src/main/java/com/accounting/app/ui/model/MemoryItemUi.kt`

- [ ] **Step 1: 创建 UI Models**

```kotlin
// feature/accounting/.../ui/model/MemoryItemUi.kt
package com.accounting.app.ui.model

data class MemoryItemUi(
    val id: Long,
    val triggerWord: String,
    val category: String,
    val type: String,       // "expense" | "income"
    val source: String     // "auto" | "seed" | "user"
)

data class MappingItemUi(
    val id: Long,
    val keyword: String,
    val category: String,
    val subcategory: String?,
    val type: String,       // "expense" | "income"
    val isManual: Boolean,
    val isEnabled: Boolean,
    val hitCount: Int
)
```

- [ ] **Step 2: 创建 AccountingBridge 接口**

```kotlin
// feature/accounting/.../AccountingBridge.kt
package com.accounting.app

import android.content.Context
import android.net.Uri
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.model.MappingItemUi
import kotlinx.coroutines.flow.Flow

interface AccountingBridge {
    // Dashboard 数据
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
    fun getMappings(tab: String): Flow<List<MappingItemUi>>
    suspend fun addMapping(keyword: String, type: String, category: String, subcategory: String?)
    suspend fun updateMapping(id: Long, keyword: String, category: String, subcategory: String?)
    suspend fun deleteMapping(id: Long)
    suspend fun toggleMappingEnabled(id: Long, enabled: Boolean)
    suspend fun promoteMappingToManual(id: Long)
    suspend fun cleanStaleAutoMappings()

    // 数据导出
    fun prepareCsvExport(): String
    fun prepareLogExport(): String

    // 分类数据
    fun getExpenseRootCategories(): List<Pair<String, Long>>
    fun getIncomeRootCategories(): List<Pair<String, Long>>
    fun getExpenseSubcategories(): Map<Long, List<Pair<String, Long>>>
    fun getIncomeSubcategories(): Map<Long, List<Pair<String, Long>>>

    // 预留：AI 记账解析（下一阶段实现）
    // suspend fun parseAccountingInput(text: String): AccountingCandidate
}
```

- [ ] **Step 3: 提交**

```bash
git add feature/accounting/src/main/java/com/accounting/app/AccountingBridge.kt feature/accounting/src/main/java/com/accounting/app/ui/model/MemoryItemUi.kt
git commit -m "feat: 创建 AccountingBridge 接口和 UI Models"
```

---

## Task 6: 创建 AccountingBridgeImpl 实现

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/AccountingBridgeImpl.kt`
- Modify: `feature/accounting/src/main/java/com/accounting/app/AccountingApp.kt`

- [ ] **Step 1: 创建 AccountingBridgeImpl**

实现所有接口方法，委托给 `appRepository`，负责 Entity → UiModel 转换。关键方法参考 `MainViewModel` 和 `AppRepository` 中的现有逻辑。

```kotlin
// feature/accounting/.../AccountingBridgeImpl.kt
package com.accounting.app

import android.content.Context
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.data.local.pref.UserPreferences
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.util.CsvUtils
import com.accounting.app.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountingBridgeImpl(
    private val repo: AppRepository,
    private val context: Context
) : AccountingBridge {

    private val userPreferences = UserPreferences(context)

    override fun getMonthlyExpense(): Flow<Double?> {
        // 参考 MainViewModel 中的月支出查询逻辑
        // 将 Long(分) 转换为 Double(元)
        return repo.expenseDao.getMonthExpenseFlow().map { cents ->
            cents?.let { it / 100.0 }
        }
    }

    override fun isAutoLearnEnabled(): Flow<Boolean> {
        return userPreferences.getAutoLearn()
    }

    override suspend fun setAutoLearnEnabled(enabled: Boolean) {
        userPreferences.setAutoLearn(enabled)
    }

    override fun getMemories(type: String): Flow<List<MemoryItemUi>> {
        return repo.categoryMemoryDao.getMemoriesByTypeFlow(type).map { entities ->
            entities.map { it.toUiModel() }
        }
    }

    override suspend fun addMemory(triggerWord: String, type: String, category: String) {
        repo.addMemory(triggerWord, type, category, "user")
    }

    override suspend fun deleteMemory(id: Long) {
        repo.categoryMemoryDao.delete(id)
    }

    override suspend fun clearAllMemories() {
        repo.clearAllMemories()
    }

    override suspend fun restoreDefaultMemories() {
        repo.restoreDefaultMemories()
    }

    override fun getMappings(tab: String): Flow<List<MappingItemUi>> {
        return repo.categoryMappingDao.getMappingsByTabFlow(tab).map { entities ->
            entities.map { it.toUiModel() }
        }
    }

    override suspend fun addMapping(keyword: String, type: String, category: String, subcategory: String?) {
        repo.addMapping(keyword, type, category, subcategory)
    }

    override suspend fun updateMapping(id: Long, keyword: String, category: String, subcategory: String?) {
        repo.updateMapping(id, keyword, category, subcategory)
    }

    override suspend fun deleteMapping(id: Long) {
        repo.categoryMappingDao.delete(id)
    }

    override suspend fun toggleMappingEnabled(id: Long, enabled: Boolean) {
        repo.categoryMappingDao.setEnabled(id, enabled)
    }

    override suspend fun promoteMappingToManual(id: Long) {
        repo.promoteMappingToManual(id)
    }

    override suspend fun cleanStaleAutoMappings() {
        repo.cleanStaleAutoMappings()
    }

    override fun prepareCsvExport(): String {
        return CsvUtils.exportToString(context, repo)
    }

    override fun prepareLogExport(): String {
        return AppLogger.exportLog()
    }

    override fun getExpenseRootCategories(): List<Pair<String, Long>> {
        return repo.getExpenseRootCategoriesSync()
    }

    override fun getIncomeRootCategories(): List<Pair<String, Long>> {
        return repo.getIncomeRootCategoriesSync()
    }

    override fun getExpenseSubcategories(): Map<Long, List<Pair<String, Long>>> {
        return repo.getExpenseSubcategoriesSync()
    }

    override fun getIncomeSubcategories(): Map<Long, List<Pair<String, Long>>> {
        return repo.getIncomeSubcategoriesSync()
    }

    private fun CategoryMemoryEntity.toUiModel() = MemoryItemUi(
        id = id,
        triggerWord = triggerWord,
        category = category,
        type = type,
        source = source
    )

    private fun CategoryMappingEntity.toUiModel() = MappingItemUi(
        id = id,
        keyword = keyword,
        category = category,
        subcategory = subcategory,
        type = type,
        isManual = source == "manual",
        isEnabled = isEnabled,
        hitCount = hitCount
    )
}
```

> **注意：** 上面的方法名（如 `getMonthExpenseFlow`、`getMemoriesByTypeFlow`、`getMappingsByTabFlow`）需要对照实际的 DAO 方法名调整。实施时先读 DAO 接口确认方法签名。

- [ ] **Step 2: 在 AccountingApp 中添加 getBridge()**

在 `AccountingApp.kt` 的 companion object 中添加：

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

- [ ] **Step 3: 提交**

```bash
git add feature/accounting/src/main/java/com/accounting/app/AccountingBridgeImpl.kt feature/accounting/src/main/java/com/accounting/app/AccountingApp.kt
git commit -m "feat: 创建 AccountingBridgeImpl 并在 AccountingApp 中暴露"
```

---

## Task 7: autoLearn 持久化迁移

**Files:**
- Modify: `feature/accounting/src/main/java/com/accounting/app/data/local/pref/UserPreferences.kt`
- Modify: `feature/accounting/src/main/java/com/accounting/app/ui/MainViewModel.kt`

- [ ] **Step 1: 在 UserPreferences 中添加 autoLearn 持久化**

```kotlin
// 在 UserPreferences.kt 中添加
companion object {
    // ... 现有 key ...
    val KEY_AUTO_LEARN = booleanPreferencesKey("auto_learn_enabled")
}

fun getAutoLearn(): Flow<Boolean> = context.userDataStore.data.map { prefs ->
    prefs[KEY_AUTO_LEARN] ?: false  // 默认关闭
}

suspend fun setAutoLearn(enabled: Boolean) {
    context.userDataStore.edit { prefs ->
        prefs[KEY_AUTO_LEARN] = enabled
    }
}
```

- [ ] **Step 2: 修改 MainViewModel.toggleAutoLearn()**

将内存态更新改为先写 DataStore 再更新 UiState：

```kotlin
fun toggleAutoLearn() {
    viewModelScope.launch {
        val newValue = !_uiState.value.autoLearnEnabled
        userPreferences.setAutoLearn(newValue)
        _uiState.update { it.copy(autoLearnEnabled = newValue) }
    }
}
```

同时在 init 或加载流程中添加 autoLearn 初始值加载：
```kotlin
// 在 ViewModel 初始化时
viewModelScope.launch {
    userPreferences.getAutoLearn().collect { enabled ->
        _uiState.update { it.copy(autoLearnEnabled = enabled) }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add feature/accounting/src/main/java/com/accounting/app/data/local/pref/UserPreferences.kt feature/accounting/src/main/java/com/accounting/app/ui/MainViewModel.kt
git commit -m "feat: autoLearn 持久化迁移到 UserPreferences DataStore"
```

---

## Task 8: 合并 MemoryManageScreen + MappingManageScreen

**Files:**
- Create: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MemoryMappingManageScreen.kt`
- Delete: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt`
- Delete: `feature/accounting/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt`

- [ ] **Step 1: 创建 MemoryMappingManageScreen**

合并两个屏幕为一个带 Tab 切换的页面。关键结构：

```kotlin
// feature/accounting/.../ui/screens/MemoryMappingManageScreen.kt
package com.accounting.app.ui.screens

// Tab 1: 分类记忆 (从 MemoryManageScreen 移植)
// Tab 2: 分类映射 (从 MappingManageScreen 移植)

@Composable
fun MemoryMappingManageScreen(
    memories: List<MemoryItemUi>,
    mappings: List<MappingItemUi>,
    expenseRootCategories: List<Pair<String, Long>>,
    incomeRootCategories: List<Pair<String, Long>>,
    expenseSubcategories: Map<Long, List<Pair<String, Long>>>,
    incomeSubcategories: Map<Long, List<Pair<String, Long>>>,
    onAddMemory: (String, String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAllMemories: () -> Unit,
    onRestoreMemories: () -> Unit,
    onSearch: (String) -> Unit,
    onSourceFilter: (String) -> Unit,
    memorySearchQuery: String,
    memorySourceFilter: String,
    onAddMapping: (String, String, String, String?) -> Unit,
    onUpdateMapping: (Long, String, String, String?) -> Unit,
    onDeleteMapping: (Long) -> Unit,
    onToggleMappingEnabled: (Long, Boolean) -> Unit,
    onPromoteMappingToManual: (Long) -> Unit,
    onCleanStaleAutoMappings: () -> Unit,
    onBack: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(0) } // 0=记忆, 1=映射

    Scaffold(
        topBar = {
            // TopAppBar with TabRow: 分类记忆 | 分类映射
        }
    ) { padding ->
        when (currentTab) {
            0 -> MemoryTabContent(memories, ...)  // 从 MemoryManageScreen 移植
            1 -> MappingTabContent(mappings, ...) // 从 MappingManageScreen 移植
        }
    }
}
```

> **实施说明：** 将 `MemoryManageScreen.kt`（460行）和 `MappingManageScreen.kt`（574行）的核心 UI 逻辑移植到新的合并文件中。两个 Tab 的内容保持原样，只是放在同一个 Composable 中用 TabRow 切换。注意参数从 `UiState` 改为独立参数（因为合并页不在 MainViewModel 内）。

- [ ] **Step 2: grep 验证旧文件引用**

```bash
grep -r "MemoryManageScreen" --include="*.kt" .
grep -r "MappingManageScreen" --include="*.kt" .
```

期望：仅出现在自身文件定义处（即将删除）。

- [ ] **Step 3: 删除旧文件**

```bash
rm feature/accounting/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt
rm feature/accounting/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: 合并 MemoryManageScreen 和 MappingManageScreen 为 MemoryMappingManageScreen"
```

---

## Task 9: 重建工作台 SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/aigrowth/os/feature/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/aigrowth/os/feature/settings/MemoryMappingViewModel.kt`

- [ ] **Step 1: 创建 MemoryMappingViewModel**

```kotlin
// app/.../feature/settings/MemoryMappingViewModel.kt
package com.aigrowth.os.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.ui.model.MemoryItemUi
import com.accounting.app.ui.model.MappingItemUi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemoryMappingViewModel : ViewModel() {
    private val bridge = AccountingApp.getBridge()

    val memories: StateFlow<List<MemoryItemUi>> =
        bridge.getMemories("expense").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mappings: StateFlow<List<MappingItemUi>> =
        bridge.getMappings("MANUAL").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseRootCategories = bridge.getExpenseRootCategories()
    val incomeRootCategories = bridge.getIncomeRootCategories()
    val expenseSubcategories = bridge.getExpenseSubcategories()
    val incomeSubcategories = bridge.getIncomeSubcategories()

    fun addMemory(triggerWord: String, type: String, category: String) {
        viewModelScope.launch { bridge.addMemory(triggerWord, type, category) }
    }
    fun deleteMemory(id: Long) { viewModelScope.launch { bridge.deleteMemory(id) } }
    fun clearAllMemories() { viewModelScope.launch { bridge.clearAllMemories() } }
    fun restoreDefaultMemories() { viewModelScope.launch { bridge.restoreDefaultMemories() } }
    fun addMapping(keyword: String, type: String, cat: String, sub: String?) {
        viewModelScope.launch { bridge.addMapping(keyword, type, cat, sub) }
    }
    fun deleteMapping(id: Long) { viewModelScope.launch { bridge.deleteMapping(id) } }
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

- [ ] **Step 2: 重建 SettingsScreen 为三组卡片**

```kotlin
// app/.../feature/settings/SettingsScreen.kt
@Composable
fun SettingsScreen(
    onNavigateToMemoryMapping: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bridge = AccountingApp.getBridge()

    // AI 设置状态（现有 SharedPreferences）
    var apiKey by remember { mutableStateOf(loadApiKey(context)) }
    var selectedModel by remember { mutableStateOf(loadModel(context)) }

    // 记账设置状态
    val autoLearn by bridge.isAutoLearnEnabled().collectAsState(initial = false)

    // SAF launchers
    val csvLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(bridge.prepareCsvExport().toByteArray())
            }
        }
    }
    val logLauncher = rememberLauncherForActivityResult(CreateDocument("text/plain")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(bridge.prepareLogExport().toByteArray())
            }
        }
    }

    Scaffold(topBar = { /* TopAppBar "设置" */ }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            // Section 1: AI 设置
            item { SettingsGroupCard("🤖 AI 设置") {
                SettingItem("DeepSeek API Key", apiKey.masked()) { /* show dialog */ }
                SettingItem("AI 模型", selectedModel) { /* picker */ }
            }}

            // Section 2: 记账设置
            item { SettingsGroupCard("💰 记账设置") {
                SwitchSettingItem("自动学习分类", autoLearn) {
                    CoroutineScope(Dispatchers.IO).launch {
                        bridge.setAutoLearnEnabled(it)
                    }
                }
                NavigationSettingItem("记忆与分类管理 →") { onNavigateToMemoryMapping() }
            }}

            // Section 3: 数据管理
            item { SettingsGroupCard("📂 数据管理") {
                NavigationSettingItem("导出 CSV →") { csvLauncher.launch("记账导出_${timestamp()}.csv") }
                NavigationSettingItem("导出日志 →") { logLauncher.launch("记账日志_${timestamp()}.log") }
            }}
        }
    }
}
```

> **实施说明：** `SettingsGroupCard`、`SettingItem`、`SwitchSettingItem`、`NavigationSettingItem` 是辅助 composable，在工作台 design system 风格下实现（白色圆角卡片 + Morandi 配色）。`loadApiKey`/`loadModel`/`saveApiKey`/`saveModel` 复用现有逻辑。

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/aigrowth/os/feature/settings/
git commit -m "feat: 重建工作台 SettingsScreen 为三组卡片 + MemoryMappingViewModel"
```

---

## Task 10: DashboardScreen 添加双列数据网格

**Files:**
- Modify: `feature/learning/src/main/java/com/aigrowth/os/feature/learning/presentation/ui/DashboardScreen.kt`
- Create: `app/src/main/java/com/aigrowth/os/DashboardAccountingViewModel.kt`

- [ ] **Step 1: 创建 DashboardAccountingViewModel**

```kotlin
// app/.../DashboardAccountingViewModel.kt
package com.aigrowth.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import kotlinx.coroutines.flow.*

class DashboardAccountingViewModel : ViewModel() {
    private val bridge = AccountingApp.getBridge()

    val monthlyExpense: StateFlow<Double?> = bridge.getMonthlyExpense()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

- [ ] **Step 2: 在 DashboardScreen 中添加参数和网格 UI**

在 `DashboardScreen` composable 签名中添加：
```kotlin
onAccountingClick: () -> Unit = {},
monthlyExpense: StateFlow<Double?> = MutableStateFlow(null)
```

在 LazyColumn 中 `TodayStatsRow` 之后插入双列网格：
```kotlin
item {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 左格：记账
        AccountingCard(
            monthlyExpense = monthlyExpense.collectAsState().value,
            onClick = onAccountingClick,
            modifier = Modifier.weight(1f)
        )
        // 右格：记忆（已有 onMemoryClick）
        MemoryCard(
            onClick = onMemoryClick,
            modifier = Modifier.weight(1f)
        )
    }
}
```

实现 `AccountingCard` composable：
```kotlin
@Composable
private fun AccountingCard(monthlyExpense: Double?, onClick: () -> Unit, modifier: Modifier) {
    MorandiCard(onClick = onClick, modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💰", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(4.dp))
                Text("本月支出", style = MaterialTheme.typography.labelSmall, color = Morandi.TextSecondary)
            }
            Text(
                text = monthlyExpense?.let { "¥${formatAmount(it)}" } ?: "暂无数据",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Morandi.TextPrimary
            )
            Text("记一笔 →", style = MaterialTheme.typography.labelSmall, color = Morandi.Brand)
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/aigrowth/os/DashboardAccountingViewModel.kt feature/learning/src/main/java/com/aigrowth/os/feature/learning/presentation/ui/DashboardScreen.kt
git commit -m "feat: DashboardScreen 添加双列数据网格（记账入口 + 月支出展示）"
```

---

## Task 11: AIGrowthOSApp 接线回调 + NavHost 路由

**Files:**
- Modify: `app/src/main/java/com/aigrowth/os/AIGrowthOSApp.kt`

- [ ] **Step 1: 在 Screen sealed class 中添加 MemoryMapping 路由**

```kotlin
@Serializable data object MemoryMapping : Screen()  // 新增
```

- [ ] **Step 2: 在 NavHost 中接线和添加路由**

```kotlin
composable(Screen.Dashboard.route) {
    val accountingVm: DashboardAccountingViewModel = viewModel()
    DashboardScreen(
        onGoalListClick = { navController.navigate(Screen.GoalList.route) },
        onMemoryClick = { navController.navigate(Screen.Memory.route) },
        onSettingsClick = { navController.navigate(Screen.Settings.route) },
        onTaskClick = { taskId -> /* 现有逻辑 */ },
        onTaskListClick = { navController.navigate(Screen.DailyTask.route) },
        onAccountingClick = {
            context.startActivity(Intent(context, com.accounting.app.MainActivity::class.java))
        },
        monthlyExpense = accountingVm.monthlyExpense
    )
}

composable(Screen.MemoryMapping.route) {
    val vm: MemoryMappingViewModel = viewModel()
    MemoryMappingManageScreen(
        memories = vm.memories.collectAsState().value,
        mappings = vm.mappings.collectAsState().value,
        expenseRootCategories = vm.expenseRootCategories,
        incomeRootCategories = vm.incomeRootCategories,
        expenseSubcategories = vm.expenseSubcategories,
        incomeSubcategories = vm.incomeSubcategories,
        onAddMemory = vm::addMemory,
        onDeleteMemory = vm::deleteMemory,
        onClearAllMemories = vm::clearAllMemories,
        onRestoreMemories = vm::restoreDefaultMemories,
        onSearch = { /* TODO */ },
        onSourceFilter = { /* TODO */ },
        memorySearchQuery = "",
        memorySourceFilter = "",
        onAddMapping = { kw, type, cat, sub -> vm.addMapping(kw, type, cat, sub) },
        onUpdateMapping = { id, kw, cat, sub -> /* TODO */ },
        onDeleteMapping = vm::deleteMapping,
        onToggleMappingEnabled = vm::toggleMappingEnabled,
        onPromoteMappingToManual = vm::promoteMappingToManual,
        onCleanStaleAutoMappings = vm::cleanStaleAutoMappings,
        onBack = { navController.popBackStack() }
    )
}
```

在 SettingsScreen 调用处传入导航回调：
```kotlin
composable(Screen.Settings.route) {
    SettingsScreen(
        onNavigateToMemoryMapping = { navController.navigate(Screen.MemoryMapping.route) },
        onBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/aigrowth/os/AIGrowthOSApp.kt
git commit -m "feat: AIGrowthOSApp 接线 onAccountingClick + NavHost MemoryMapping 路由"
```

---

## Task 12: 版本号更新 + 全项目编译验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts` 中将 `versionCode` +1，调整 `versionName`。

- [ ] **Step 2: 全项目编译验证**

```bash
cd "d:\记账app\工作台app\AI-Growth-OS"
.\gradlew.bat assembleDebug
```

期望：BUILD SUCCESSFUL。修复所有编译错误。

- [ ] **Step 3: UI 回归验收清单**

对照设计文档中的 13 项 UI 回归清单逐项验证（Tab 切换、搜索 debounce、添加/删除/恢复默认等）。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "chore: 版本号更新 + 全项目编译验证通过"
```

---

## Self-Review Notes

**Spec coverage:**
- ✅ Step 1 capture 删除 → Task 1-4 (修复残留引用 + grep + 编译)
- ✅ Step 2 AccountingBridge → Task 5-6
- ✅ Step 3 autoLearn 持久化 → Task 7
- ✅ Step 4 记忆映射合并 → Task 8
- ✅ Step 5 工作台 SettingsScreen → Task 9
- ✅ Step 6 accounting SettingsScreen 删除 → 已在之前完成
- ✅ Step 7 DashboardScreen → Task 10
- ✅ Step 8 接线 + NavHost → Task 11
- ✅ Step 9 版本号 + 编译 → Task 12

**关键实施注意事项:**
- Task 6 中的 DAO 方法名需要对照实际 DAO 接口调整
- Task 8 的合并需要逐行移植原屏幕代码，约 1000 行工作量
- Task 11 中的 `onSearch`/`onSourceFilter`/`onUpdateMapping` 需要补充实现
