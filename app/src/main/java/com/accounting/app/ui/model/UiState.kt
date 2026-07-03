package com.accounting.app.ui.model

import com.accounting.app.data.local.dao.CategoryAmount
import com.accounting.app.data.local.entity.CategoryMemoryEntity

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
    val totalMemoryCount: Int = 0,                         // 当前 Tab 词条总数

    // ===== 弹窗状态 =====
    val showEditDialog: EditDialogData? = null,  // 修改分类弹窗
    val showManualEntry: ManualEntryData? = null, // 手动记账弹窗
    val showLearnDialog: LearnDialogData? = null, // 关键词学习确认弹窗

    // ===== 设置 =====
    val autoLearnEnabled: Boolean = true,  // 记账后自动弹出学习确认
    val savedApiKey: String = "",  // 已保存的 API Key（回填输入框用）

    // ===== CSV 导出 =====
    val csvExportData: String? = null,  // 非 null 时触发 UI 层调用 SAF 创建文件并写入
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
    val confidence: Float,
    val matchedMemory: Boolean,
    val rawInput: String
)

/**
 * 修改分类弹窗数据
 *
 * 简化方案：弹窗只修改分类，不修改 type。
 * type 字段记录当前收支类型，用于决定更新哪张表。
 */
data class EditDialogData(
    val recordId: Long,
    val type: String,         // 当前收支类型（不可修改）
    val category: String,     // 当前一级分类
    val subcategory: String?, // 当前二级分类
    val merchant: String?,    // 商家（用于提取触发词）
    val rawInput: String      // 原始输入
)

/**
 * 手动记账弹窗数据
 */
data class ManualEntryData(
    val prefillNote: String = ""  // 预填的备注（通常是失败的原始输入）
)

/**
 * 关键词学习确认弹窗数据
 */
data class LearnDialogData(
    val triggerWord: String,    // 候选触发词（取 merchant 字段）
    val type: String,           // 收支类型
    val category: String,       // 当前AI识别的一级分类
    val subcategory: String?    // 当前AI识别的二级分类
)

/**
 * 记忆分组数据 — 一级分类组
 */
data class MemoryGroup(
    val categoryName: String,       // 一级分类名
    val subGroups: List<SubGroup>   // 下属二级分组列表
)

/**
 * 记忆分组数据 — 二级分类子组
 */
data class SubGroup(
    val subName: String?,                    // 二级分类名（可为 null，如收入类部分无二级分类）
    val items: List<CategoryMemoryEntity>    // 该二级分类下的词条列表
)
