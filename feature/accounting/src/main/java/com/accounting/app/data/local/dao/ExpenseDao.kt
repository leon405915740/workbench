package com.accounting.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.accounting.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * 支出记录数据访问对象。
 */
@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE time >= :startTime AND time < :endTime ORDER BY time DESC")
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense WHERE time >= :startTime AND time < :endTime ORDER BY time DESC LIMIT :limit")
    fun getByTimeRangeWithLimit(startTime: Long, endTime: Long, limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense ORDER BY time DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense ORDER BY time DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("UPDATE expense SET category = :category, subcategory = :subcategory WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String, subcategory: String?): Int

    /**
     * 按 id 全字段更新支出（不修改 confidence / rawInput / createdAt）。
     * 返回受影响行数：1=更新成功，0=id 不存在。
     */
    @Query("UPDATE expense SET amount = :amount, category = :category, subcategory = :subcategory, merchant = :merchant, time = :time, note = :note WHERE id = :id")
    suspend fun updateAllFields(id: Long, amount: Long, category: String, subcategory: String?, merchant: String?, time: Long, note: String?): Int

    @Query("UPDATE expense SET attachmentPath = :path WHERE id = :id")
    suspend fun updateAttachment(id: Long, path: String?): Int

    @Query("SELECT SUM(amount) FROM expense WHERE time >= :startTime AND time < :endTime")
    fun getSumByTimeRange(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expense WHERE time >= :startTime AND time < :endTime GROUP BY category ORDER BY totalAmount DESC")
    fun getCategoryStats(startTime: Long, endTime: Long): Flow<List<CategoryAmount>>

    @Query("SELECT COUNT(*) FROM expense")
    suspend fun count(): Int
}
