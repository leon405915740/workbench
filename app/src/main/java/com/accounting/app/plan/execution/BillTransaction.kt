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
        AppLogger.d(
            requestId, "BillTransaction",
            "金额已脱敏", billIndex
        )
        return id
    }

    private suspend fun update(item: BillPlanItem, requestId: String, billIndex: Int): Long {
        val targetId = item.targetBillId ?: throw Exception("缺少目标账单ID")
        if (item.type == "expense") {
            expenseDao.updateCategory(targetId, item.category, item.subCategory)
        } else {
            incomeDao.updateCategory(targetId, item.category, item.subCategory)
        }
        AppLogger.i(
            requestId, "入库执行",
            "结果：成功，更新账单ID：$targetId，类型：${item.type}", billIndex
        )
        return targetId
    }

    private suspend fun delete(item: BillPlanItem, requestId: String, billIndex: Int): Long {
        val targetId = item.targetBillId ?: throw Exception("缺少目标账单ID")
        if (item.type == "expense") {
            expenseDao.deleteById(targetId)
        } else {
            incomeDao.deleteById(targetId)
        }
        AppLogger.i(
            requestId, "入库执行",
            "结果：成功，删除账单ID：$targetId，类型：${item.type}", billIndex
        )
        return targetId
    }
}