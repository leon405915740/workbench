package com.accounting.app.ui.model

import com.accounting.app.data.local.dao.CategoryAmount
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.CategoryMappingEntity

/**
 * 页面 Tab 枚举
 */
enum class AppTab { CHAT, DASHBOARD, SETTINGS }

/**
 * Dashboard 收支切换 Tab
 */
enum class DashTab { EXPENSE, INCOME }

/**
 * 统一 UI 状态。
 * 单 ViewModel 持有，驱动所有页面的 UI 更新。
 */
data class UiState(
    // ===== Chat 页面 =====
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,

    // ===== 全局 =====
    val currentTab: AppTab = AppTab.CHAT,
    val toast: String? = null,
    val error: String? = null,
    val chatResetSignal: Int = 0,   // 每次重复点击记账Tab递增，ChatScreen收到后滚动到顶
    val dashResetSignal: Int = 0,   // 每次重复点击统计Tab递增，DashboardScreen收到后滚动到顶

    // ===== Dashboard 页面 =====
    val dashTab: DashTab = DashTab.EXPENSE,
    val todayExpense: Long = 0,       // 今日支出（分）
    val monthExpense: Long = 0,       // 本月支出（分）
    val todayIncome: Long = 0,        // 今日收入（分）
    val monthIncome: Long = 0,        // 本月收入（分）
    val categoryStats: List<CategoryAmount> = emptyList(),
    val recentRecords: List<RecentRecord> = emptyList(),

    // ===== 设置/记忆管理 =====
    val allMemories: List<CategoryMemoryEntity> = emptyList(),
    val memoryGroups: List<MemoryGroup> = emptyList(),    // 分组数据供 UI 渲染
    val expandedCategories: Set<String> = emptySet(),      // 折叠状态（一级分类名集合）
    val memorySearchQuery: String = "",                    // 搜索关键词
    val memorySourceFilter: String = "",                   // 来源筛选：""=全部 / seed / auto / user
    val totalMemoryCount: Int = 0,                         // 当前 Tab 词条总数

    // ===== 分类映射管理 =====
    val currentMappingTab: String = "MANUAL",
    val mappings: List<CategoryMappingEntity> = emptyList(),
    val showAddMappingDialog: Boolean = false,

    // ===== 弹窗状态 =====
    val showEditDialog: EditDialogData? = null,  // 编辑/新建账单弹窗（recordId=null=新建，非空=编辑）
    val showLearnDialog: LearnDialogData? = null, // 关键词学习确认弹窗

    // ===== 设置 =====
    val autoLearnEnabled: Boolean = true,  // 记账后自动弹出学习确认
    val savedApiKey: String = "",  // 已保存的 API Key（回填输入框用）

    // ===== CSV 导出 =====
    val csvExportData: String? = null,  // 非 null 时触发 UI 层调用 SAF 创建文件并写入

    // ===== 日志导出 =====
    val logExportData: String? = null,  // 非 null 时触发 UI 层调用 SAF 创建文件并写入日志
)

/**
 * 最近记录统一数据类（支出和收入统一展示）
 */
data class RecentRecord(
    val id: Long,
    val type: String,         // expense / income
    val amount: Long,
    val category: String,
    val subcategory: String?,
    val merchant: String?,
    val time: Long,
    val note: String?,        // 备注
    val confidence: Float,
    val matchedMemory: Boolean,
    val rawInput: String
)

/**
 * 编辑账单弹窗数据（统一新建 + 编辑双模式）。
 *
 * - recordId 为 null 表示新建模式，非空表示编辑模式
 * - type 字段在编辑模式下不可修改（避免跨表迁移）
 * - subcategory 字段保留原值，编辑弹窗不提供修改入口，confirmEditRecord 传原值给 DAO 避免覆盖
 * - originalCategory 保存打开编辑时的原分类，用于记忆学习判断分类是否变更
 * - merchant/note 用户清空后以 null 存储（与实体字段可空性一致，不传空字符串）
 * - amount 类型为 Long（分），与 ExpenseEntity/IncomeEntity 一致
 */
data class EditDialogData(
    val recordId: Long?,         // null=新建模式，非空=编辑模式
    val type: String,            // 收支类型（编辑模式不可修改）
    val category: String,        // 当前分类
    val subcategory: String?,    // 子分类（保留原值，编辑弹窗不修改）
    val merchant: String?,       // 商家
    val rawInput: String,        // 原始输入
    val amount: Long,            // 金额（分）
    val time: Long,              // 时间戳（毫秒）
    val note: String?,           // 备注
    val originalCategory: String // 打开编辑时的原分类（记忆学习用，新建模式为空）
)

/**
 * 关键词学习确认弹窗数据
 */
data class LearnDialogData(
    val triggerWord: String,    // 候选触发词（取 merchant 字段）
    val type: String,           // 收支类型
    val category: String        // 当前AI识别的分类
)

/**
 * 记忆分组数据 — 分类组
 */
data class MemoryGroup(
    val categoryName: String,            // 分类名
    val items: List<CategoryMemoryEntity> // 该分类下的词条列表
)
