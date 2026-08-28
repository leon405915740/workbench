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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class AccountingBridgeImpl(
    private val repo: AppRepository,
    private val context: Context
) : AccountingBridge {

    private companion object {
        const val NODE = "AccountingBridge"
    }

    override fun getMonthlyExpense(): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()
        return repo.getExpenseSum(start, end).map { cents -> cents?.let { it / 100.0 } }
    }

    override fun isAutoLearnEnabled(): Flow<Boolean> =
        repo.getUserPreferences().getAutoLearn()

    override suspend fun setAutoLearnEnabled(enabled: Boolean, requestId: String) {
        AppLogger.i(requestId, NODE, "setAutoLearnEnabled 入口: enabled=$enabled")
        try {
            repo.getUserPreferences().setAutoLearn(enabled)
            AppLogger.d(requestId, NODE, "setAutoLearnEnabled 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "setAutoLearnEnabled 异常", e)
            throw e
        }
    }

    override fun isQuickRecordEnabled(): Flow<Boolean> =
        repo.getUserPreferences().getQuickRecordEnabled()

    override suspend fun setQuickRecordEnabled(enabled: Boolean, requestId: String) {
        AppLogger.i(requestId, NODE, "setQuickRecordEnabled 入口: enabled=$enabled")
        try {
            repo.getUserPreferences().setQuickRecordEnabled(enabled)
            AppLogger.d(requestId, NODE, "setQuickRecordEnabled 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "setQuickRecordEnabled 异常", e)
            throw e
        }
    }

    override fun getMemories(type: String): Flow<List<MemoryItemUi>> =
        repo.getAllMemoriesByType(type).map { entities ->
            entities.map { it.toUiModel() }
        }

    override suspend fun addMemory(triggerWord: String, type: String, category: String, requestId: String) {
        AppLogger.i(requestId, NODE, "addMemory 入口: triggerWord=$triggerWord, type=$type, category=$category")
        try {
            val memory = CategoryMemoryEntity(
                triggerWord = triggerWord,
                type = type,
                category = category,
                subcategory = null,
                hitCount = 1,
                source = "user",
                confidence = 100,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repo.upsertMemory(memory, requestId)
            AppLogger.d(requestId, NODE, "addMemory 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "addMemory 异常", e)
            throw e
        }
    }

    override suspend fun deleteMemory(id: Long, requestId: String) {
        AppLogger.i(requestId, NODE, "deleteMemory 入口: id=$id")
        try {
            repo.deleteMemory(id, requestId)
            AppLogger.d(requestId, NODE, "deleteMemory 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "deleteMemory 异常", e)
            throw e
        }
    }

    override suspend fun clearAllMemories(requestId: String) {
        AppLogger.i(requestId, NODE, "clearAllMemories 入口")
        try {
            repo.deleteAllMemories(requestId)
            AppLogger.d(requestId, NODE, "clearAllMemories 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "clearAllMemories 异常", e)
            throw e
        }
    }

    override suspend fun restoreDefaultMemories(requestId: String) {
        AppLogger.i(requestId, NODE, "restoreDefaultMemories 入口")
        try {
            repo.reseedMemories(requestId)
            AppLogger.d(requestId, NODE, "restoreDefaultMemories 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "restoreDefaultMemories 异常", e)
            throw e
        }
    }

    override fun getMappingsBySource(source: String): Flow<List<MappingItemUi>> = flow {
        val entities = repo.getMappingsBySource(source)
        emit(entities.map { it.toUiModel() })
    }

    override suspend fun addMapping(keyword: String, type: String, categoryId: Long, subcategoryId: Long?, requestId: String) {
        AppLogger.i(requestId, NODE, "addMapping 入口: keyword=$keyword, type=$type, categoryId=$categoryId, subcategoryId=$subcategoryId")
        try {
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
                lastHitAt = null
            )
            repo.upsertMapping(mapping, requestId)
            AppLogger.d(requestId, NODE, "addMapping 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "addMapping 异常", e)
            throw e
        }
    }

    override suspend fun deleteMapping(id: Long, requestId: String) {
        AppLogger.i(requestId, NODE, "deleteMapping 入口: id=$id")
        try {
            repo.deleteMappingById(id, requestId)
            AppLogger.d(requestId, NODE, "deleteMapping 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "deleteMapping 异常", e)
            throw e
        }
    }

    override suspend fun toggleMappingEnabled(id: Long, enabled: Boolean, requestId: String) {
        AppLogger.i(requestId, NODE, "toggleMappingEnabled 入口: id=$id, enabled=$enabled")
        try {
            repo.updateMappingEnabled(id, enabled, requestId)
            AppLogger.d(requestId, NODE, "toggleMappingEnabled 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "toggleMappingEnabled 异常", e)
            throw e
        }
    }

    override suspend fun promoteMappingToManual(id: Long, requestId: String) {
        AppLogger.i(requestId, NODE, "promoteMappingToManual 入口: id=$id")
        try {
            repo.promoteMappingToManual(id, requestId)
            AppLogger.d(requestId, NODE, "promoteMappingToManual 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "promoteMappingToManual 异常", e)
            throw e
        }
    }

    override suspend fun cleanStaleAutoMappings(requestId: String) {
        AppLogger.i(requestId, NODE, "cleanStaleAutoMappings 入口")
        try {
            repo.cleanStaleAutoMappings(System.currentTimeMillis(), requestId)
            AppLogger.d(requestId, NODE, "cleanStaleAutoMappings 出口: 成功")
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "cleanStaleAutoMappings 异常", e)
            throw e
        }
    }

    override suspend fun prepareCsvExport(): String? {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, NODE, "prepareCsvExport 入口")
        try {
            val expenses = repo.getAllExpenses().first()
            val incomes = repo.getAllIncomes().first()
            val csv = CsvUtils.generateCsv(expenses, incomes)
            AppLogger.d(requestId, NODE, "prepareCsvExport 出口: 成功, 数据条数=${expenses.size + incomes.size}")
            return csv
        } catch (e: Exception) {
            AppLogger.e(requestId, NODE, "prepareCsvExport 异常", e)
            throw e
        }
    }

    override suspend fun prepareLogExport(): String? {
        AppLogger.i("", NODE, "prepareLogExport 入口")
        return AppLogger.getMergedLogFile()?.readText().also {
            AppLogger.d("", NODE, "prepareLogExport 出口: ${if (it != null) "成功" else "无日志文件"}")
        }
    }

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
}
