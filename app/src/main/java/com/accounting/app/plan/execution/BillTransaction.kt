package com.accounting.app.plan.execution

import com.accounting.app.data.local.dao.ExpenseDao
import com.accounting.app.data.local.dao.IncomeDao
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import com.accounting.app.data.model.BillPlanItem
import com.accounting.app.data.model.PlanAction
import com.accounting.app.log.AppLogger
import com.accounting.app.parser.time.TimeUtils

class BillTransaction(
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao
) {

    suspend fun execute(item: BillPlanItem, rawInput: String, requestId: String, billIndex: Int): Long {
        return when (item.action) {
            PlanAction.ADD -> insert(item, rawInput, requestId, billIndex)
            PlanAction.UPDATE -> update(item, requestId, billIndex)
            PlanAction.DELETE -> delete(item, requestId, billIndex)
        }
    }

    private suspend fun insert(
        item: BillPlanItem,
        rawInput: String,
        requestId: String,
        billIndex: Int
    ): Long {
        val type = item.type
        val category = item.category
        AppLogger.d(
            requestId, "BillTransaction",
            "requestId=$requestId, action=INSERT, stage=start, id=${item.targetBillId}, type=$type, category=$category",
            billIndex
        )
        return try {
            val id = if (item.type == "expense") {
                expenseDao.insert(
                    ExpenseEntity(
                        amount = item.amount,
                        category = item.category,
                        subcategory = item.subCategory,
                        merchant = item.merchant,
                        time = item.billTime,
                        note = item.remark,
                        confidence = item.confidence,
                        rawInput = rawInput,
                        createdAt = TimeUtils.now()
                    )
                )
            } else {
                incomeDao.insert(
                    IncomeEntity(
                        amount = item.amount,
                        category = item.category,
                        subcategory = item.subCategory,
                        merchant = item.merchant,
                        time = item.billTime,
                        note = item.remark,
                        confidence = item.confidence,
                        rawInput = rawInput,
                        createdAt = TimeUtils.now()
                    )
                )
            }
            AppLogger.i(
                requestId, "BillTransaction",
                "requestId=$requestId, action=INSERT, stage=success, result=success, id=$id, type=$type, category=$category",
                billIndex
            )
            id
        } catch (e: Exception) {
            AppLogger.e(
                requestId, "BillTransaction",
                "requestId=$requestId, action=INSERT, stage=error, result=failure, id=${item.targetBillId}, type=$type, category=$category, error=${e.message}",
                e, billIndex
            )
            throw e
        }
    }

    private suspend fun update(item: BillPlanItem, requestId: String, billIndex: Int): Long {
        val type = item.type
        val category = item.category
        AppLogger.d(
            requestId, "BillTransaction",
            "requestId=$requestId, action=UPDATE, stage=start, id=${item.targetBillId}, type=$type, category=$category",
            billIndex
        )
        return try {
            val targetId = item.targetBillId ?: throw Exception("缺少目标账单ID")
            if (item.type == "expense") {
                expenseDao.updateCategory(targetId, item.category, item.subCategory)
            } else {
                incomeDao.updateCategory(targetId, item.category, item.subCategory)
            }
            AppLogger.i(
                requestId, "BillTransaction",
                "requestId=$requestId, action=UPDATE, stage=success, result=success, id=$targetId, type=$type, category=$category",
                billIndex
            )
            targetId
        } catch (e: Exception) {
            AppLogger.e(
                requestId, "BillTransaction",
                "requestId=$requestId, action=UPDATE, stage=error, result=failure, id=${item.targetBillId}, type=$type, category=$category, error=${e.message}",
                e, billIndex
            )
            throw e
        }
    }

    private suspend fun delete(item: BillPlanItem, requestId: String, billIndex: Int): Long {
        val type = item.type
        val category = item.category
        AppLogger.d(
            requestId, "BillTransaction",
            "requestId=$requestId, action=DELETE, stage=start, id=${item.targetBillId}, type=$type, category=$category",
            billIndex
        )
        return try {
            val targetId = item.targetBillId ?: throw Exception("缺少目标账单ID")
            if (item.type == "expense") {
                expenseDao.deleteById(targetId)
            } else {
                incomeDao.deleteById(targetId)
            }
            AppLogger.i(
                requestId, "BillTransaction",
                "requestId=$requestId, action=DELETE, stage=success, result=success, id=$targetId, type=$type, category=$category",
                billIndex
            )
            targetId
        } catch (e: Exception) {
            AppLogger.e(
                requestId, "BillTransaction",
                "requestId=$requestId, action=DELETE, stage=error, result=failure, id=${item.targetBillId}, type=$type, category=$category, error=${e.message}",
                e, billIndex
            )
            throw e
        }
    }
}