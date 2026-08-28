package com.accounting.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.accounting.app.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE type = :type AND parentId IS NULL ORDER BY sortOrder ASC")
    suspend fun getRootCategories(type: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    suspend fun getSubcategories(parentId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND name = :name AND parentId IS NULL")
    suspend fun getRootCategoryByName(type: String, name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND parentId = :parentId")
    suspend fun getSubcategoryByName(name: String, parentId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM categories WHERE id = :id)")
    suspend fun categoryExists(id: Long): Boolean
}