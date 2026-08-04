package com.accounting.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import com.accounting.app.data.local.pref.PersistedMessage
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.data.repository.ParseResult
import com.accounting.app.ui.model.AppTab
import com.accounting.app.ui.model.ChatMessage
import com.accounting.app.ui.model.DashTab
import com.accounting.app.ui.model.EditDialogData
import com.accounting.app.ui.model.LearnDialogData
import com.accounting.app.ui.model.MemoryGroup
import com.accounting.app.ui.model.RecentRecord
import com.accounting.app.ui.model.UiState
import com.accounting.app.capture.model.PaymentInfo
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.CategoryConstants
import com.accounting.app.log.AppLogger
import com.accounting.app.util.CsvUtils
import com.accounting.app.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var memoryJob: Job? = null

    init {
        observeStats()
        restoreChatHistory()
        loadSavedApiKey()
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.d("", "MainViewModel", "ViewModel 已清除")
    }

    // ===================== 自动采集（识别付款弹窗记账） =====================

    /**
     * 收到来自 PaymentAccessibilityService 的 Intent 跳转（前台化）后，
     * 由 MainActivity.onNewIntent/onCreate 调用，预填记账确认弹窗。
     * 不直接入库，保证用户对每笔记账有掌控。
     */
    fun onPaymentCapturedFromIntent(info: PaymentInfo) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(
            requestId, "AutoCapture_Intent",
            "收到支付信息：金额=${info.amount}分，商户=${info.merchant}，来源=${info.source}"
        )
        _uiState.update {
            it.copy(
                showEditDialog = EditDialogData(
                    recordId = null,
                    type = "expense",
                    category = "",
                    subcategory = null,
                    merchant = info.merchant,
                    rawInput = "自动采集",
                    amount = info.amount ?: 0L,
                    time = info.payTime ?: 0L,
                    note = null,
                    originalCategory = ""
                )
            )
        }
    }

    /** 加载已保存的 API Key 到 UI 状态 */
    private fun loadSavedApiKey() {
        viewModelScope.launch {
            val key = repository.getApiKey()
            _uiState.update { it.copy(savedApiKey = key) }
        }
    }

    /** 从 DataStore 恢复聊天记录（仅恢复文本消息，不恢复卡片） */
    private fun restoreChatHistory() {
        viewModelScope.launch {
            repository.getUserPreferences().getChatHistory().first().forEach { p ->
                when (p.type) {
                    "user" -> _uiState.update { it.copy(messages = it.messages + ChatMessage.UserMessage(p.text, p.timestamp)) }
                    "ai" -> _uiState.update { it.copy(messages = it.messages + ChatMessage.AiMessage(p.text, p.timestamp)) }
                    "aiText" -> _uiState.update { it.copy(messages = it.messages + ChatMessage.AiTextMessage(p.text, p.timestamp)) }
                }
            }
        }
    }

    /** 将当前消息列表持久化到 DataStore */
    private fun persistChatHistory() {
        val all = _uiState.value.messages.mapNotNull { msg ->
            when (msg) {
                is ChatMessage.UserMessage -> PersistedMessage("user", msg.text, "", msg.timestamp)
                is ChatMessage.AiMessage -> PersistedMessage("ai", msg.text, "", msg.timestamp)
                is ChatMessage.AiTextMessage -> PersistedMessage("aiText", msg.content, "", msg.timestamp)
                is ChatMessage.ErrorMessage -> PersistedMessage("ai", msg.text, msg.rawInput, msg.timestamp)
                is ChatMessage.CardMessage -> null
            }
        }
        viewModelScope.launch {
            repository.getUserPreferences().saveChatHistory(all)
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun switchTab(tab: AppTab) {
        val current = _uiState.value.currentTab
        if (tab == current) {
            // 重复点击同一Tab → 触发"回到顶部"信号
            _uiState.update { state ->
                when (tab) {
                    AppTab.CHAT -> state.copy(chatResetSignal = state.chatResetSignal + 1)
                    AppTab.DASHBOARD -> state.copy(dashResetSignal = state.dashResetSignal + 1)
                    AppTab.SETTINGS -> state // 设置页暂无滚动需求
                }
            }
        } else {
            _uiState.update { it.copy(currentTab = tab) }
        }
    }

    fun switchDashTab(tab: DashTab) {
        _uiState.update { it.copy(dashTab = tab) }
    }

    // ===================== 核心消息发送（三级分流） =====================

    /**
     * 发送消息：疑问检测 → 金额检测 → 兜底对话。
     * 1. 疑问特征词/问号 → chatQuery() 对话流程
     * 2. 含金额 → parseAccountingInput() 记账流程
     * 3. 都不是 → chatQuery() 普通对话
     *
     * 节点1「入口接收」埋点：第一行生成 requestId 并打印用户原始输入文本。
     * requestId 贯穿整条请求所有下游调用，多笔拆分场景下逐笔透传 billIndex。
     */
    fun sendMessage() {
        val rawInput = _uiState.value.inputText.trim()
        if (rawInput.isBlank()) return

        // 节点1：入口生成 requestId 并打印原始输入（第一行！）
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "入口接收", "requestId=$requestId, action=SEND_MESSAGE, stage=start, input=$rawInput")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage.UserMessage(rawInput, TimeUtils.now()),
                    inputText = "",
                    isLoading = true
                )
            }

            // 三级分流判断
            if (AmountUtils.isQuestionInput(rawInput)) {
                // 疑问 → 对话查询
                val reply = try {
                    withContext(Dispatchers.IO) {
                        repository.chatQuery(rawInput, requestId)
                    }
                } catch (e: Exception) {
                    AppLogger.e(requestId, "对话查询", "requestId=$requestId, action=CHAT_QUERY, stage=error, result=failure, error=${e.message}", e)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage.ErrorMessage(
                                text = "查询失败：${e.message}", rawInput = rawInput,
                                timestamp = TimeUtils.now()
                            ),
                            isLoading = false, toast = e.message
                        )
                    }
                    persistChatHistory()
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage.AiTextMessage(reply, TimeUtils.now()),
                        isLoading = false
                    )
                }
                persistChatHistory()
                return@launch
            }

            if (AmountUtils.containsAmount(rawInput)) {
                // 含金额 → 记账流程（支持多笔拆分）
                // 检查金额数量上限
                val segmentCount = AmountUtils.extractAmounts(rawInput, requestId).size
                if (segmentCount > 10) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage.ErrorMessage(
                                text = "内容过长，请分批次输入", rawInput = rawInput,
                                timestamp = TimeUtils.now()
                            ),
                            isLoading = false
                        )
                    }
                    persistChatHistory()
                    return@launch
                }

                val results = try {
                    withContext(Dispatchers.IO) {
                        repository.parseAccountingInput(rawInput, requestId)
                    }
                } catch (e: Exception) {
                    AppLogger.e(requestId, "解析执行", "requestId=$requestId, action=PARSE, stage=error, result=failure, error=${e.message}", e)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage.ErrorMessage(
                                text = "解析失败：${e.message}", rawInput = rawInput,
                                timestamp = TimeUtils.now()
                            ),
                            isLoading = false, toast = e.message
                        )
                    }
                    persistChatHistory()
                    return@launch
                }
                val successCount = results.count { it is ParseResult.Success }
                val failures = results.filterIsInstance<ParseResult.Failure>()
                if (failures.isNotEmpty() && results.all { it is ParseResult.Failure }) {
                    // 全部失败
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage.ErrorMessage(
                                text = "解析失败：${failures.first().reason}", rawInput = rawInput,
                                timestamp = TimeUtils.now()
                            ),
                            isLoading = false, toast = failures.first().reason
                        )
                    }
                } else {
                    // 批量入库：单笔明细 + 汇总日志
                    var successInsertCount = 0
                    var failInsertCount = 0
                    val totalCount = results.size
                    var lastUnmatchedCard: ChatMessage.CardMessage? = null
                    for ((index, result) in results.withIndex()) {
                        val billIndex = index + 1
                        if (result is ParseResult.Success) {
                            val recordId = withContext(Dispatchers.IO) {
                                if (result.type == "expense") {
                                    try {
                                        repository.insertExpense(ExpenseEntity(
                                        amount = result.amount, category = result.category,
                                        subcategory = result.subcategory, merchant = result.merchant,
                                        time = result.time, note = result.note,
                                        confidence = result.confidence, rawInput = rawInput,
                                        createdAt = TimeUtils.now()
                                    ), requestId, billIndex)
                                } catch (e: Exception) {
                                    AppLogger.e(requestId, "入库执行", "requestId=$requestId, action=INSERT, stage=error, result=failure, billIndex=$billIndex, type=expense, error=${e.message}", e, billIndex)
                                    null
                                }
                                } else {
                                    try {
                                        repository.insertIncome(IncomeEntity(
                                        amount = result.amount, category = result.category,
                                        subcategory = result.subcategory, merchant = result.merchant,
                                        time = result.time, note = result.note,
                                        confidence = result.confidence, rawInput = rawInput,
                                        createdAt = TimeUtils.now()
                                    ), requestId, billIndex)
                                } catch (e: Exception) {
                                    AppLogger.e(requestId, "入库执行", "requestId=$requestId, action=INSERT, stage=error, result=failure, billIndex=$billIndex, type=income, error=${e.message}", e, billIndex)
                                    null
                                }
                                }
                            }
                            if (recordId == null) {
                                failInsertCount++
                                continue
                            }
                            successInsertCount++
                            if (result.matchedMemory && result.memoryId != null) {
                                try {
                                    withContext(Dispatchers.IO) {
                                        repository.incrementMemoryHitCount(result.memoryId, requestId)
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e(requestId, "记忆命中", "requestId=$requestId, action=INCREMENT_MEMORY_HIT, stage=error, result=failure, id=${result.memoryId}, error=${e.message}", e)
                                }
                            }
                            val card = ChatMessage.CardMessage(
                                recordId = recordId, type = result.type, amount = result.amount,
                                category = result.category, subcategory = result.subcategory,
                                merchant = result.merchant, recordTime = result.time,
                                note = result.note, confidence = result.confidence,
                                matchedMemory = result.matchedMemory,
                                rawInput = rawInput, source = result.source, timestamp = TimeUtils.now()
                            )
                            _uiState.update { it.copy(messages = it.messages + card) }
                            if (!result.matchedMemory && !result.merchant.isNullOrBlank()) {
                                lastUnmatchedCard = card
                            }
                        }
                    }
                    // 节点7 汇总日志（仅多笔场景下输出，单笔场景单笔明细已带结果）
                    if (totalCount > 1) {
                        AppLogger.i(
                            requestId,
                            "入库执行-汇总",
                            "总条数：$totalCount，成功：$successInsertCount，失败：$failInsertCount"
                        )
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    // 自动弹出学习确认
                    if (_uiState.value.autoLearnEnabled && lastUnmatchedCard != null) {
                        openLearnDialog(lastUnmatchedCard)
                    }
                    // 多笔拆分提示
                    if (successCount > 1) {
                        _uiState.update { it.copy(toast = "已自动拆分 ${successCount} 笔记账") }
                    }
                }
                persistChatHistory()
                return@launch
            }

            // 兜底：普通对话
            val reply = withContext(Dispatchers.IO) {
                repository.chatQuery(rawInput, requestId)
            }
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage.AiTextMessage(reply, TimeUtils.now()),
                    isLoading = false
                )
            }
            persistChatHistory()
        }
    }

    // ===================== 修改分类与记忆学习 =====================

    fun openEditDialog(message: ChatMessage.CardMessage) {
        _uiState.update {
            it.copy(showEditDialog = EditDialogData(
                recordId = message.recordId, type = message.type,
                category = message.category,
                subcategory = message.subcategory,
                merchant = message.merchant, rawInput = message.rawInput,
                amount = message.amount, time = message.recordTime,
                note = message.note,
                originalCategory = message.category
            ))
        }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = null) }
    }

    // ===================== 全字段编辑（统计页 + 记账页统一入口） =====================

    /**
     * 从统计页打开编辑弹窗。构造包含全字段的 [EditDialogData]，originalCategory 保存原分类用于记忆学习判断。
     */
    fun openEditDialogFromDashboard(record: RecentRecord) {
        _uiState.update {
            it.copy(showEditDialog = EditDialogData(
                recordId = record.id,
                type = record.type,
                category = record.category,
                subcategory = record.subcategory,
                merchant = record.merchant,
                rawInput = record.rawInput,
                amount = record.amount,
                time = record.time,
                note = record.note,
                originalCategory = record.category
            ))
        }
    }

    /**
     * 编辑模式提交：全字段更新数据库。所有上下文通过 [updatedData] 传入，不读取 UI State。
     *
     * - subcategory 传原值，避免覆盖
     * - 仅分类变更（category != originalCategory）且商家可提取触发词时触发记忆学习
     * - 同步 messages 列表对应 CardMessage（rawInput/confidence 保持不变）
     */
    fun confirmEditRecord(updatedData: EditDialogData) {
        val recordId = updatedData.recordId ?: return
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "编辑账单", "requestId=$requestId, action=UPDATE_FULL, stage=start, recordId=$recordId, type=${updatedData.type}, category=${updatedData.category}")

        viewModelScope.launch {
            val rowsAffected = try {
                withContext(Dispatchers.IO) {
                    if (updatedData.type == "expense") {
                        repository.updateExpenseFull(
                            recordId, updatedData.amount, updatedData.category,
                            updatedData.subcategory, updatedData.merchant,
                            updatedData.time, updatedData.note, requestId
                        )
                    } else {
                        repository.updateIncomeFull(
                            recordId, updatedData.amount, updatedData.category,
                            updatedData.subcategory, updatedData.merchant,
                            updatedData.time, updatedData.note, requestId
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "编辑账单", "requestId=$requestId, action=UPDATE_FULL, stage=error, result=failure, recordId=$recordId, error=${e.message}", e)
                _uiState.update { it.copy(error = e.message) }
                return@launch
            }

            if (rowsAffected == 0) {
                AppLogger.i(requestId, "编辑账单", "requestId=$requestId, action=UPDATE_FULL, stage=success, result=success, recordId=$recordId, rowsAffected=0")
                _uiState.update { it.copy(toast = "更新失败") }
                return@launch
            }

            AppLogger.i(requestId, "编辑账单", "requestId=$requestId, action=UPDATE_FULL, stage=success, result=success, recordId=$recordId, rowsAffected=$rowsAffected")

            // 记忆学习：仅分类变更时触发
            if (updatedData.category != updatedData.originalCategory) {
                val triggerWord = extractTriggerWord(updatedData.merchant)
                if (triggerWord != null) {
                    withContext(Dispatchers.IO) {
                        repository.upsertMemory(CategoryMemoryEntity(
                            triggerWord = triggerWord, type = updatedData.type,
                            category = updatedData.category, subcategory = null,
                            source = "auto",
                            createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                        ), requestId)
                    }
                    AppLogger.d(requestId, "编辑账单", "记忆学习触发：$triggerWord → ${updatedData.category}")
                }
            } else {
                AppLogger.d(requestId, "编辑账单", "分类未变更，跳过记忆学习")
            }

            // 同步 messages 列表（rawInput/confidence 保持不变）
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg is ChatMessage.CardMessage && msg.recordId == recordId) {
                            msg.copy(
                                amount = updatedData.amount,
                                category = updatedData.category,
                                subcategory = updatedData.subcategory,
                                merchant = updatedData.merchant,
                                recordTime = updatedData.time,
                                note = updatedData.note
                            )
                        } else msg
                    },
                    showEditDialog = null,
                    toast = "已更新"
                )
            }
        }
    }

    // ===================== 删除记录 =====================

    fun deleteRecord(recordId: Long, type: String) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "删除记录", "requestId=$requestId, action=DELETE_RECORD, stage=start, recordId=$recordId, type=$type")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (type == "expense") repository.deleteExpense(recordId, requestId)
                    else repository.deleteIncome(recordId, requestId)
                }
                AppLogger.i(requestId, "删除记录", "requestId=$requestId, action=DELETE_RECORD, stage=success, result=success, recordId=$recordId, type=$type")
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filterNot {
                            it is ChatMessage.CardMessage && it.recordId == recordId
                        },
                        toast = "已删除记录"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "删除记录", "requestId=$requestId, action=DELETE_RECORD, stage=error, result=failure, recordId=$recordId, type=$type, error=${e.message}", e)
            }
        }
    }

    // ===================== 手动记账 =====================

    fun openManualEntry(prefillNote: String = "") {
        _uiState.update {
            it.copy(showEditDialog = EditDialogData(
                recordId = null,
                type = "expense",
                category = "",
                subcategory = null,
                merchant = null,
                rawInput = prefillNote.ifBlank { "手动记账" },
                amount = 0L,
                time = 0L,
                note = prefillNote.takeIf { it.isNotBlank() },
                originalCategory = ""
            ))
        }
    }

    fun submitManualEntry(
        type: String, amount: Long, category: String,
        merchant: String?, time: Long, note: String?, rawInput: String
    ) {
        // 手动记账：单独生成 requestId
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "入口接收", "手动记账，用户输入：$rawInput")

        viewModelScope.launch {
            val billIndex = 1
            val recordId = try {
                withContext(Dispatchers.IO) {
                    if (type == "expense") {
                        repository.insertExpense(ExpenseEntity(
                            amount = amount, category = category, subcategory = null,
                            merchant = merchant, time = time, note = note,
                            confidence = 1.0f, rawInput = rawInput, createdAt = TimeUtils.now()
                        ), requestId, billIndex)
                    } else {
                        repository.insertIncome(IncomeEntity(
                            amount = amount, category = category, subcategory = null,
                            merchant = merchant, time = time, note = note,
                            confidence = 1.0f, rawInput = rawInput, createdAt = TimeUtils.now()
                        ), requestId, billIndex)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "入库执行", "手动记账入库异常：${e.message}", e, billIndex)
                return@launch
            }
            val triggerWord = extractTriggerWord(merchant)
            if (triggerWord != null) {
                withContext(Dispatchers.IO) {
                    repository.upsertMemory(CategoryMemoryEntity(
                        triggerWord = triggerWord, type = type,
                        category = category, subcategory = null,
                        source = "auto",
                        createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                    ), requestId)
                }
            }
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage.CardMessage(
                        recordId = recordId, type = type, amount = amount,
                        category = category, subcategory = null,
                        merchant = merchant, recordTime = time,
                        note = note, confidence = 1.0f, matchedMemory = false,
                        rawInput = rawInput, source = "manual", timestamp = TimeUtils.now()
                    ),
                    showEditDialog = null, toast = "记账成功"
                )
            }
        }
    }

    // ===================== 统计数据自动同步 =====================

    private fun observeStats() {
        val todayStart = TimeUtils.getTodayStart()
        val monthStart = TimeUtils.getMonthStart()
        val futureEnd = TimeUtils.now() + 86_400_000L

        viewModelScope.launch {
            repository.getExpenseSum(todayStart, futureEnd)
                .collect { sum -> _uiState.update { it.copy(todayExpense = sum ?: 0) } }
        }
        viewModelScope.launch {
            repository.getExpenseSum(monthStart, futureEnd)
                .collect { sum -> _uiState.update { it.copy(monthExpense = sum ?: 0) } }
        }
        viewModelScope.launch {
            repository.getIncomeSum(todayStart, futureEnd)
                .collect { sum -> _uiState.update { it.copy(todayIncome = sum ?: 0) } }
        }
        viewModelScope.launch {
            repository.getIncomeSum(monthStart, futureEnd)
                .collect { sum -> _uiState.update { it.copy(monthIncome = sum ?: 0) } }
        }
        viewModelScope.launch {
            _uiState.map { it.dashTab }.distinctUntilChanged().flatMapLatest { tab ->
                if (tab == DashTab.EXPENSE) repository.getExpenseCategoryStats(monthStart, futureEnd)
                else repository.getIncomeCategoryStats(monthStart, futureEnd)
            }.collect { stats -> _uiState.update { it.copy(categoryStats = stats) } }
        }
        viewModelScope.launch {
            _uiState.map { it.dashTab }.distinctUntilChanged().flatMapLatest { tab ->
                if (tab == DashTab.EXPENSE) repository.getRecentExpenses(20).map { list -> list.map { it.toRecentRecord("expense") } }
                else repository.getRecentIncomes(20).map { list -> list.map { it.toRecentRecord("income") } }
            }.collect { records -> _uiState.update { it.copy(recentRecords = records) } }
        }
    }

    // ===================== 记忆管理 =====================

    fun loadMemories(type: String) {
        memoryJob?.cancel()
        memoryJob = viewModelScope.launch {
            repository.getAllMemoriesByType(type).collect { memories ->
                val groups = buildGroups(type, memories)
                _uiState.update {
                    it.copy(
                        allMemories = memories,
                        memoryGroups = groups,
                        totalMemoryCount = memories.size,
                        expandedCategories = groups.map { g -> g.categoryName }.toSet(),
                        memorySearchQuery = "",
                        memorySourceFilter = ""
                    )
                }
            }
        }
    }

    /** 组装分组数据：按分类分组，空分组隐藏，词条排序 auto→user→seed，同级按 updatedAt */
    private fun buildGroups(type: String, memories: List<CategoryMemoryEntity>): List<MemoryGroup> {
        val catOrder = if (type == "income") CategoryConstants.incomeCategories else CategoryConstants.expenseCategories
        // 来源优先级：auto(自动学习) > user(手动添加) > seed(系统预置)
        val sourcePriority = mapOf("auto" to 0, "user" to 1, "seed" to 2)
        val groups = mutableListOf<MemoryGroup>()
        for (cat in catOrder) {
            val catMemories = memories.filter { it.category == cat }
            if (catMemories.isNotEmpty()) {
                val sorted = catMemories.sortedWith(
                    compareByDescending<CategoryMemoryEntity> { sourcePriority[it.source] ?: 99 }
                        .thenByDescending { it.updatedAt }
                )
                groups.add(MemoryGroup(categoryName = cat, items = sorted))
            }
        }
        return groups
    }

    /** 搜索过滤记忆词条 */
    fun onMemorySearch(query: String) {
        _uiState.update { it.copy(memorySearchQuery = query) }
        applyMemoryFilters()
    }

    /** 设置来源筛选（seed/auto/user/""=全部），与搜索叠加生效 */
    fun setMemorySourceFilter(source: String) {
        _uiState.update { it.copy(memorySourceFilter = source) }
        applyMemoryFilters()
    }

    /** 统一应用搜索+来源筛选，重建分组 */
    private fun applyMemoryFilters() {
        val state = _uiState.value
        val rawMemories = state.allMemories
        // 来源过滤
        val bySource = if (state.memorySourceFilter.isBlank()) rawMemories
        else rawMemories.filter { it.source == state.memorySourceFilter }
        // 关键词搜索（叠加在来源过滤之上）
        val filtered = if (state.memorySearchQuery.isBlank()) bySource
        else bySource.filter { it.triggerWord.contains(state.memorySearchQuery, ignoreCase = true) }
        val type = if (rawMemories.firstOrNull()?.type == "income") "income" else "expense"
        val groups = buildGroups(type, filtered)
        _uiState.update {
            it.copy(
                memoryGroups = groups,
                expandedCategories = if (state.memorySearchQuery.isNotBlank() || state.memorySourceFilter.isNotBlank())
                    groups.map { g -> g.categoryName }.toSet()
                else it.expandedCategories
            )
        }
    }

    /** 切换一级分类折叠状态 */
    fun toggleCategoryExpand(catName: String) {
        _uiState.update { state ->
            val newSet = state.expandedCategories.toMutableSet()
            if (catName in newSet) newSet.remove(catName) else newSet.add(catName)
            state.copy(expandedCategories = newSet)
        }
    }

    /** 手动新增记忆 */
    fun addMemory(triggerWord: String, type: String, category: String) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "新增记忆", "requestId=$requestId, action=ADD_MEMORY, stage=start, triggerWord=$triggerWord, type=$type, category=$category")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.upsertMemory(CategoryMemoryEntity(
                        triggerWord = triggerWord, type = type,
                        category = category, subcategory = null,
                        createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                    ), requestId)
                }
                AppLogger.i(requestId, "新增记忆", "requestId=$requestId, action=ADD_MEMORY, stage=success, result=success, triggerWord=$triggerWord, type=$type, category=$category")
                _uiState.update { it.copy(toast = "已添加记忆：$triggerWord") }
            } catch (e: Exception) {
                AppLogger.e(requestId, "新增记忆", "requestId=$requestId, action=ADD_MEMORY, stage=error, result=failure, triggerWord=$triggerWord, type=$type, category=$category, error=${e.message}", e)
            }
        }
    }

    fun deleteMemory(id: Long) {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "删除记忆", "requestId=$requestId, action=DELETE_MEMORY, stage=start, id=$id")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteMemory(id, requestId) }
                AppLogger.i(requestId, "删除记忆", "requestId=$requestId, action=DELETE_MEMORY, stage=success, result=success, id=$id")
            } catch (e: Exception) {
                AppLogger.e(requestId, "删除记忆", "requestId=$requestId, action=DELETE_MEMORY, stage=error, result=failure, id=$id, error=${e.message}", e)
            }
        }
    }

    fun clearAllMemories() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "清空记忆", "requestId=$requestId, action=CLEAR_ALL_MEMORIES, stage=start")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteAllMemories(requestId)
                }
                AppLogger.i(requestId, "清空记忆", "requestId=$requestId, action=CLEAR_ALL_MEMORIES, stage=success, result=success")
                _uiState.update { it.copy(toast = "已清空所有记忆") }
            } catch (e: Exception) {
                AppLogger.e(requestId, "清空记忆", "requestId=$requestId, action=CLEAR_ALL_MEMORIES, stage=error, result=failure, error=${e.message}", e)
            }
        }
    }

    fun restoreDefaultMemories() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "恢复默认记忆", "requestId=$requestId, action=RESTORE_DEFAULT_MEMORIES, stage=start")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.reseedMemories(requestId)
                }
                AppLogger.i(requestId, "恢复默认记忆", "requestId=$requestId, action=RESTORE_DEFAULT_MEMORIES, stage=success, result=success")
                _uiState.update { it.copy(toast = "已恢复默认记忆") }
            } catch (e: Exception) {
                AppLogger.e(requestId, "恢复默认记忆", "requestId=$requestId, action=RESTORE_DEFAULT_MEMORIES, stage=error, result=failure, error=${e.message}", e)
            }
        }
    }

    // ===================== API Key =====================

    fun saveApiKey(key: String) {
        val requestId = AppLogger.generateRequestId()
        val maskedKey = if (key.isBlank()) "<empty>" else AppLogger.maskApiKey(key)
        AppLogger.d(requestId, "保存API Key", "requestId=$requestId, action=SAVE_API_KEY, stage=start, key=$maskedKey")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.setApiKey(key, requestId)
                }
                AppLogger.i(requestId, "保存API Key", "requestId=$requestId, action=SAVE_API_KEY, stage=success, result=success, key=$maskedKey")
                _uiState.update { it.copy(savedApiKey = key, toast = "API Key 已保存并生效") }
            } catch (e: Exception) {
                AppLogger.e(requestId, "保存API Key", "requestId=$requestId, action=SAVE_API_KEY, stage=error, result=failure, key=$maskedKey, error=${e.message}", e)
            }
        }
    }

    // ===================== CSV 导出 =====================

    fun prepareCsvExport() {
        viewModelScope.launch {
            val (expenses, incomes) = withContext(Dispatchers.IO) {
                val expenses = repository.getAllExpenses().first()
                val incomes = repository.getAllIncomes().first()
                Pair(expenses, incomes)
            }
            _uiState.update { it.copy(csvExportData = CsvUtils.generateCsv(expenses, incomes)) }
        }
    }

    fun clearCsvExportData() {
        _uiState.update { it.copy(csvExportData = null) }
    }

    // ===================== 日志导出 =====================

    fun prepareLogExport() {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                AppLogger.getMergedLogFile()?.readText() ?: "暂无日志"
            }
            _uiState.update { it.copy(logExportData = content) }
        }
    }

    fun clearLogExportData() {
        _uiState.update { it.copy(logExportData = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    // ===================== 关键词学习 =====================

    /** 打开关键词学习确认弹窗（从卡片「保存关键词」按钮或自动弹窗触发） */
    fun openLearnDialog(message: ChatMessage.CardMessage) {
        if (message.matchedMemory || message.merchant.isNullOrBlank()) return
        if (!repository.isValidTriggerWord(message.merchant)) return
        val displayCategory = if (message.source == "ai_correction") "[AI推断] ${message.category}" else message.category
        _uiState.update {
            it.copy(showLearnDialog = LearnDialogData(
                triggerWord = message.merchant,
                type = message.type,
                category = displayCategory
            ))
        }
    }

    /** 关闭关键词学习弹窗 */
    fun dismissLearnDialog() {
        _uiState.update { it.copy(showLearnDialog = null) }
    }

    /** 确认学习：将触发词+分类写入记忆库 */
    fun confirmLearnKeyword(triggerWord: String, type: String, category: String) {
        val cleanCategory = category.removePrefix("[AI推断] ")
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "确认学习关键词", "requestId=$requestId, action=CONFIRM_LEARN_KEYWORD, stage=start, triggerWord=$triggerWord, type=$type, category=$cleanCategory")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.upsertMemory(CategoryMemoryEntity(
                        triggerWord = triggerWord, type = type,
                        category = cleanCategory, subcategory = null,
                        source = "auto",
                        createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                    ), requestId)
                }
                AppLogger.i(requestId, "确认学习关键词", "requestId=$requestId, action=CONFIRM_LEARN_KEYWORD, stage=success, result=success, triggerWord=$triggerWord, type=$type, category=$cleanCategory")
                val typeLabel = if (type == "expense") "支出" else "收入"
                _uiState.update {
                    it.copy(
                        showLearnDialog = null,
                        toast = "✅ 已学习（$typeLabel）：$triggerWord → $cleanCategory"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "确认学习关键词", "requestId=$requestId, action=CONFIRM_LEARN_KEYWORD, stage=error, result=failure, triggerWord=$triggerWord, type=$type, category=$cleanCategory, error=${e.message}", e)
            }
        }
    }

    /** 切换「记账后自动弹出学习确认」开关 */
    fun toggleAutoLearn() {
        _uiState.update { it.copy(autoLearnEnabled = !it.autoLearnEnabled) }
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 提取触发词：merchant 不为空 + 不在黑名单中才有效。
     * 通用动词/泛化词/疑问词命中黑名单直接返回 null，避免污染记忆库。
     */
    private fun extractTriggerWord(merchant: String?): String? {
        val word = merchant?.takeIf { it.isNotBlank() } ?: return null
        return if (repository.isValidTriggerWord(word)) word else null
    }

    private fun ExpenseEntity.toRecentRecord(type: String) = RecentRecord(
        id = id, type = type, amount = amount, category = category,
        subcategory = subcategory, merchant = merchant, time = time,
        note = note, confidence = confidence, matchedMemory = false, rawInput = rawInput
    )

    private fun IncomeEntity.toRecentRecord(type: String) = RecentRecord(
        id = id, type = type, amount = amount, category = category,
        subcategory = subcategory, merchant = merchant, time = time,
        note = note, confidence = confidence, matchedMemory = false, rawInput = rawInput
    )

    companion object {
        fun factory(repository: AppRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
        }
    }
}
