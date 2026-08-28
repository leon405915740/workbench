package com.accounting.app.domain.classification

import com.accounting.app.data.local.dao.CategoryDao
import com.accounting.app.data.local.entity.CategoryEntity
import com.accounting.app.log.AppLogger
import java.util.concurrent.ConcurrentHashMap

object CategoryService {
    private var categoryDao: CategoryDao? = null
    private val categoryCache = ConcurrentHashMap<Long, CategoryEntity>()
    private val rootCategoryCache = ConcurrentHashMap<String, List<CategoryEntity>>()
    private val subcategoryCache = ConcurrentHashMap<Long, List<CategoryEntity>>()

    fun init(dao: CategoryDao) {
        categoryDao = dao
    }

    suspend fun loadCache() {
        val dao = categoryDao ?: return
        val allCategories = dao.getCategoriesByType("expense") + dao.getCategoriesByType("income")
        categoryCache.clear()
        rootCategoryCache.clear()
        subcategoryCache.clear()

        allCategories.forEach { category ->
            categoryCache[category.id] = category
        }

        val expenseRoots = allCategories.filter { it.type == "expense" && it.parentId == null }
        val incomeRoots = allCategories.filter { it.type == "income" && it.parentId == null }
        rootCategoryCache["expense"] = expenseRoots
        rootCategoryCache["income"] = incomeRoots

        expenseRoots.forEach { root ->
            subcategoryCache[root.id] = allCategories.filter { it.parentId == root.id }
        }
        incomeRoots.forEach { root ->
            subcategoryCache[root.id] = allCategories.filter { it.parentId == root.id }
        }
    }

    suspend fun getRootCategories(type: String): List<CategoryEntity> {
        val cached = rootCategoryCache[type]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }
        val dao = categoryDao ?: return emptyList()
        val result = dao.getRootCategories(type)
        rootCategoryCache[type] = result
        result.forEach { categoryCache[it.id] = it }
        return result
    }

    suspend fun getSubcategoriesByParentId(parentId: Long): List<CategoryEntity> {
        val cached = subcategoryCache[parentId]
        if (cached != null) {
            return cached
        }
        val dao = categoryDao ?: return emptyList()
        val result = dao.getSubcategories(parentId)
        subcategoryCache[parentId] = result
        result.forEach { categoryCache[it.id] = it }
        return result
    }

    suspend fun getCategoryById(id: Long): CategoryEntity? {
        val cached = categoryCache[id]
        if (cached != null) {
            return cached
        }
        val dao = categoryDao ?: return null
        val result = dao.getCategoryById(id)
        if (result != null) {
            categoryCache[id] = result
        }
        return result
    }

    suspend fun getRootCategoryByName(type: String, name: String): CategoryEntity? {
        val roots = getRootCategories(type)
        return roots.find { it.name == name }
    }

    suspend fun getSubcategoryByName(name: String, parentId: Long): CategoryEntity? {
        val subs = getSubcategoriesByParentId(parentId)
        return subs.find { it.name == name }
    }

    suspend fun getCategoryByName(type: String, name: String): CategoryEntity? {
        val sanitized = name.replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]"), "")
        if (sanitized.length < 2) return null

        val roots = getRootCategories(type)
        val exactMatch = roots.find { it.name == sanitized }
        if (exactMatch != null) return exactMatch

        val fuzzyMatches = roots.filter { it.name.contains(sanitized) }
        if (fuzzyMatches.isEmpty()) return null

        return fuzzyMatches
            .sortedWith(compareBy({ it.name.length }, { it.sortOrder }, { it.id }))
            .first()
    }

    /**
     * 添加分类。
     *
     * 注意：当前直接调用 CategoryDao 写入数据库，未经过 PlanExecutor.execute()。
     * 原因是 PlanExecutor 专为 expense/income 账单操作设计，不支持 category 领域。
     * 如需统一写入入口，需扩展 PlanExecutor / BillTransaction 支持 category 操作。
     */
    @Deprecated("直接写入 DAO，未经过 PlanExecutor，未来应重构为统一写入入口")
    suspend fun addCategory(entity: CategoryEntity) {
        AppLogger.w("", "CategoryService", "addCategory 直接写入 DAO（绕过 PlanExecutor）")
        val dao = categoryDao ?: return
        val id = dao.insertCategory(entity)
        val newEntity = entity.copy(id = id)
        categoryCache[id] = newEntity
        loadCache()
    }

    /**
     * 更新分类。
     *
     * 注意：当前直接调用 CategoryDao 写入数据库，未经过 PlanExecutor.execute()。
     * 原因是 PlanExecutor 专为 expense/income 账单操作设计，不支持 category 领域。
     * 如需统一写入入口，需扩展 PlanExecutor / BillTransaction 支持 category 操作。
     */
    @Deprecated("直接写入 DAO，未经过 PlanExecutor，未来应重构为统一写入入口")
    suspend fun updateCategory(category: CategoryEntity) {
        AppLogger.w("", "CategoryService", "updateCategory 直接写入 DAO（绕过 PlanExecutor）")
        val dao = categoryDao ?: return
        dao.updateCategory(category)
        categoryCache[category.id] = category
        loadCache()
    }

    /**
     * 删除分类。
     *
     * 注意：当前直接调用 CategoryDao 写入数据库，未经过 PlanExecutor.execute()。
     * 原因是 PlanExecutor 专为 expense/income 账单操作设计，不支持 category 领域。
     * 如需统一写入入口，需扩展 PlanExecutor / BillTransaction 支持 category 操作。
     */
    @Deprecated("直接写入 DAO，未经过 PlanExecutor，未来应重构为统一写入入口")
    suspend fun deleteCategory(id: Long) {
        AppLogger.w("", "CategoryService", "deleteCategory 直接写入 DAO（绕过 PlanExecutor）")
        val dao = categoryDao ?: return
        dao.deleteCategory(id)
        categoryCache.remove(id)
        loadCache()
    }
}