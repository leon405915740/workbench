package com.accounting.app

import com.accounting.app.ui.model.MappingItemUi
import com.accounting.app.ui.model.MemoryItemUi
import kotlinx.coroutines.flow.Flow

interface AccountingBridge {
    fun getMonthlyExpense(): Flow<Double?>
    fun isAutoLearnEnabled(): Flow<Boolean>
    suspend fun setAutoLearnEnabled(enabled: Boolean, requestId: String)
    fun isQuickRecordEnabled(): Flow<Boolean>
    suspend fun setQuickRecordEnabled(enabled: Boolean, requestId: String)
    fun getMemories(type: String): Flow<List<MemoryItemUi>>
    suspend fun addMemory(triggerWord: String, type: String, category: String, requestId: String)
    suspend fun deleteMemory(id: Long, requestId: String)
    suspend fun clearAllMemories(requestId: String)
    suspend fun restoreDefaultMemories(requestId: String)
    fun getMappingsBySource(source: String): Flow<List<MappingItemUi>>
    suspend fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?, requestId: String)
    suspend fun deleteMapping(id: Long, requestId: String)
    suspend fun toggleMappingEnabled(id: Long, enabled: Boolean, requestId: String)
    suspend fun promoteMappingToManual(id: Long, requestId: String)
    suspend fun cleanStaleAutoMappings(requestId: String)
    suspend fun prepareCsvExport(): String?
    suspend fun prepareLogExport(): String?
    fun getExpenseCategories(): List<Pair<String, Long>>
    fun getIncomeCategories(): List<Pair<String, Long>>
}
