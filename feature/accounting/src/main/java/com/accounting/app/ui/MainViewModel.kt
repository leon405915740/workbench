package com.accounting.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.accounting.app.data.local.entity.CategoryMappingEntity
import com.accounting.app.data.local.entity.CategoryMemoryEntity
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import com.accounting.app.data.local.pref.PersistedMessage
import com.accounting.app.data.repository.AppRepository
import com.accounting.app.data.repository.ParseResult
import com.accounting.app.domain.classification.CategoryService
import com.accounting.app.ui.model.AppTab
import com.accounting.app.ui.model.ChatMessage
import com.accounting.app.ui.model.DashTab
import com.accounting.app.ui.model.EditDialogData
import com.accounting.app.ui.model.LearnDialogData
import com.accounting.app.ui.model.MemoryGroup
import com.accounting.app.ui.model.RecentRecord
import com.accounting.app.ui.model.UiState
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
import kotlinx.coroutines.flow.combine
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
        observeAutoLearn()
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.d("", "MainViewModel", "ViewModel 已清除")
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
                }
            }
        } else {
            _uiState.update { it.copy(currentTab = tab) }
        }
    }

    fun switchDashTab(tab: DashTab) {
        _uiState.update { it.copy(dashTab = tab) }
    }

    // ===================== 核心消息发送（记账页：仅记账） =====================

    /**
     * 发送消息：金额检测 → 记账流程 / 提示引导。
     * 1. 含金额 → parseAccountingInput() 记账流程（入库 + 卡片）
     * 2. 不含金额 → 提示用户输入记账内容或到统计页查询
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

            // 记账意图检测
            AppLogger.d(requestId, "记账意图检测", "开始检测: $rawInput")
            val hasAmount = AmountUtils.containsAmount(rawInput)
            AppLogger.d(requestId, "记账意图检测", "结果: $hasAmount")
            if (hasAmount) {
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
                val successResults = results.filterIsInstance<ParseResult.Success>()
                val allFailed = results.isNotEmpty() && results.all { it is ParseResult.Failure }
                val allSuccessButFallback = successResults.isNotEmpty() &&
                    successResults.size == results.size &&
                    successResults.all { it.source in setOf("fallback", "ai_fallback") }
                if (allFailed || allSuccessButFallback) {
                    // 规则全部失败或全部 fallback → AccountingAiParser 兜底（展示预确认弹窗）
                    val candidate = try {
                        withContext(Dispatchers.IO) {
                            repository.parseWithAccountingAi(rawInput, requestId)
                        }
                    } catch (e: Exception) {
                        AppLogger.e(requestId, "AccountingAiParser", "调用异常: ${e.message}", e)
                        null
                    }
                    if (candidate != null) {
                        AppLogger.d(requestId, "AccountingAiParser", "AI候选已生成，展示确认弹窗: type=${candidate.type}, category=${candidate.category}, amount=${candidate.amount}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showEditDialog = EditDialogData(
                                    recordId = null,
                                    pendingRequestId = requestId,
                                    type = candidate.type,
                                    category = candidate.category,
                                    subcategory = null,
                                    merchant = candidate.description,
                                    rawInput = rawInput,
                                    amount = candidate.amount,
                                    time = TimeUtils.now(),
                                    note = candidate.note,
                                    originalCategory = ""
                                )
                            )
                        }
                        return@launch
                    } else {
                        AppLogger.d(requestId, "AccountingAiParser", "AI解析失败，返回null")
                        _uiState.update {
                            it.copy(
                                messages = it.messages + ChatMessage.AiTextMessage(
                                    "无法识别记账内容，请输入如：午饭 25 元",
                                    TimeUtils.now()
                                ),
                                isLoading = false
                            )
                        }
                        persistChatHistory()
                        return@launch
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
                            if (!result.matchedMemory && result.source != "mapping" && !result.merchant.isNullOrBlank()) {
                                AppLogger.d(requestId, "学习弹窗", "触发词未命中记忆/映射，标记待学习: source=${result.source}, merchant=${result.merchant}")
                                lastUnmatchedCard = card
                            } else {
                                AppLogger.d(requestId, "学习弹窗", "跳过学习弹窗: matchedMemory=${result.matchedMemory}, source=${result.source}")
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
                    // 记账成功提示（单笔/多笔统一反馈，供财务主布局无聊天卡片时使用）
                    _uiState.update {
                        it.copy(toast = when {
                            successCount > 1 -> "已自动拆分 ${successCount} 笔记账"
                            successCount == 1 -> "已记账"
                            else -> null
                        })
                    }
                }
                persistChatHistory()
                return@launch
            }

            // 非记账输入：提示用户
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage.AiTextMessage(
                        "请输入记账内容（如：午饭 25 元），如需查询数据请到统计页",
                        TimeUtils.now()
                    ),
                    isLoading = false
                )
            }
            persistChatHistory()
            return@launch
        }
    }

    // ===================== 统计页聊天查询 =====================

    fun updateDashboardInput(text: String) {
        _uiState.update { it.copy(dashboardInputText = text) }
    }

    /** 财务主布局搜索关键词（不打断输入法组合态，由 UI 层在组合结束后触发） */
    fun updateFinanceSearch(query: String) {
        _uiState.update { it.copy(financeSearchQuery = query) }
    }

    fun sendDashboardQuery() {
        val rawInput = _uiState.value.dashboardInputText.trim()
        if (rawInput.isBlank()) return

        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "sendDashboardQuery", "start, input=$rawInput")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dashboardMessages = it.dashboardMessages + ChatMessage.UserMessage(rawInput, TimeUtils.now()),
                    dashboardInputText = "",
                    dashboardIsLoading = true
                )
            }

            try {
                val reply = withContext(Dispatchers.IO) {
                    repository.chatQuery(rawInput, requestId)
                }
                AppLogger.d(requestId, "sendDashboardQuery", "success, replyLength=${reply.length}")
                _uiState.update {
                    it.copy(
                        dashboardMessages = it.dashboardMessages + ChatMessage.AiTextMessage(reply, TimeUtils.now()),
                        dashboardIsLoading = false
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "sendDashboardQuery", "error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        dashboardMessages = it.dashboardMessages + ChatMessage.AiTextMessage(
                            "查询失败，请稍后重试",
                            TimeUtils.now()
                        ),
                        dashboardIsLoading = false
                    )
                }
            } finally {
                _uiState.update { it.copy(dashboardIsLoading = false) }
            }
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
                originalCategory = record.category,
                attachmentPath = record.attachmentPath,
                originalAttachmentPath = record.attachmentPath
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

            // 附件处理：仅在路径变化时写库（Repository 内清理被替换的旧文件）
            if (updatedData.attachmentPath != updatedData.originalAttachmentPath) {
                try {
                    withContext(Dispatchers.IO) {
                        if (updatedData.type == "expense") {
                            repository.updateExpenseAttachment(recordId, updatedData.attachmentPath, requestId)
                        } else {
                            repository.updateIncomeAttachment(recordId, updatedData.attachmentPath, requestId)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(requestId, "编辑账单", "附件更新失败：${e.message}", e)
                }
            }

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
                    // 双写：同步写入 category_mapping 表（失败仅记 WARNING，不打断用户「已更新」体验）
                    writeCategoryMappingSafely(requestId, "编辑账单", triggerWord, updatedData.type, updatedData.category)
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

    /** 财务主布局 AI 录入入口：直接以自然语言文本走现有解析/确认/入库链路 */
    fun submitAiEntry(text: String) {
        if (text.isBlank()) return
        updateInputText(text)
        sendMessage()
    }

    /** 付款后唤起：根据通知解析出的金额/商家预填记账弹窗（type 固定支出） */
    fun openPaymentQuickEntry(amount: Long, merchant: String?) {
        val label = merchant?.takeIf { it.isNotBlank() } ?: "快捷记账"
        val requestId = AppLogger.generateRequestId()
        AppLogger.d(requestId, "付款唤起", "节点1 入口: requestId=$requestId, amount=${amount}分, merchant=$label")
        _uiState.update { state ->
            state.copy(
                currentTab = AppTab.CHAT,
                showEditDialog = EditDialogData(
                    recordId = null,
                    type = "expense",
                    category = "",
                    subcategory = null,
                    merchant = label,
                    rawInput = label,
                    amount = amount,
                    time = TimeUtils.now(),
                    note = null,
                    originalCategory = ""
                )
            )
        }
    }

    fun submitManualEntry(
        type: String, amount: Long, category: String,
        merchant: String?, time: Long, note: String?, rawInput: String,
        pendingRequestId: String? = null,
        attachmentPath: String? = null
    ) {
        // 手动记账：优先复用 AI 兜底透传的 requestId，否则单独生成
        val requestId = pendingRequestId ?: AppLogger.generateRequestId()
        val sourceTag = if (pendingRequestId != null) ", source=ai_confirmed" else ""
        AppLogger.d(requestId, "入口接收", "手动记账，用户输入：$rawInput$sourceTag")

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
            if (attachmentPath != null) {
                try {
                    withContext(Dispatchers.IO) {
                        if (type == "expense") {
                            repository.updateExpenseAttachment(recordId, attachmentPath, requestId)
                        } else {
                            repository.updateIncomeAttachment(recordId, attachmentPath, requestId)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(requestId, "入库执行", "附件写入失败：${e.message}", e, billIndex)
                }
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
                // 双写：同步写入 category_mapping 表（失败仅记 WARNING，不打断用户「记账成功」体验）
                writeCategoryMappingSafely(requestId, "手动记账", triggerWord, type, category)
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
        // 财务主布局：收支合并，按时间倒序（全部账单）
        viewModelScope.launch {
            combine(
                repository.getAllExpenses(),
                repository.getAllIncomes()
            ) { expenses, incomes ->
                (expenses.map { it.toRecentRecord("expense") } + incomes.map { it.toRecentRecord("income") })
                    .sortedByDescending { it.time }
            }.collect { merged ->
                _uiState.update { it.copy(financeRecords = merged) }
            }
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
                // 双写：同步写入 category_mapping 表（失败仅记 WARNING，不打断用户「保存成功」体验）
                writeCategoryMappingSafely(requestId, "确认学习关键词", triggerWord, type, cleanCategory)
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

    /** 切换「记账后自动弹出学习确认」开关（持久化到 UserPreferences） */
    fun toggleAutoLearn() {
        val newValue = !_uiState.value.autoLearnEnabled
        _uiState.update { it.copy(autoLearnEnabled = newValue) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getUserPreferences().setAutoLearn(newValue)
            }
        }
    }

    /** 观察自动学习开关（工作台设置页修改后，记账侧即时同步） */
    private fun observeAutoLearn() {
        viewModelScope.launch {
            repository.getUserPreferences().getAutoLearn().collect { enabled ->
                _uiState.update { it.copy(autoLearnEnabled = enabled) }
            }
        }
    }

    // ===================== 分类映射管理 =====================

    fun loadMappings() {
        viewModelScope.launch {
            val mappings = withContext(Dispatchers.IO) { repository.getAllMappings() }
            val expenseRoots = withContext(Dispatchers.IO) { CategoryService.getRootCategories("expense") }
            val incomeRoots = withContext(Dispatchers.IO) { CategoryService.getRootCategories("income") }
            val expenseSubs = expenseRoots.associate { root ->
                root.id to withContext(Dispatchers.IO) { CategoryService.getSubcategoriesByParentId(root.id) }
                    .map { it.name to it.id }
            }
            val incomeSubs = incomeRoots.associate { root ->
                root.id to withContext(Dispatchers.IO) { CategoryService.getSubcategoriesByParentId(root.id) }
                    .map { it.name to it.id }
            }
            _uiState.update {
                it.copy(
                    mappings = mappings,
                    expenseRootCategories = expenseRoots.map { c -> c.name to c.id },
                    incomeRootCategories = incomeRoots.map { c -> c.name to c.id },
                    expenseSubcategories = expenseSubs,
                    incomeSubcategories = incomeSubs
                )
            }
        }
    }

    fun switchMappingTab(tab: String) {
        _uiState.update { it.copy(currentMappingTab = tab) }
    }

    fun showAddMappingDialog() {
        _uiState.update { it.copy(showAddMappingDialog = true) }
    }

    fun dismissAddMappingDialog() {
        _uiState.update { it.copy(showAddMappingDialog = false) }
    }

    fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?) {
        if (keyword.isBlank()) return
        val requestId = AppLogger.generateRequestId()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.upsertMapping(CategoryMappingEntity(
                        keyword = keyword, type = type, categoryId = categoryId,
                        subcategoryId = subcategoryId, source = "MANUAL",
                        enabled = true, hitCount = 0,
                        createdAt = TimeUtils.now(), updatedAt = TimeUtils.now(),
                        lastHitAt = null
                    ), requestId)
                }
                _uiState.update { it.copy(showAddMappingDialog = false, toast = "映射已添加") }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "新增映射", "stage=error, error=${e.message}", e)
                _uiState.update { it.copy(toast = "新增映射失败：${e.message}") }
            }
        }
    }

    fun updateMapping(id: Long, keyword: String, categoryId: Long, subcategoryId: Long?) {
        if (keyword.isBlank()) return
        val requestId = AppLogger.generateRequestId()
        viewModelScope.launch {
            try {
                val existing = withContext(Dispatchers.IO) {
                    repository.getAllMappings().firstOrNull { it.id == id }
                } ?: return@launch
                withContext(Dispatchers.IO) {
                    repository.upsertMapping(existing.copy(
                        keyword = keyword, categoryId = categoryId, subcategoryId = subcategoryId,
                        updatedAt = TimeUtils.now()
                    ), requestId)
                }
                _uiState.update { it.copy(toast = "映射已更新") }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "更新映射", "stage=error, error=${e.message}", e)
                _uiState.update { it.copy(toast = "更新映射失败：${e.message}") }
            }
        }
    }

    fun deleteMapping(id: Long) {
        val requestId = AppLogger.generateRequestId()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteMappingById(id, requestId) }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "删除映射", "stage=error, error=${e.message}", e)
            }
        }
    }

    fun toggleMappingEnabled(id: Long, enabled: Boolean) {
        val requestId = AppLogger.generateRequestId()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.updateMappingEnabled(id, enabled, requestId) }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "切换映射", "stage=error, error=${e.message}", e)
            }
        }
    }

    fun promoteMappingToManual(id: Long) {
        val requestId = AppLogger.generateRequestId()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.promoteMappingToManual(id, requestId) }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "提升映射", "stage=error, error=${e.message}", e)
            }
        }
    }

    fun cleanStaleAuto() {
        val requestId = AppLogger.generateRequestId()
        // 清理 30 天未命中的自动映射
        val beforeTime = TimeUtils.now() - 30L * 24 * 60 * 60 * 1000
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    repository.cleanStaleAutoMappings(beforeTime, requestId)
                }
                _uiState.update { it.copy(toast = "已清理 $count 条无效自动映射") }
                loadMappings()
            } catch (e: Exception) {
                AppLogger.e(requestId, "清理映射", "stage=error, error=${e.message}", e)
            }
        }
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

    /**
     * 双写补偿：在记忆表（CategoryMemory）写入成功后，同步写入映射表（category_mapping）。
     *
     * 约束：
     * - categoryId 通过 [CategoryService.getCategoryByName] 反查；查不到则跳过映射写入并记 WARNING
     * - upsertMapping 失败仅记 WARNING，不抛异常、不打断用户「保存成功」体验
     * - 记忆表是主表，映射表写入失败不影响学习成功语义
     */
    private suspend fun writeCategoryMappingSafely(
        requestId: String,
        node: String,
        triggerWord: String,
        type: String,
        category: String
    ) {
        val categoryId = try {
            CategoryService.getCategoryByName(type, category)?.id
        } catch (e: Exception) {
            AppLogger.w(requestId, "映射写入", "requestId=$requestId, node=$node, action=UPSERT_MAPPING, stage=skip, reason=categoryLookupError, type=$type, category=$category, keyword=$triggerWord, error=${e.message}")
            return
        }
        if (categoryId == null) {
            AppLogger.w(requestId, "映射写入", "requestId=$requestId, node=$node, action=UPSERT_MAPPING, stage=skip, reason=categoryNotFound, type=$type, category=$category, keyword=$triggerWord")
            return
        }
        try {
            repository.upsertMapping(
                CategoryMappingEntity(
                    keyword = triggerWord,
                    type = type,
                    categoryId = categoryId,
                    subcategoryId = null,
                    source = "AUTO",
                    enabled = true,
                    createdAt = TimeUtils.now(),
                    updatedAt = TimeUtils.now(),
                    lastHitAt = null
                ),
                requestId
            )
        } catch (e: Exception) {
            AppLogger.w(requestId, "映射写入", "requestId=$requestId, node=$node, action=UPSERT_MAPPING, stage=fallback, result=warning, type=$type, keyword=$triggerWord, categoryId=$categoryId, error=${e.message}")
        }
    }

    private fun ExpenseEntity.toRecentRecord(type: String) = RecentRecord(
        id = id, type = type, amount = amount, category = category,
        subcategory = subcategory, merchant = merchant, time = time,
        note = note, confidence = confidence, matchedMemory = false, rawInput = rawInput,
        attachmentPath = attachmentPath
    )

    private fun IncomeEntity.toRecentRecord(type: String) = RecentRecord(
        id = id, type = type, amount = amount, category = category,
        subcategory = subcategory, merchant = merchant, time = time,
        note = note, confidence = confidence, matchedMemory = false, rawInput = rawInput,
        attachmentPath = attachmentPath
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
