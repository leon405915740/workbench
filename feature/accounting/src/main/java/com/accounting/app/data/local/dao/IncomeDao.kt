package com.accounting.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.accounting.app.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 收入记录数据访问对象。
 *
 * 方法签名与 ExpenseDao 完全一致，仅操作 income 表。
 */
@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Delete
    suspend fun delete(income: IncomeEntity)

    @Query("DELETE FROM income WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM income WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    @Query("SELECT * FROM income WHERE time >= :startTime AND time < :endTime ORDER BY time DESC")
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE time >= :startTime AND time < :endTime ORDER BY time DESC LIMIT :limit")
    fun getByTimeRangeWithLimit(startTime: Long, endTime: Long, limit: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income ORDER BY time DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income ORDER BY time DESC")
    fun getAll(): Flow<List<IncomeEntity>>

    @Query("UPDATE income SET category = :category, subcategory = :subcategory WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String, subcategory: String?): Int

    /**
     * 按 id 全字段更新收入（不修改 confidence / rawInput / createdAt）。
     * 返回受影响行数：1=更新成功，0=id 不存在。
     */
    @Query("UPDATE income SET amount = :amount, category = :category, subcategory = :subcategory, merchant = :merchant, time = :time, note = :note WHERE id = :id")
    suspend fun updateAllFields(id: Long, amount: Long, category: String, subcategory: String?, merchant: String?, time: Long, note: String?): Int

    @Query("SELECT SUM(amount) FROM income WHERE time >= :startTime AND time < :endTime")
    fun getSumByTimeRange(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT category, SUM(amount) as totalAmount FROM income WHERE time >= :startTime AND time < :endTime GROUP BY category ORDER BY totalAmount DESC")
    fun getCategoryStats(startTime: Long, endTime: Long): Flow<List<CategoryAmount>>

    @Query("SELECT COUNT(*) FROM income")
    suspend fun count(): Int
}
