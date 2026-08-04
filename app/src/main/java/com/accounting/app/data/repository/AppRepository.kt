package com.accounting.app.data.repository

import android.content.Context
import com.accounting.app.data.local.dao.CategoryAmount
import com.accounting.app.data.local.database.AppDatabase
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.database.SeedData
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import com.accounting.app.data.local.pref.UserPreferences
import com.accounting.app.data.remote.DeepSeekApi
import com.accounting.app.data.remote.DeepSeekModels
import com.accounting.app.data.remote.RetrofitClient
import com.accounting.app.data.remote.model.ChatMessage
import com.accounting.app.data.remote.model.ChatRequest
import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.parser.category.AiClassifier
import com.accounting.app.parser.category.ClassificationService
import com.accounting.app.parser.intent.MappingMatcher
import com.accounting.app.plan.execution.BillTransaction
import com.accounting.app.plan.execution.PlanExecutor
import com.accounting.app.plan.model.ExecuteResult
import com.accounting.app.ai.service.AiPlanner
import com.accounting.app.plan.builder.PlanBuilder
import com.accounting.app.parser.intent.IntentRouter
import com.accounting.app.parser.model.RoutingResult
import com.accounting.app.log.AppLogger
import com.accounting.app.util.AmountUtils
import com.accounting.app.parser.time.TimeUtils
import com.accounting.app.data.local.pref.PersistedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 记账解析结果（兼容旧版 API，保留为空壳）。
 *
 * 使用 sealed class 限定结果只能是成功或失败两种状态，
 * 调用方通过 when 分支处理，强制覆盖所有情况。
 */
sealed class ParseResult {
    /**
     * 解析成功。
     *
     * @param type           收支类型：expense / income
     * @param amount         金额（分），由 AI 返回的元换算得到
     * @param category       一级分类
     * @param subcategory    二级分类，可能为空
     * @param merchant       商家名，可能为空
     * @param time           时间戳（毫秒）
     * @param note           备注，可能为空
     * @param confidence     置信度 0~1；命中记忆时强制为 1.0
     * @param matchedMemory  是否命中前置记忆
     */
    data class Success(
        val type: String,
        val amount: Long,
        val category: String,
        val subcategory: String?,
        val merchant: String?,
        val time: Long,
        val note: String?,
        val confidence: Float,
        val matchedMemory: Boolean,
        val memoryId: Long? = null,  // 命中记忆的 id，用于入库成功后 hitCount +1
        val source: String = ""
    ) : ParseResult()

    /** 解析失败，reason 描述失败原因供 UI 提示。 */
    data class Failure(val reason: String) : ParseResult()
}

/**
 * 应用统一数据仓库。
 *
 * 职责：
 * 1. 代理所有 DAO 操作，对上层屏蔽 Room 细节
 * 2. 管理 API Key（DataStore + BuildConfig 回退）
 * 3. 计划执行（事务批量操作）
 *
 * 设计说明：
 * - 持有 Context 是为了初始化数据库与 DataStore，实际使用 applicationContext
 * - DeepSeekApi 在构造时即创建，避免每次请求重建 Retrofit
 * - 计划生成逻辑委托给 PlanBuilder，Repository 仅负责持久化
 */
class AppRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val expenseDao = database.expenseDao()
    private val incomeDao = database.incomeDao()
    private val categoryMemoryDao = database.categoryMemoryDao()
    private val categoryMappingDao = database.categoryMappingDao()
    private val userPrefs = UserPreferences(context)
    private val deepSeekApi: DeepSeekApi = RetrofitClient.create()

    private val aiPlanner = AiPlanner(RetrofitClient.createPlannerApi()) { getApiKey() }
    private val planBuilder = PlanBuilder(aiPlanner)
    private val planExecutor = PlanExecutor(
        database,
        BillTransaction(expenseDao, incomeDao)
    )
    private val intentRouter = IntentRouter(planBuilder, aiPlanner, this)

    init {
        MappingMatcher.init(this)
        ClassificationService.aiClassifier = AiClassifier { getApiKey() }
    }

    // 暴露 userPrefs 给 ViewModel 用于聊天记录持久化
    fun getUserPreferences(): UserPreferences = userPrefs

    // ===== 关键词体系：收支预判 + 触发词黑名单 =====

    // 触发词黑名单：通用词/泛化词/疑问词禁止入库
    private val triggerBlacklist = setOf(
        "花", "花了", "买了", "吃了", "喝了", "花", "买", "吃", "消费", "支出", "付了", "买单",
        "吃饭", "买东西", "购物", "日用品", "生活用品", "杂物", "其他", "东西",
        "这个", "那个", "啥", "一些", "一点",
        "多少钱", "多少", "几元"
    )

    // ===================== 数据库操作代理 =====================

    /**
     * 新增支出记录，返回自增 id。
     *
     * 节点7「入库执行」埋点：打印执行结果（成功/失败）、入库条数、账单ID；失败时打印异常堆栈。
     *
     * @param requestId 请求唯一ID（无默认值）
     * @param billIndex 笔序号（多笔场景下从 1 开始，单笔场景可传 null）
     */
    suspend fun insertExpense(entity: ExpenseEntity, requestId: String, billIndex: Int? = null): Long {
        return try {
            val id = expenseDao.insert(entity)
            if (billIndex != null) {
                AppLogger.i(requestId, "入库执行", "结果：成功，账单ID：$id，类型：expense，金额：${entity.amount}分", billIndex)
            } else {
                AppLogger.i(requestId, "入库执行", "结果：成功，账单ID：$id，类型：expense，金额：${entity.amount}分")
            }
            id
        } catch (e: Exception) {
            val msg = "结果：失败，类型：expense，金额：${entity.amount}分，异常：${e.message}"
            if (billIndex != null) {
                AppLogger.e(requestId, "入库执行", msg, e, billIndex)
            } else {
                AppLogger.e(requestId, "入库执行", msg, e)
            }
            throw e
        }
    }

    /**
     * 新增收入记录，返回自增 id。
     *
     * 节点7「入库执行」埋点。
     *
     * @param requestId 请求唯一ID（无默认值）
     * @param billIndex 笔序号（多笔场景下从 1 开始，单笔场景可传 null）
     */
    suspend fun insertIncome(entity: IncomeEntity, requestId: String, billIndex: Int? = null): Long {
        return try {
            val id = incomeDao.insert(entity)
            if (billIndex != null) {
                AppLogger.i(requestId, "入库执行", "结果：成功，账单ID：$id，类型：income，金额：${entity.amount}分", billIndex)
            } else {
                AppLogger.i(requestId, "入库执行", "结果：成功，账单ID：$id，类型：income，金额：${entity.amount}分")
            }
            id
        } catch (e: Exception) {
            val msg = "结果：失败，类型：income，金额：${entity.amount}分，异常：${e.message}"
            if (billIndex != null) {
                AppLogger.e(requestId, "入库执行", msg, e, billIndex)
            } else {
                AppLogger.e(requestId, "入库执行", msg, e)
            }
            throw e
        }
    }

    /** 按 id 删除支出 */
    suspend fun deleteExpense(id: Long, requestId: String) {
        AppLogger.d(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=start, id=$id, type=expense")
        try {
            expenseDao.deleteById(id)
            AppLogger.i(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=success, result=success, id=$id, type=expense")
        } catch (e: Exception) {
            AppLogger.e(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=error, result=failure, id=$id, type=expense, error=${e.message}", e)
            throw e
        }
    }

    /** 按 id 删除收入 */
    suspend fun deleteIncome(id: Long, requestId: String) {
        AppLogger.d(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=start, id=$id, type=income")
        try {
            incomeDao.deleteById(id)
            AppLogger.i(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=success, result=success, id=$id, type=income")
        } catch (e: Exception) {
            AppLogger.e(requestId, "删除执行", "requestId=$requestId, action=DELETE, stage=error, result=failure, id=$id, type=income, error=${e.message}", e)
            throw e
        }
    }

    /** 修改支出分类 */
    suspend fun updateExpenseCategory(id: Long, category: String, subcategory: String?, requestId: String) {
        AppLogger.d(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=start, id=$id, type=expense, category=$category, subcategory=$subcategory")
        try {
            val rowsAffected = expenseDao.updateCategory(id, category, subcategory)
            AppLogger.i(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=success, result=success, id=$id, type=expense, category=$category, rowsAffected=$rowsAffected")
        } catch (e: Exception) {
            AppLogger.e(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=error, result=failure, id=$id, type=expense, category=$category, subcategory=$subcategory, error=${e.message}", e)
            throw e
        }
    }

    /** 修改收入分类 */
    suspend fun updateIncomeCategory(id: Long, category: String, subcategory: String?, requestId: String) {
        AppLogger.d(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=start, id=$id, type=income, category=$category, subcategory=$subcategory")
        try {
            val rowsAffected = incomeDao.updateCategory(id, category, subcategory)
            AppLogger.i(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=success, result=success, id=$id, type=income, category=$category, rowsAffected=$rowsAffected")
        } catch (e: Exception) {
            AppLogger.e(requestId, "分类更新执行", "requestId=$requestId, action=UPDATE_CATEGORY, stage=error, result=failure, id=$id, type=income, category=$category, subcategory=$subcategory, error=${e.message}", e)
            throw e
        }
    }

    /** 全字段更新支出（不修改 confidence / rawInput / createdAt），返回受影响行数 */
    suspend fun updateExpenseFull(
        id: Long, amount: Long, category: String, subcategory: String?,
        merchant: String?, time: Long, note: String?, requestId: String
    ): Int {
        AppLogger.d(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=start, id=$id, type=expense, category=$category")
        return try {
            val rowsAffected = expenseDao.updateAllFields(id, amount, category, subcategory, merchant, time, note)
            AppLogger.i(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=success, result=success, id=$id, type=expense, category=$category, rowsAffected=$rowsAffected")
            rowsAffected
        } catch (e: Exception) {
            AppLogger.e(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=error, result=failure, id=$id, type=expense, category=$category, error=${e.message}", e)
            throw e
        }
    }

    /** 全字段更新收入（不修改 confidence / rawInput / createdAt），返回受影响行数 */
    suspend fun updateIncomeFull(
        id: Long, amount: Long, category: String, subcategory: String?,
        merchant: String?, time: Long, note: String?, requestId: String
    ): Int {
        AppLogger.d(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=start, id=$id, type=income, category=$category")
        return try {
            val rowsAffected = incomeDao.updateAllFields(id, amount, category, subcategory, merchant, time, note)
            AppLogger.i(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=success, result=success, id=$id, type=income, category=$category, rowsAffected=$rowsAffected")
            rowsAffected
        } catch (e: Exception) {
            AppLogger.e(requestId, "全字段更新执行", "requestId=$requestId, action=UPDATE_FULL, stage=error, result=failure, id=$id, type=income, category=$category, error=${e.message}", e)
            throw e
        }
    }

    /** 获取最近 limit 条支出 */
    fun getRecentExpenses(limit: Int): Flow<List<ExpenseEntity>> = expenseDao.getRecent(limit)

    /** 获取最近 limit 条收入 */
    fun getRecentIncomes(limit: Int): Flow<List<IncomeEntity>> = incomeDao.getRecent(limit)

    /** 按时间范围查询支出 */
    fun getExpenseByTimeRange(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getByTimeRange(start, end)

    /** 按时间范围查询收入 */
    fun getIncomeByTimeRange(start: Long, end: Long): Flow<List<IncomeEntity>> =
        incomeDao.getByTimeRange(start, end)

    /** 按时间范围获取支出总额 */
    fun getExpenseSum(start: Long, end: Long): Flow<Long?> = expenseDao.getSumByTimeRange(start, end)

    /** 按时间范围获取收入总额 */
    fun getIncomeSum(start: Long, end: Long): Flow<Long?> = incomeDao.getSumByTimeRange(start, end)

    /** 按时间范围获取支出分类统计 */
    fun getExpenseCategoryStats(start: Long, end: Long): Flow<List<CategoryAmount>> =
        expenseDao.getCategoryStats(start, end)

    /** 按时间范围获取收入分类统计 */
    fun getIncomeCategoryStats(start: Long, end: Long): Flow<List<CategoryAmount>> =
        incomeDao.getCategoryStats(start, end)

    /** 获取全部支出 */
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAll()

    /** 获取全部收入 */
    fun getAllIncomes(): Flow<List<IncomeEntity>> = incomeDao.getAll()

    // ===================== 分类映射操作 =====================

    suspend fun upsertMapping(mapping: CategoryMappingEntity, requestId: String): Long {
        AppLogger.d(requestId, "映射写入", "requestId=$requestId, action=UPSERT_MAPPING, stage=start, type=${mapping.type}, keyword=${mapping.keyword}, source=${mapping.source}")
        return try {
            val id = categoryMappingDao.upsert(mapping)
            AppLogger.i(requestId, "映射写入", "requestId=$requestId, action=UPSERT_MAPPING, stage=success, result=success, id=$id, type=${mapping.type}, keyword=${mapping.keyword}")
            id
        } catch (e: Exception) {
            AppLogger.e(requestId, "映射写入", "requestId=$requestId, action=UPSERT_MAPPING, stage=error, result=failure, type=${mapping.type}, keyword=${mapping.keyword}, error=${e.message}", e)
            throw e
        }
    }

    suspend fun matchMapping(type: String, text: String): CategoryMappingEntity? =
        categoryMappingDao.match(type, text)

    suspend fun getMappingsBySource(source: String): List<CategoryMappingEntity> =
        categoryMappingDao.getBySource(source)

    suspend fun getAllMappings(): List<CategoryMappingEntity> = categoryMappingDao.getAll()

    suspend fun deleteMappingById(id: Long, requestId: String) {
        AppLogger.d(requestId, "映射删除", "requestId=$requestId, action=DELETE_MAPPING, stage=start, id=$id")
        try {
            categoryMappingDao.deleteById(id)
            AppLogger.i(requestId, "映射删除", "requestId=$requestId, action=DELETE_MAPPING, stage=success, result=success, id=$id")
        } catch (e: Exception) {
            AppLogger.e(requestId, "映射删除", "requestId=$requestId, action=DELETE_MAPPING, stage=error, result=failure, id=$id, error=${e.message}", e)
            throw e
        }
    }

    suspend fun updateMappingEnabled(id: Long, enabled: Boolean, requestId: String) {
        AppLogger.d(requestId, "映射启用更新", "requestId=$requestId, action=UPDATE_MAPPING_ENABLED, stage=start, id=$id, enabled=$enabled")
        try {
            val rowsAffected = categoryMappingDao.updateEnabled(id, enabled)
            AppLogger.i(requestId, "映射启用更新", "requestId=$requestId, action=UPDATE_MAPPING_ENABLED, stage=success, result=success, id=$id, enabled=$enabled, rowsAffected=$rowsAffected")
        } catch (e: Exception) {
            AppLogger.e(requestId, "映射启用更新", "requestId=$requestId, action=UPDATE_MAPPING_ENABLED, stage=error, result=failure, id=$id, enabled=$enabled, error=${e.message}", e)
            throw e
        }
    }

    suspend fun promoteMappingToManual(id: Long, requestId: String) {
        AppLogger.d(requestId, "映射提升", "requestId=$requestId, action=PROMOTE_MAPPING, stage=start, id=$id")
        try {
            val rowsAffected = categoryMappingDao.promoteToManual(id, TimeUtils.now())
            AppLogger.i(requestId, "映射提升", "requestId=$requestId, action=PROMOTE_MAPPING, stage=success, result=success, id=$id, rowsAffected=$rowsAffected")
        } catch (e: Exception) {
            AppLogger.e(requestId, "映射提升", "requestId=$requestId, action=PROMOTE_MAPPING, stage=error, result=failure, id=$id, error=${e.message}", e)
            throw e
        }
    }

    suspend fun cleanStaleAutoMappings(beforeTime: Long, requestId: String): Int {
        AppLogger.d(requestId, "清理过期映射", "requestId=$requestId, action=CLEAN_STALE_MAPPINGS, stage=start, beforeTime=$beforeTime")
        return try {
            val rowsAffected = categoryMappingDao.cleanStaleAutoMappings(beforeTime)
            AppLogger.i(requestId, "清理过期映射", "requestId=$requestId, action=CLEAN_STALE_MAPPINGS, stage=success, result=success, rowsAffected=$rowsAffected")
            rowsAffected
        } catch (e: Exception) {
            AppLogger.e(requestId, "清理过期映射", "requestId=$requestId, action=CLEAN_STALE_MAPPINGS, stage=error, result=failure, error=${e.message}", e)
            throw e
        }
    }

    suspend fun findMappingByKeywordAndType(keyword: String, type: String): CategoryMappingEntity? =
        categoryMappingDao.findByKeywordAndType(keyword, type)

    suspend fun incrementMappingHitCount(id: Long, requestId: String) {
        AppLogger.d(requestId, "映射命中自增", "requestId=$requestId, action=INCREMENT_MAPPING_HIT, stage=start, id=$id")
        try {
            categoryMappingDao.incrementHitCount(id, TimeUtils.now())
            AppLogger.i(requestId, "映射命中自增", "requestId=$requestId, action=INCREMENT_MAPPING_HIT, stage=success, result=success, id=$id")
        } catch (e: Exception) {
            AppLogger.e(requestId, "映射命中自增", "requestId=$requestId, action=INCREMENT_MAPPING_HIT, stage=error, result=failure, id=$id, error=${e.message}", e)
            throw e
        }
    }

    fun getAllMemoriesByType(type: String): Flow<List<CategoryMemoryEntity>> = categoryMemoryDao.getAllByType(type)

    suspend fun upsertMemory(memory: CategoryMemoryEntity, requestId: String): Long {
        AppLogger.d(requestId, "记忆写入", "requestId=$requestId, action=UPSERT_MEMORY, stage=start, type=${memory.type}, triggerWord=${memory.triggerWord}, category=${memory.category}, source=${memory.source}")
        return try {
            val id = categoryMemoryDao.upsert(memory)
            AppLogger.i(requestId, "记忆写入", "requestId=$requestId, action=UPSERT_MEMORY, stage=success, result=success, id=$id, type=${memory.type}, triggerWord=${memory.triggerWord}, category=${memory.category}")
            id
        } catch (e: Exception) {
            AppLogger.e(requestId, "记忆写入", "requestId=$requestId, action=UPSERT_MEMORY, stage=error, result=failure, type=${memory.type}, triggerWord=${memory.triggerWord}, category=${memory.category}, error=${e.message}", e)
            throw e
        }
    }

    suspend fun deleteMemory(id: Long, requestId: String) {
        AppLogger.d(requestId, "记忆删除", "requestId=$requestId, action=DELETE_MEMORY, stage=start, id=$id")
        try {
            categoryMemoryDao.deleteById(id)
            AppLogger.i(requestId, "记忆删除", "requestId=$requestId, action=DELETE_MEMORY, stage=success, result=success, id=$id")
        } catch (e: Exception) {
            AppLogger.e(requestId, "记忆删除", "requestId=$requestId, action=DELETE_MEMORY, stage=error, result=failure, id=$id, error=${e.message}", e)
            throw e
        }
    }

    suspend fun deleteAllMemories(requestId: String) {
        AppLogger.d(requestId, "全量记忆删除", "requestId=$requestId, action=DELETE_ALL_MEMORIES, stage=start")
        try {
            categoryMemoryDao.deleteAll()
            AppLogger.i(requestId, "全量记忆删除", "requestId=$requestId, action=DELETE_ALL_MEMORIES, stage=success, result=success")
        } catch (e: Exception) {
            AppLogger.e(requestId, "全量记忆删除", "requestId=$requestId, action=DELETE_ALL_MEMORIES, stage=error, result=failure, error=${e.message}", e)
            throw e
        }
    }

    suspend fun reseedMemories(requestId: String) {
        AppLogger.d(requestId, "记忆重置", "requestId=$requestId, action=RESEED_MEMORIES, stage=start")
        try {
            categoryMemoryDao.deleteAll()
            categoryMemoryDao.insertAll(SeedData.seedMemories)
            AppLogger.i(requestId, "记忆重置", "requestId=$requestId, action=RESEED_MEMORIES, stage=success, result=success")
        } catch (e: Exception) {
            AppLogger.e(requestId, "记忆重置", "requestId=$requestId, action=RESEED_MEMORIES, stage=error, result=failure, error=${e.message}", e)
            throw e
        }
    }

    suspend fun incrementMemoryHitCount(id: Long, requestId: String) {
        AppLogger.d(requestId, "记忆命中自增", "requestId=$requestId, action=INCREMENT_MEMORY_HIT, stage=start, id=$id")
        try {
            categoryMemoryDao.incrementHitCount(id, TimeUtils.now())
            AppLogger.i(requestId, "记忆命中自增", "requestId=$requestId, action=INCREMENT_MEMORY_HIT, stage=success, result=success, id=$id")
        } catch (e: Exception) {
            AppLogger.e(requestId, "记忆命中自增", "requestId=$requestId, action=INCREMENT_MEMORY_HIT, stage=error, result=failure, id=$id, error=${e.message}", e)
            throw e
        }
    }

    fun normalizeCategoryForMemory(@Suppress("UNUSED_PARAMETER") category: String, subcategory: String?): String? =
        subcategory?.trim()?.takeIf { it.isNotEmpty() }

    // ===================== API Key 管理 =====================

    /**
     * 读取 API Key：返回用户在设置页配置的值。
     * 若用户未配置，返回空字符串，由调用方自行处理。
     */
    suspend fun getApiKey(): String {
        return userPrefs.getApiKey().first()
    }

    /** 写入用户配置的 API Key 到 DataStore */
    suspend fun setApiKey(key: String, requestId: String) {
        val maskedKey = if (key.isEmpty()) "<empty>" else AppLogger.maskApiKey(key)
        AppLogger.d(requestId, "API Key 保存", "requestId=$requestId, action=SET_API_KEY, stage=start, apiKey=$maskedKey")
        try {
            userPrefs.setApiKey(key)
            AppLogger.i(requestId, "API Key 保存", "requestId=$requestId, action=SET_API_KEY, stage=success, result=success, apiKey=$maskedKey")
        } catch (e: Exception) {
            AppLogger.e(requestId, "API Key 保存", "requestId=$requestId, action=SET_API_KEY, stage=error, result=failure, apiKey=$maskedKey, error=${e.message}", e)
            throw e
        }
    }

    /** 判断触发词是否在黑名单中（公开，供 ViewModel 调用） */
    fun isValidTriggerWord(word: String): Boolean {
        return word.length >= 2 && !triggerBlacklist.contains(word)
    }

    // ===================== 计划执行 =====================

    /**
     * 批量事务执行记账计划。
     * 所有操作放在同一个数据库事务中，全部成功则提交，任意失败则回滚。
     *
     * @param plan 执行计划
     * @return 执行结果，包含成功/失败状态和失败原因
     */
    suspend fun executePlan(plan: BillExecutePlan): ExecuteResult {
        return planExecutor.execute(plan)
    }

    /**
     * 生成记账执行计划（门面转发：委托给 PlanBuilder）。
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     */
    suspend fun generatePlan(rawInput: String, requestId: String): BillExecutePlan? {
        return planBuilder.buildPlan(rawInput, requestId)
    }

    /**
     * 意图路由：三层漏斗分发。
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     */
    suspend fun routeIntent(rawInput: String, requestId: String): RoutingResult {
        return intentRouter.route(rawInput, requestId)
    }

    suspend fun parseAccountingInput(rawInput: String, requestId: String): List<ParseResult> {
        val plan = when (val routed = routeIntent(rawInput, requestId)) {
            is RoutingResult.Success -> routed.plan
            is RoutingResult.AiSuccess -> routed.plan
            is RoutingResult.Failure -> return listOf(ParseResult.Failure(routed.reason))
        }
        return plan.items.map { item ->
            ParseResult.Success(
                type = item.type,
                amount = item.amount,
                category = item.category,
                subcategory = item.subCategory,
                merchant = item.merchant,
                time = item.billTime,
                note = item.remark,
                confidence = item.confidence,
                matchedMemory = item.matchedMemory,
                memoryId = item.memoryId,
                source = item.source
            )
        }
    }

    // ===================== AI 对话查询 =====================

    /**
     * 对话查询：将用户问题与本地统计数据一起发送给 AI，返回纯文本回复。
     * 用于"这个月花了多少""餐饮消费分析"等非记账意图的对话。
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     */
    suspend fun chatQuery(userInput: String, requestId: String): String {
        return try {
            val timeRange = TimeUtils.extractTimeRange(userInput)
            val start = timeRange.start
            val end = timeRange.end

            val expense = expenseDao.getSumByTimeRange(start, end).first() ?: 0
            val income = incomeDao.getSumByTimeRange(start, end).first() ?: 0
            val topExpense = expenseDao.getCategoryStats(start, end).first()
                .sortedByDescending { it.totalAmount }.take(3)
            val allExpenses = expenseDao.getByTimeRangeWithLimit(start, end, 100).first()
            val allIncomes = incomeDao.getByTimeRangeWithLimit(start, end, 100).first()

            val timeDesc = describeTimeRange(start, end)
            val statsLines = mutableListOf<String>()
            statsLines.add("${timeDesc}总支出：${AmountUtils.fenToYuan(expense)}元，总收入：${AmountUtils.fenToYuan(income)}元")
            if (topExpense.isNotEmpty()) {
                statsLines.add("${timeDesc}支出Top3分类：")
                topExpense.forEachIndexed { i, stat ->
                    statsLines.add("${i + 1}. ${stat.category}：${AmountUtils.fenToYuan(stat.totalAmount)}元")
                }
            }
            val allRecords = (allExpenses.map { "支出 ${it.merchant ?: ""} ${it.category} ¥${AmountUtils.fenToYuan(it.amount)} ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(it.time)}" } +
                    allIncomes.map { "收入 ${it.merchant ?: ""} ${it.category} ¥${AmountUtils.fenToYuan(it.amount)} ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(it.time)}" })
                .sortedByDescending { it.takeLast(19) }
            if (allRecords.isNotEmpty()) {
                statsLines.add("${timeDesc}全部记录：")
                allRecords.forEachIndexed { i, line -> statsLines.add("${i + 1}. $line") }
            } else {
                statsLines.add("${timeDesc}暂无记录")
            }

            val systemPrompt = """
【权限红线】你仅具备账单查询、统计分析、消费解读能力，绝对不可以执行记账、修改、删除账单的操作，也不得声称已完成记账、修改、删除。
如果用户输入的是记账类内容，请统一回复：「请直接发送记账内容，我会为你生成账单记录」，不得编造任何记录结果、消费总额等虚假数据。

你是记账助手，仅回答记账、消费分析、账单查询相关问题，不闲聊、不回答无关内容。
所有回答必须严格基于下方提供的真实账单数据，数据中没有的内容直接说明「暂无相关记录」，绝对禁止编造金额、分类、记录。
回答简洁直白，用中文口语化表达，不要使用markdown格式。

【当前账单数据】
${statsLines.joinToString("\n")}
""".trimIndent()

            val apiKey = getApiKey()
            if (apiKey.isBlank() || apiKey == "your_api_key_here") {
                AppLogger.e(requestId, "AI请求发起", "对话查询未配置 API Key", null)
                return "请先在设置页配置 API Key，才能使用 AI 对话功能。"
            }
            val maskedKey = AppLogger.maskApiKey(apiKey)
            val startTime = System.currentTimeMillis()
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(startTime)
            AppLogger.i(
                requestId,
                "AI请求发起",
                "模型：${DeepSeekModels.FLASH}（对话查询），Prompt长度：${systemPrompt.length + userInput.length}字符，" +
                        "开始时间：$timeStr，API Key：$maskedKey，时间范围：${TimeUtils.formatTime(start)} 至 ${TimeUtils.formatTime(end)}"
            )

            val response = deepSeekApi.chatCompletion(
                "Bearer $apiKey",
                ChatRequest(
                    model = DeepSeekModels.FLASH,
                    messages = listOf(
                        ChatMessage("system", systemPrompt),
                        ChatMessage("user", userInput)
                    )
                )
            )
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                val preview = if (content != null && content.length > 100) content.substring(0, 100) + "..." else content ?: ""
                AppLogger.d(requestId, "AI响应返回", "状态：成功（对话查询），返回长度：${content?.length ?: 0}字符，摘要：$preview")
                content ?: "抱歉，我没有理解您的问题，请换种方式问问看。"
            } else {
                val code = response.code()
                AppLogger.e(
                    requestId,
                    "AI响应返回",
                    "状态：失败（对话查询），错误码：$code，错误信息：${response.message()}",
                    null
                )
                "网络出了点问题，请稍后再试。"
            }
        } catch (e: Exception) {
            if (e.message?.contains("API Key") == true) {
                AppLogger.e(requestId, "AI响应返回", "对话查询 API Key 异常：${e.message}", e)
                "请先在设置页配置 API Key，才能使用 AI 对话功能。"
            } else {
                AppLogger.e(requestId, "AI响应返回", "对话查询异常：${e.message}", e)
                "抱歉，暂时无法回答您的问题：${e.message}"
            }
        }
    }

    private fun describeTimeRange(start: Long, end: Long): String {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val lastWeekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val lastMonthStart = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when {
            start == today.timeInMillis -> return "今日"
            start == yesterday.timeInMillis -> return "昨日"
            start == weekStart.timeInMillis -> return "本周"
            start == lastWeekStart.timeInMillis -> return "上周"
            start == monthStart.timeInMillis -> return "本月"
            start == lastMonthStart.timeInMillis -> return "上月"
            else -> return "${SimpleDateFormat("MM-dd", Locale.CHINA).format(start)}至${SimpleDateFormat("MM-dd", Locale.CHINA).format(end)}"
        }
    }
}
