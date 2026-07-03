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
import com.accounting.app.ui.model.ManualEntryData
import com.accounting.app.ui.model.MemoryGroup
import com.accounting.app.ui.model.RecentRecord
import com.accounting.app.ui.model.SubGroup
import com.accounting.app.ui.model.UiState
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.AppLogger
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
        _uiState.update { it.copy(currentTab = tab) }
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
        AppLogger.d(requestId, "入口接收", "用户输入：$rawInput")

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
                val reply = repository.chatQuery(rawInput, requestId)
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

                val results = repository.parseAccountingInput(rawInput, requestId)
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
                            val recordId = if (result.type == "expense") {
                                try {
                                    repository.insertExpense(ExpenseEntity(
                                        amount = result.amount, category = result.category,
                                        subcategory = result.subcategory, merchant = result.merchant,
                                        time = result.time, note = result.note,
                                        confidence = result.confidence, rawInput = rawInput,
                                        createdAt = TimeUtils.now()
                                    ), requestId, billIndex)
                                } catch (e: Exception) {
                                    AppLogger.e(requestId, "入库执行", "单笔入库异常：${e.message}", e, billIndex)
                                    failInsertCount++
                                    continue
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
                                    AppLogger.e(requestId, "入库执行", "单笔入库异常：${e.message}", e, billIndex)
                                    failInsertCount++
                                    continue
                                }
                            }
                            successInsertCount++
                            if (result.matchedMemory && result.memoryId != null) {
                                repository.incrementMemoryHitCount(result.memoryId)
                            }
                            val card = ChatMessage.CardMessage(
                                recordId = recordId, type = result.type, amount = result.amount,
                                category = result.category, subcategory = result.subcategory,
                                merchant = result.merchant, recordTime = result.time,
                                confidence = result.confidence, matchedMemory = result.matchedMemory,
                                rawInput = rawInput, timestamp = TimeUtils.now()
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
            val reply = repository.chatQuery(rawInput, requestId)
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
                category = message.category, subcategory = message.subcategory,
                merchant = message.merchant, rawInput = message.rawInput
            ))
        }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = null) }
    }

    fun confirmEditCategory(newCategory: String, newSubcategory: String?) {
        val dialog = _uiState.value.showEditDialog ?: return
        viewModelScope.launch {
            if (dialog.type == "expense") {
                repository.updateExpenseCategory(dialog.recordId, newCategory, newSubcategory)
            } else {
                repository.updateIncomeCategory(dialog.recordId, newCategory, newSubcategory)
            }
            val triggerWord = extractTriggerWord(dialog.merchant)
            if (triggerWord != null) {
                val normalizedSub = repository.normalizeCategoryForMemory(newCategory, newSubcategory)
                repository.upsertMemory(CategoryMemoryEntity(
                    triggerWord = triggerWord, type = dialog.type,
                    category = newCategory, subcategory = normalizedSub,
                    createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                ))
                val typeLabel = if (dialog.type == "expense") "支出" else "收入"
                val subLabel = newSubcategory?.let { "-$it" } ?: ""
                _uiState.update {
                    it.copy(toast = "✅ 已学习（$typeLabel）：$triggerWord → $newCategory$subLabel")
                }
            }
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg is ChatMessage.CardMessage && msg.recordId == dialog.recordId) {
                            msg.copy(category = newCategory, subcategory = newSubcategory)
                        } else msg
                    },
                    showEditDialog = null
                )
            }
        }
    }

    // ===================== 删除记录 =====================

    fun deleteRecord(recordId: Long, type: String) {
        viewModelScope.launch {
            if (type == "expense") repository.deleteExpense(recordId)
            else repository.deleteIncome(recordId)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.filterNot {
                        it is ChatMessage.CardMessage && it.recordId == recordId
                    },
                    toast = "已删除记录"
                )
            }
        }
    }

    // ===================== 手动记账 =====================

    fun openManualEntry(prefillNote: String = "") {
        _uiState.update { it.copy(showManualEntry = ManualEntryData(prefillNote = prefillNote)) }
    }

    fun dismissManualEntry() {
        _uiState.update { it.copy(showManualEntry = null) }
    }

    fun submitManualEntry(
        type: String, amount: Long, category: String, subcategory: String?,
        merchant: String?, time: Long, note: String?, rawInput: String
    ) {
        // 手动记账：单独生成 requestId
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "入口接收", "手动记账，用户输入：$rawInput")

        viewModelScope.launch {
            val billIndex = 1
            val recordId = try {
                if (type == "expense") {
                    repository.insertExpense(ExpenseEntity(
                        amount = amount, category = category, subcategory = subcategory,
                        merchant = merchant, time = time, note = note,
                        confidence = 1.0f, rawInput = rawInput, createdAt = TimeUtils.now()
                    ), requestId, billIndex)
                } else {
                    repository.insertIncome(IncomeEntity(
                        amount = amount, category = category, subcategory = subcategory,
                        merchant = merchant, time = time, note = note,
                        confidence = 1.0f, rawInput = rawInput, createdAt = TimeUtils.now()
                    ), requestId, billIndex)
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "入库执行", "手动记账入库异常：${e.message}", e, billIndex)
                return@launch
            }
            val triggerWord = extractTriggerWord(merchant)
            if (triggerWord != null) {
                val normalizedSub = repository.normalizeCategoryForMemory(category, subcategory)
                repository.upsertMemory(CategoryMemoryEntity(
                    triggerWord = triggerWord, type = type,
                    category = category, subcategory = normalizedSub,
                    createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
                ))
            }
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage.CardMessage(
                        recordId = recordId, type = type, amount = amount,
                        category = category, subcategory = subcategory,
                        merchant = merchant, recordTime = time,
                        confidence = 1.0f, matchedMemory = false,
                        rawInput = rawInput, timestamp = TimeUtils.now()
                    ),
                    showManualEntry = null, toast = "记账成功"
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

    /** 收支类型一级分类固定展示顺序 */
    private val expenseCategoryOrder = listOf("餐饮", "交通", "购物", "居家", "通讯", "娱乐", "医疗", "教育", "其他")
    private val incomeCategoryOrder = listOf("工资", "奖金", "红包", "报销", "退款", "投资收益", "兼职收入", "其他收入")

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
                        memorySearchQuery = ""
                    )
                }
            }
        }
    }

    /** 组装分组数据：按一级→二级分类分组，空分组隐藏，词条排序 user/seed→updatedAt */
    private fun buildGroups(type: String, memories: List<CategoryMemoryEntity>): List<MemoryGroup> {
        val catOrder = if (type == "income") incomeCategoryOrder else expenseCategoryOrder
        val groups = mutableListOf<MemoryGroup>()
        for (cat in catOrder) {
            val subGroups = mutableListOf<SubGroup>()
            val catMemories = memories.filter { it.category == cat }
            val subNames = catMemories.map { it.subcategory }.distinct()
            for (sub in subNames) {
                val items = catMemories
                    .filter { it.subcategory == sub }
                    .sortedWith(compareByDescending<CategoryMemoryEntity> { it.source == "user" }
                        .thenByDescending { it.updatedAt })
                if (items.isNotEmpty()) {
                    subGroups.add(SubGroup(subName = sub, items = items))
                }
            }
            if (subGroups.isNotEmpty()) {
                groups.add(MemoryGroup(categoryName = cat, subGroups = subGroups))
            }
        }
        return groups
    }

    /** 搜索过滤记忆词条 */
    fun onMemorySearch(query: String) {
        _uiState.update { it.copy(memorySearchQuery = query) }
        val rawMemories = _uiState.value.allMemories
        val filtered = if (query.isBlank()) rawMemories
        else rawMemories.filter { it.triggerWord.contains(query, ignoreCase = true) }
        val groups = buildGroups(
            if (_uiState.value.allMemories.firstOrNull()?.type == "income") "income" else "expense",
            filtered
        )
        _uiState.update {
            it.copy(
                memoryGroups = groups,
                expandedCategories = if (query.isNotBlank()) groups.map { g -> g.categoryName }.toSet()
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
    fun addMemory(triggerWord: String, type: String, category: String, subcategory: String?) {
        viewModelScope.launch {
            val normalizedSub = repository.normalizeCategoryForMemory(category, subcategory)
            repository.upsertMemory(CategoryMemoryEntity(
                triggerWord = triggerWord, type = type,
                category = category, subcategory = normalizedSub,
                createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
            ))
            _uiState.update { it.copy(toast = "已添加记忆：$triggerWord") }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.deleteAllMemories()
            _uiState.update { it.copy(toast = "已清空所有记忆") }
        }
    }

    fun restoreDefaultMemories() {
        viewModelScope.launch {
            repository.reseedMemories()
            _uiState.update { it.copy(toast = "已恢复默认记忆") }
        }
    }

    // ===================== API Key =====================

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            repository.setApiKey(key)
            _uiState.update { it.copy(savedApiKey = key, toast = "API Key 已保存并生效") }
        }
    }

    // ===================== CSV 导出 =====================

    fun prepareCsvExport() {
        viewModelScope.launch {
            val expenses = repository.getAllExpenses().first()
            val incomes = repository.getAllIncomes().first()
            _uiState.update { it.copy(csvExportData = CsvUtils.generateCsv(expenses, incomes)) }
        }
    }

    fun clearCsvExportData() {
        _uiState.update { it.copy(csvExportData = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    // ===================== 关键词学习 =====================

    /** 打开关键词学习确认弹窗（从卡片「保存关键词」按钮或自动弹窗触发） */
    fun openLearnDialog(message: ChatMessage.CardMessage) {
        if (message.matchedMemory || message.merchant.isNullOrBlank()) return
        if (!repository.isValidTriggerWord(message.merchant)) return
        _uiState.update {
            it.copy(showLearnDialog = LearnDialogData(
                triggerWord = message.merchant,
                type = message.type,
                category = message.category,
                subcategory = message.subcategory
            ))
        }
    }

    /** 关闭关键词学习弹窗 */
    fun dismissLearnDialog() {
        _uiState.update { it.copy(showLearnDialog = null) }
    }

    /** 确认学习：将触发词+分类写入记忆库，时段分类自动降级为正餐 */
    fun confirmLearnKeyword(triggerWord: String, type: String, category: String, subcategory: String?) {
        viewModelScope.launch {
            val normalizedSub = repository.normalizeCategoryForMemory(category, subcategory)
            repository.upsertMemory(CategoryMemoryEntity(
                triggerWord = triggerWord, type = type,
                category = category, subcategory = normalizedSub,
                createdAt = TimeUtils.now(), updatedAt = TimeUtils.now()
            ))
            val typeLabel = if (type == "expense") "支出" else "收入"
            val subLabel = subcategory?.let { "-$it" } ?: ""
            _uiState.update {
                it.copy(
                    showLearnDialog = null,
                    toast = "✅ 已学习（$typeLabel）：$triggerWord → $category$subLabel"
                )
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
        confidence = confidence, matchedMemory = false, rawInput = rawInput
    )

    private fun IncomeEntity.toRecentRecord(type: String) = RecentRecord(
        id = id, type = type, amount = amount, category = category,
        subcategory = subcategory, merchant = merchant, time = time,
        confidence = confidence, matchedMemory = false, rawInput = rawInput
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
