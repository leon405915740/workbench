package com.accounting.app.data.repository

import android.content.Context
import com.accounting.app.BuildConfig
import com.accounting.app.data.local.dao.CategoryAmount
import com.accounting.app.data.local.database.AppDatabase
import com.accounting.app.data.local.database.SeedData
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import com.accounting.app.data.local.pref.UserPreferences
import com.accounting.app.data.remote.DeepSeekApi
import com.accounting.app.data.remote.DeepSeekModels
import com.accounting.app.data.remote.RetrofitClient
import com.accounting.app.data.remote.model.ChatMessage
import com.accounting.app.data.remote.model.ChatRequest
import com.accounting.app.util.AppLogger
import com.accounting.app.util.TimeUtils
import com.accounting.app.data.local.pref.PersistedMessage
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.CategoryConstants
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 记账解析结果。
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
        val memoryId: Long? = null  // 命中记忆的 id，用于入库成功后 hitCount +1
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
 * 3. 封装 DeepSeek 调用与记账输入解析逻辑
 *
 * 设计说明：
 * - 持有 Context 是为了初始化数据库与 DataStore，实际使用 applicationContext
 * - DeepSeekApi 在构造时即创建，避免每次请求重建 Retrofit
 * - 解析流程的 hitCount 自增不在本类 parse 阶段做，
 *   由调用方在记账入库成功后显式调用 incrementMemoryHitCount
 */
class AppRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val expenseDao = database.expenseDao()
    private val incomeDao = database.incomeDao()
    private val memoryDao = database.categoryMemoryDao()
    private val userPrefs = UserPreferences(context)
    private val deepSeekApi: DeepSeekApi = RetrofitClient.create()

    // 暴露 userPrefs 给 ViewModel 用于聊天记录持久化
    fun getUserPreferences(): UserPreferences = userPrefs

    // ===== 关键词体系：收支预判 + 触发词黑名单 =====

    // 支出预判词（命中任一则预判 expense）
    private val expenseKeywords = listOf(
        "花", "花了", "花费", "消费", "支出", "付了", "付款", "买单", "结账",
        "充了", "充值", "买", "买了", "购买", "打车", "吃饭", "外卖", "奶茶",
        "咖啡", "房租", "话费", "电费", "水费", "会员"
    )
    // 收入预判词（命中任一则预判 income，优先短语匹配）
    private val incomeKeywords = listOf(
        "发工资", "到账", "收入", "收到", "赚了", "盈利", "报销", "退款",
        "分红", "奖金", "工资", "兼职", "理财收益", "补贴", "收了", "入账"
    )
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
    suspend fun deleteExpense(id: Long) = expenseDao.deleteById(id)

    /** 按 id 删除收入 */
    suspend fun deleteIncome(id: Long) = incomeDao.deleteById(id)

    /** 修改支出分类 */
    suspend fun updateExpenseCategory(id: Long, category: String, subcategory: String?) =
        expenseDao.updateCategory(id, category, subcategory)

    /** 修改收入分类 */
    suspend fun updateIncomeCategory(id: Long, category: String, subcategory: String?) =
        incomeDao.updateCategory(id, category, subcategory)

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

    // ===================== 记忆操作 =====================

    /** 按类型监听全部记忆 */
    fun getAllMemoriesByType(type: String): Flow<List<CategoryMemoryEntity>> =
        memoryDao.getAllByType(type)

    /** 按类型取命中次数 Top 20 记忆（挂起一次性查询） */
    suspend fun getTop20MemoriesByType(type: String): List<CategoryMemoryEntity> =
        memoryDao.getTop20ByType(type)

    /** 用户修改分类时覆盖记忆（REPLACE 策略） */
    suspend fun upsertMemory(memory: CategoryMemoryEntity) = memoryDao.upsert(memory)

    /** 按 id 删除记忆 */
    suspend fun deleteMemory(id: Long) = memoryDao.deleteById(id)

    /** 清空全部记忆 */
    suspend fun deleteAllMemories() = memoryDao.deleteAll()

    /** 命中次数 +1，updatedAt 同步刷新 */
    suspend fun incrementMemoryHitCount(id: Long) =
        memoryDao.incrementHitCount(id, TimeUtils.now())

    /**
     * 恢复默认记忆：仅删除系统种子词(source=seed)，保留用户自定义记忆(source=user)。
     * 种子词 INSERT OR IGNORE 避免与用户记忆主键冲突。
     */
    suspend fun reseedMemories() {
        memoryDao.deleteBySource("seed")
        memoryDao.insertAll(SeedData.seedMemories)
    }

    // ===================== API Key 管理 =====================

    /**
     * 读取 API Key：优先使用用户在设置页配置的值，
     * 为空则回退到编译期注入的 BuildConfig.DEEPSEEK_API_KEY。
     */
    suspend fun getApiKey(): String {
        val userKey = userPrefs.getApiKey().first()
        return if (userKey.isNotBlank()) userKey else BuildConfig.DEEPSEEK_API_KEY
    }

    /** 写入用户配置的 API Key 到 DataStore */
    suspend fun setApiKey(key: String) = userPrefs.setApiKey(key)

    // ===================== AI 解析核心 =====================

    /**
     * 本地解析结果标记。
     * @param fromMemory   是否命中用户记忆（true=用户记忆，false=内置规则）
     * @param fromBuiltin  是否命中内置规则（时间规则或场景词）
     */
    data class LocalResult(
        val type: String, val amount: Long, val category: String, val subcategory: String?,
        val merchant: String?, val time: Long, val fromMemory: Boolean, val fromBuiltin: Boolean,
        val memoryId: Long? = null
    )

    // ===================== 本地解析核心（三层匹配 + 多笔拆分） =====================

    /**
     * 第1级：完全本地解析（零 API 调用，毫秒级响应）。
     * 支持多笔拆分：一句话含多个金额时，自动拆成多条记录。
     * 分类匹配优先级：用户记忆 > 时间规则 > 内置场景词。
     *
     * @return 本地解析成功的记录列表，返回空列表表示无法本地解析（需降级AI）
     */
    private suspend fun localParseRecords(rawInput: String, requestId: String): List<LocalResult> {
        val segments = AmountUtils.extractAmounts(rawInput, requestId)
        if (segments.isEmpty()) return emptyList()
        if (segments.size > 10) return emptyList() // 超限走提示

        // 提取全局时间（今天/昨天/前天），所有分拆账单统一继承
        val globalTime = TimeUtils.simpleParseTime(rawInput)

        val results = mutableListOf<LocalResult>()
        for ((index, seg) in segments.withIndex()) {
            val billIndex = index + 1
            val cleanSegment = AmountUtils.cleanSegment(seg.textBefore)
            val localRecord = parseSingleSegment(cleanSegment, seg.amountFen, requestId, billIndex)
            if (localRecord != null) {
                // 全局时间优先，单笔内时间词可覆盖
                val finalTime = if (globalTime != null) {
                    val segTime = TimeUtils.simpleParseTime(cleanSegment)
                    segTime ?: globalTime
                } else {
                    TimeUtils.simpleParseTime(cleanSegment) ?: TimeUtils.now()
                }
                results.add(localRecord.copy(time = finalTime))
            }
        }
        return results
    }

    /**
     * 单条分段的本地分类匹配（三层优先级）。
     * 1. 用户记忆匹配 → 返回记忆分类（fromMemory=true）
     * 2. 时间规则匹配 → 返回时间对应餐饮分类（fromBuiltin=true, merchant=null）
     * 3. 内置场景词匹配 → 返回场景对应分类（fromBuiltin=true, merchant=null）
     * 4. 全部未命中 → 返回 null（降级AI）
     *
     * 节点4「分类匹配」埋点：打印待匹配文本、命中的触发词、来源(user/seed)、基础分类、时段规则命中情况、最终分类结果。
     */
    private suspend fun parseSingleSegment(
        textBefore: String,
        amountFen: Long,
        requestId: String,
        billIndex: Int
    ): LocalResult? {
        val cleanText = AmountUtils.filterStopWords(textBefore)

        // 第1层：用户记忆匹配（最高优先级）
        val probableType = preJudgeType(cleanText, requestId, billIndex)
        val memory = matchMemory(cleanText, probableType, requestId, billIndex)
        if (memory != null) {
            val (finalCat, finalSub) = applyTimeCategory(memory.category, memory.subcategory, cleanText)
            // 时段规则命中情况
            val timeCategory = TimeUtils.matchTimeCategory(cleanText)
            val timeRuleHit = if (timeCategory != null) "${timeCategory.first}-${timeCategory.second}命中" else "未命中"
            val source = if (memory.source == "user") "user" else "seed"
            AppLogger.d(
                requestId,
                "分类匹配",
                "待匹配：$cleanText，触发词：${memory.triggerWord}，来源：$source，" +
                        "基础分类：${memory.category}-${memory.subcategory ?: ""}，" +
                        "时段规则：$timeRuleHit，最终分类：${finalCat}-${finalSub ?: ""}",
                billIndex
            )
            return LocalResult(
                type = memory.type, amount = amountFen,
                category = finalCat, subcategory = finalSub,
                merchant = memory.triggerWord,
                time = TimeUtils.simpleParseTime(cleanText) ?: TimeUtils.now(),
                fromMemory = true, fromBuiltin = false, memoryId = memory.id
            )
        }

        // 第2层：时间规则匹配
        val timeCat = TimeUtils.matchTimeCategory(cleanText)
        if (timeCat != null) {
            AppLogger.d(
                requestId,
                "分类匹配",
                "待匹配：$cleanText，触发词：时段词(${timeCat.first}-${timeCat.second})，来源：builtin，" +
                        "基础分类：${timeCat.first}-${timeCat.second}，时段规则：命中，最终分类：${timeCat.first}-${timeCat.second}",
                billIndex
            )
            return LocalResult(
                type = "expense", amount = amountFen,
                category = timeCat.first, subcategory = timeCat.second,
                merchant = null,  // 内置规则不填商家
                time = TimeUtils.simpleParseTime(cleanText) ?: TimeUtils.now(),
                fromMemory = false, fromBuiltin = true
            )
        }

        // 第3层：内置场景词匹配
        for ((keyword, catPair) in CategoryConstants.builtinSceneMap) {
            if (cleanText.contains(keyword)) {
                AppLogger.d(
                    requestId,
                    "分类匹配",
                    "待匹配：$cleanText，触发词：$keyword，来源：builtin，" +
                            "基础分类：${catPair.first}-${catPair.second}，时段规则：未命中，最终分类：${catPair.first}-${catPair.second}",
                    billIndex
                )
                return LocalResult(
                    type = "expense", amount = amountFen,
                    category = catPair.first, subcategory = catPair.second,
                    merchant = null,  // 内置规则不填商家
                    time = TimeUtils.simpleParseTime(cleanText) ?: TimeUtils.now(),
                    fromMemory = false, fromBuiltin = true
                )
            }
        }

        // 拿不准 → 返回 null，降级走 AI
        AppLogger.d(requestId, "分类匹配", "待匹配：$cleanText，触发词：无，来源：无，基础分类：无，时段规则：未命中，最终分类：null（降级AI）", billIndex)
        return null
    }

    /**
     * 记账输入解析（三级降级：本地→FLASH→PRO）。
     * 优先尝试全本地解析（支持多笔拆分），失败则降级到 AI。
     * @return 解析结果列表，可能包含多笔本地记录或单笔AI记录
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     */
    suspend fun parseAccountingInput(rawInput: String, requestId: String): List<ParseResult> {
        return try {
            // 第1级：完全本地解析（支持多笔拆分）
            val localRecords = localParseRecords(rawInput, requestId)
            if (localRecords.isNotEmpty()) {
                return localRecords.map { r ->
                    val (finalCat, finalSub) = applyTimeCategory(r.category, r.subcategory, rawInput)
                    ParseResult.Success(
                        type = r.type, amount = r.amount,
                        category = finalCat, subcategory = finalSub,
                        merchant = r.merchant, time = r.time,
                        note = null, confidence = 1.0f,
                        matchedMemory = r.fromMemory, memoryId = r.memoryId
                    )
                }
            }

            // 降级到 AI
            val probableType = preJudgeType(rawInput, requestId, null)
            val memory = matchMemory(rawInput, probableType, requestId, null)

            if (memory != null) {
                // 第2级：半本地解析 — 记忆命中但金额/时间复杂，调用 FLASH
                val (systemPrompt, userPrompt) = buildCompletionPrompt(rawInput)
                val aiContent = callDeepSeek(DeepSeekModels.FLASH, systemPrompt, userPrompt, requestId)
                val json = parseAndValidate(aiContent, requestId)
                listOf(ParseResult.Success(
                    type = memory.type, amount = extractAmount(json),
                    category = memory.category, subcategory = memory.subcategory,
                    merchant = json.getStringOrNull("merchant"), time = TimeUtils.now(),
                    note = json.getStringOrNull("note"), confidence = 1.0f,
                    matchedMemory = true, memoryId = memory.id
                ))
            } else {
                // 第3级：全量AI解析 — 调用 PRO
                val (systemPrompt, userPrompt) = buildFullPrompt(rawInput)
                val aiContent = callDeepSeek(DeepSeekModels.PRO, systemPrompt, userPrompt, requestId)
                val json = parseAndValidate(aiContent, requestId)
                val aiType = json.get("type").asString
                val fallbackMemory = matchMemory(rawInput, aiType, requestId, null)
                if (fallbackMemory != null) {
                    listOf(ParseResult.Success(
                        type = fallbackMemory.type, amount = extractAmount(json),
                        category = fallbackMemory.category, subcategory = fallbackMemory.subcategory,
                        merchant = json.getStringOrNull("merchant"), time = TimeUtils.now(),
                        note = json.getStringOrNull("note"), confidence = 1.0f,
                        matchedMemory = true, memoryId = fallbackMemory.id
                    ))
                } else {
                    listOf(ParseResult.Success(
                        type = aiType, amount = extractAmount(json),
                        category = json.get("category").asString, subcategory = json.getStringOrNull("subcategory"),
                        merchant = json.getStringOrNull("merchant"), time = TimeUtils.now(),
                        note = json.getStringOrNull("note"),
                        confidence = json.get("confidence")?.takeIf { !it.isJsonNull }?.asFloat ?: 0.5f,
                        matchedMemory = false
                    ))
                }
            }
        } catch (e: Exception) {
            AppLogger.e(requestId, "入库执行", "解析异常：${e.message}", e)
            listOf(ParseResult.Failure(e.message ?: "解析失败"))
        }
    }

    /**
     * 关键词预判收支类型：优先匹配收入短语，再匹配支出词。
     * 二者都不匹配则默认 expense（符合日常90%场景）。
     *
     * 节点2「意图分流」埋点：打印原始文本、命中的关键词/规则、最终判定意图、判定依据。
     */
    private fun preJudgeType(rawInput: String, requestId: String, billIndex: Int? = null): String {
        val hitIncome = incomeKeywords.firstOrNull { rawInput.contains(it) }
        if (hitIncome != null) {
            val msg = "原始文本：$rawInput，命中关键词/规则：$hitIncome，最终判定意图：收入，判定依据：收入关键词"
            if (billIndex != null) AppLogger.d(requestId, "意图分流", msg, billIndex) else AppLogger.d(requestId, "意图分流", msg)
            return "income"
        }
        val hitExpense = expenseKeywords.firstOrNull { rawInput.contains(it) }
        if (hitExpense != null) {
            val msg = "原始文本：$rawInput，命中关键词/规则：$hitExpense，最终判定意图：支出，判定依据：支出关键词"
            if (billIndex != null) AppLogger.d(requestId, "意图分流", msg, billIndex) else AppLogger.d(requestId, "意图分流", msg)
            return "expense"
        }
        val msg = "原始文本：$rawInput，命中关键词/规则：无，最终判定意图：支出，判定依据：默认 expense"
        if (billIndex != null) AppLogger.d(requestId, "意图分流", msg, billIndex) else AppLogger.d(requestId, "意图分流", msg)
        return "expense"
    }

    /** 判断触发词是否在黑名单中（公开，供 ViewModel 调用） */
    fun isValidTriggerWord(word: String): Boolean {
        return word.length >= 2 && !triggerBlacklist.contains(word)
    }

    /**
     * 触发词最长匹配，优先用户记忆(source="user")，其次种子词(source="seed")。
     * 多个同类型命中时取最长触发词。
     *
     * 节点4「分类匹配」埋点：记忆命中时打印来源、触发词、最终分类。
     */
    private suspend fun matchMemory(
        rawInput: String,
        type: String,
        requestId: String,
        billIndex: Int? = null
    ): CategoryMemoryEntity? {
        val allMemories = memoryDao.getAllByTypeOnce(type)
        val userMemories = allMemories.filter { it.source == "user" && rawInput.contains(it.triggerWord) }
        if (userMemories.isNotEmpty()) {
            val hit = userMemories.maxByOrNull { it.triggerWord.length }
            if (hit != null) {
                val msg = "记忆命中（user）→ 触发词：${hit.triggerWord}，分类：${hit.category}-${hit.subcategory ?: ""}"
                if (billIndex != null) AppLogger.d(requestId, "分类匹配", msg, billIndex) else AppLogger.d(requestId, "分类匹配", msg)
            }
            return hit
        }
        val seedMemories = allMemories.filter { it.source == "seed" && rawInput.contains(it.triggerWord) }
        val hit = seedMemories.maxByOrNull { it.triggerWord.length }
        if (hit != null) {
            val msg = "记忆命中（seed）→ 触发词：${hit.triggerWord}，分类：${hit.category}-${hit.subcategory ?: ""}"
            if (billIndex != null) AppLogger.d(requestId, "分类匹配", msg, billIndex) else AppLogger.d(requestId, "分类匹配", msg)
        }
        return hit
    }

    /**
     * 品类与时段分类解耦：对餐饮-正餐做动态时段覆盖。
     * 仅 category=="餐饮" && subcategory=="正餐" 时生效，
     * 先查文本时间词，其次用系统当前时间判定时段。
     */
    fun applyTimeCategory(category: String, subcategory: String?, rawInput: String): Pair<String, String?> {
        if (category != "餐饮" || subcategory != "正餐") return Pair(category, subcategory)
        val timeSub = TimeUtils.matchTimeCategory(rawInput)?.second
        if (timeSub != null) return Pair("餐饮", timeSub)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val fallback = when {
            hour in 5..9 -> "早餐"
            hour in 10..13 -> "午餐"
            hour in 14..16 -> "饮品"
            hour in 17..20 -> "晚餐"
            else -> "夜宵"
        }
        return Pair("餐饮", fallback)
    }

    /**
     * 记忆写入时自动降级：餐饮时段分类→正餐。
     */
    fun normalizeCategoryForMemory(category: String, subcategory: String?): String? {
        if (category != "餐饮" || subcategory == null) return subcategory
        val timeSubs = setOf("早餐", "午餐", "晚餐", "夜宵", "饮品")
        return if (subcategory in timeSubs) "正餐" else subcategory
    }

    /**
     * 构造完整解析 Prompt（未命中记忆时使用）。
     * 内嵌 Top20 记忆规则，要求 AI 严格遵守。
     */
    private suspend fun buildFullPrompt(rawInput: String): Pair<String, String> {
        // 同时给出支出与收入的 Top20 记忆，便于 AI 跨类型判断
        val topMemories = memoryDao.getTop20ByType("expense") +
                memoryDao.getTop20ByType("income")
        val memoryRules = if (topMemories.isEmpty()) {
            "（暂无已知分类规则）"
        } else {
            topMemories.joinToString("\n") { m ->
                val sub = m.subcategory?.let { "-$it" } ?: ""
                "${m.triggerWord} → ${m.category}$sub"
            }
        }

        val systemPrompt = """你是专业记账助手，只输出标准JSON，绝对不能有任何解释、多余文字、markdown格式。
从用户输入中提取记账信息，严格遵守以下规则：

1. 收支类型规则
type字段只能填 expense（支出） 或 income（收入）
- 支出分类只能选：餐饮、交通、购物、居家、娱乐、通讯、医疗、教育、其他
- 收入分类只能选：工资、奖金、红包、报销、退款、投资收益、兼职收入、其他收入
请根据整句话语义判断收支类型，再选择对应分类，禁止跨类型选分类。

2. 分类规则
一级分类只能从以下列表选择：餐饮、交通、购物、居家、娱乐、通讯、医疗、教育、其他（支出）
                                工资、奖金、红包、报销、退款、投资收益、兼职收入、其他收入（收入）
二级分类在一级分类下合理细分，比如餐饮下有早餐、午餐、晚餐、外卖、奶茶等

【已知分类规则（必须100%遵守，禁止自行修改）】
$memoryRules

3. 时间规则
- 默认时间为今天当前时间
- 用户提到"昨天/前天/上周/上周三/早上8点/昨天下午"等自然语言时间，请准确换算为标准时间
- 时间格式为yyyy-MM-dd HH:mm:ss

4. 输出JSON结构
{
  "type": "expense或income",
  "amount": 数字,
  "category": "一级分类",
  "subcategory": "二级分类或null",
  "merchant": "商家名称或null",
  "time": "yyyy-MM-dd HH:mm:ss",
  "note": "备注或null",
  "confidence": 0到1之间的数字
}""".trimIndent()

        val userPrompt = "用户输入：$rawInput"
        return systemPrompt to userPrompt
    }

    /**
     * 构造补全 Prompt（命中记忆时使用）。
     * 仅请求 AI 补全金额、商家、时间、备注、置信度，
     * type/category/subcategory 由调用方使用记忆值锁死。
     */
    private fun buildCompletionPrompt(rawInput: String): Pair<String, String> {
        val systemPrompt = """你是记账信息补全助手，只输出标准JSON，禁止多余内容。
请从用户输入中提取金额、商家、时间、备注信息。

时间默认今天当前时间，用户提到相对时间请准确换算。
输出JSON结构：
{
  "amount": 数字,
  "merchant": "商家或null",
  "time": "yyyy-MM-dd HH:mm:ss",
  "note": "备注或null",
  "confidence": 0到1
}""".trimIndent()

        val userPrompt = "用户输入：$rawInput"
        return systemPrompt to userPrompt
    }

    /**
     * 调用 DeepSeek Chat Completion API。
     *
     * 节点5「AI请求发起」埋点：打印模型名称、Prompt长度、请求开始时间；API Key 脱敏后打印。
     * 节点6「AI响应返回」埋点：打印响应状态、返回内容长度、关键片段摘要；错误时打印错误码和错误信息。
     *
     * @param requestId 请求唯一ID（无默认值，调用方必须传入）
     * @return AI 返回的文本内容
     * @throws Exception 未配置 API Key、HTTP 错误、内容为空时抛出
     */
    private suspend fun callDeepSeek(model: String, systemPrompt: String, userPrompt: String, requestId: String): String {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            AppLogger.e(requestId, "AI请求发起", "未配置 API Key，模型：$model", null)
            throw Exception("未配置 API Key，请在设置页配置")
        }

        val maskedKey = AppLogger.maskApiKey(apiKey)
        val startTime = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(startTime)
        AppLogger.i(
            requestId,
            "AI请求发起",
            "模型：$model，Prompt长度：${systemPrompt.length + userPrompt.length}字符 " +
                    "（system=${systemPrompt.length}，user=${userPrompt.length}），开始时间：$timeStr，API Key：$maskedKey"
        )

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            )
        )

        return try {
            val response = deepSeekApi.chatCompletion("Bearer $apiKey", request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: throw Exception("AI 返回内容为空")
                val preview = if (content.length > 100) content.substring(0, 100) + "..." else content
                AppLogger.d(requestId, "AI响应返回", "状态：成功，返回长度：${content.length}字符，摘要：$preview")
                content
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                AppLogger.e(
                    requestId,
                    "AI响应返回",
                    "状态：失败，错误码：$code，错误信息：${response.message()}，错误体摘要：${errBody.take(200)}",
                    null
                )
                throw Exception("API 请求失败：$code")
            }
        } catch (e: Exception) {
            // 异常已被上面各分支覆盖；这里仅兜底
            throw e
        }
    }

    /**
     * JSON 解析 + 字段校验。
     *
     * 处理 AI 返回可能带 ```json``` 标记或多余文字的情况：
     * 1. 清理 markdown 包裹与多余文本
     * 2. 解析为 JsonObject
     * 3. 校验：amount > 0、time 为合法时间格式
     *    type/category 仅在字段存在时校验（补全 Prompt 不含这些字段）
     * 4. 校验失败抛异常，由上层走 Failure 流程
     */
    private fun parseAndValidate(jsonStr: String, requestId: String): JsonObject {
        // 1. 清理 markdown 包裹与多余文本
        val cleaned = cleanJsonString(jsonStr)

        // 2. 解析为 JsonObject
        val json = try {
            Gson().fromJson(cleaned, JsonObject::class.java)
        } catch (e: Exception) {
            AppLogger.e(requestId, "AI响应返回", "JSON 解析失败：${e.message}，原始内容：${jsonStr.take(200)}", e)
            throw Exception("JSON 解析失败：${e.message}")
        }

        // 3. amount 校验（必填，> 0）
        if (!json.has("amount") || json.get("amount").isJsonNull) {
            AppLogger.e(requestId, "AI响应返回", "缺少金额字段", null)
            throw Exception("缺少金额字段")
        }
        val amountElement = json.get("amount")
        val amountYuan = if (amountElement.isJsonPrimitive && amountElement.asJsonPrimitive.isNumber) {
            amountElement.asDouble
        } else {
            amountElement.asString.toDoubleOrNull()
                ?: throw Exception("金额格式错误：$amountElement")
        }
        if (amountYuan <= 0) {
            AppLogger.e(requestId, "AI响应返回", "金额必须大于 0：$amountYuan", null)
            throw Exception("金额必须大于 0")
        }

        // 4. type 校验（仅在字段存在时）
        if (json.has("type") && !json.get("type").isJsonNull) {
            val type = json.get("type").asString
            if (type != "expense" && type != "income") {
                AppLogger.e(requestId, "AI响应返回", "收支类型非法：$type", null)
                throw Exception("收支类型非法：$type")
            }
        }

        // 5. category 校验（仅在字段存在时）
        if (json.has("category") && !json.get("category").isJsonNull) {
            if (json.get("category").asString.isBlank()) {
                AppLogger.e(requestId, "AI响应返回", "分类不能为空", null)
                throw Exception("分类不能为空")
            }
        }

        // 6. time 校验（必填，合法时间格式）
        if (!json.has("time") || json.get("time").isJsonNull) {
            AppLogger.e(requestId, "AI响应返回", "缺少时间字段", null)
            throw Exception("缺少时间字段")
        }
        val timeStr = json.get("time").asString
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            sdf.isLenient = false
            sdf.parse(timeStr) ?: throw Exception("时间格式错误")
        } catch (e: Exception) {
            AppLogger.e(requestId, "AI响应返回", "时间格式错误：$timeStr", e)
            throw Exception("时间格式错误：$timeStr")
        }

        return json
    }

    /**
     * 清理 AI 返回内容：剥离 ```json``` 包裹与多余文字，
     * 仅保留第一个 { 到最后一个 } 之间的内容。
     */
    private fun cleanJsonString(raw: String): String {
        var s = raw.trim()
        // 剥离 ```json 或 ``` 前缀
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").trim()
            if (s.endsWith("```")) {
                s = s.removeSuffix("```").trim()
            }
        }
        // 提取第一个 { 到最后一个 }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start in 0 until end) {
            s = s.substring(start, end + 1)
        }
        return s
    }

    /**
     * 从 JSON 提取金额（元）并转换为分。
     * 调用前已通过 parseAndValidate 校验，此处不再重复检查。
     */
    private fun extractAmount(json: JsonObject): Long {
        val amountElement = json.get("amount")
        val yuan = if (amountElement.isJsonPrimitive && amountElement.asJsonPrimitive.isNumber) {
            amountElement.asDouble
        } else {
            amountElement.asString.toDouble()
        }
        return (yuan * 100).toLong()
    }

    /** 安全读取可为空的字符串字段：null 或 JSON null 均返回 null */
    private fun JsonObject.getStringOrNull(key: String): String? {
        val element = this.get(key) ?: return null
        if (element.isJsonNull) return null
        return element.asString
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
            val monthStart = TimeUtils.getMonthStart()
            val futureEnd = TimeUtils.now() + 86_400_000L

            val monthExpense = expenseDao.getSumByTimeRange(monthStart, futureEnd).first() ?: 0
            val monthIncome = incomeDao.getSumByTimeRange(monthStart, futureEnd).first() ?: 0
            val topExpense = expenseDao.getCategoryStats(monthStart, futureEnd).first()
                .sortedByDescending { it.totalAmount }.take(3)
            val recentExpenses = expenseDao.getRecent(5).first()
            val recentIncomes = incomeDao.getRecent(5).first()

            val statsLines = mutableListOf<String>()
            statsLines.add("本月总支出：${AmountUtils.fenToYuan(monthExpense)}元，本月总收入：${AmountUtils.fenToYuan(monthIncome)}元")
            if (topExpense.isNotEmpty()) {
                statsLines.add("本月支出Top3分类：")
                topExpense.forEachIndexed { i, stat ->
                    statsLines.add("${i + 1}. ${stat.category}：${AmountUtils.fenToYuan(stat.totalAmount)}元")
                }
            }
            val recentAll = (recentExpenses.map { "支出 ${it.merchant ?: ""} ${it.category} ¥${AmountUtils.fenToYuan(it.amount)} ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(it.time)}" } +
                    recentIncomes.map { "收入 ${it.merchant ?: ""} ${it.category} ¥${AmountUtils.fenToYuan(it.amount)} ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(it.time)}" })
                .sortedByDescending { it.takeLast(19) }.take(5)
            if (recentAll.isNotEmpty()) {
                statsLines.add("最近5条记录：")
                recentAll.forEachIndexed { i, line -> statsLines.add("${i + 1}. $line") }
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
                        "开始时间：$timeStr，API Key：$maskedKey"
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
}
