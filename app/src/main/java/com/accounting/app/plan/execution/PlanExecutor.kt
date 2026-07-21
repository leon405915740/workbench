package com.accounting.app.plan.execution

import androidx.room.withTransaction
import com.accounting.app.data.local.database.AppDatabase
import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.log.AppLogger
import com.accounting.app.plan.model.ExecuteResult
import com.accounting.app.plan.validator.PlanValidator
import com.accounting.app.plan.validator.ValidationResult

class PlanExecutor(
    private val database: AppDatabase,
    private val billTransaction: BillTransaction
) {

    suspend fun execute(plan: BillExecutePlan): ExecuteResult {
        val validationResult = PlanValidator.validate(plan, plan.requestId)
        if (validationResult is ValidationResult.Failure) {
            AppLogger.e(plan.requestId, "入库执行", "计划校验失败：${validationResult.reason}", null)
            return ExecuteResult.Failure(validationResult.reason)
        }

        return try {
            var successCount = 0
            val failedItems = mutableListOf<String>()
            val insertedIds = mutableListOf<Long>()

            database.withTransaction {
                for ((index, item) in plan.items.withIndex()) {
                    val billIndex = index + 1
                    try {
                        val id = billTransaction.execute(item, plan.rawInput, plan.requestId, billIndex)
                        insertedIds.add(id)
                        successCount++
                    } catch (e: Exception) {
                        val msg = "第${billIndex}笔失败：${e.message}"
                        AppLogger.e(plan.requestId, "入库执行", msg, e, billIndex)
                        failedItems.add(msg)
                    }
                }

                if (failedItems.isNotEmpty()) {
                    throw Exception("部分执行失败：${failedItems.joinToString("; ")}")
                }
            }

            AppLogger.i(plan.requestId, "入库执行-汇总",
                "总条数：${plan.totalCount}，成功：$successCount，失败：0")
            ExecuteResult.Success(successCount, insertedIds)
        } catch (e: Exception) {
            AppLogger.e(plan.requestId, "入库执行", "事务执行失败：${e.message}", e)
            ExecuteResult.Failure(e.message ?: "执行失败")
        }
    }
}