package com.accounting.app.plan.validator

import com.accounting.app.data.model.BillExecutePlan
import com.accounting.app.data.model.PlanAction
import com.accounting.app.log.AppLogger

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Failure(val reason: String) : ValidationResult()
}

object PlanValidator {

    fun validate(plan: BillExecutePlan, requestId: String): ValidationResult {
        if (plan.items.isEmpty()) {
            return ValidationResult.Failure("计划为空")
        }

        for ((index, item) in plan.items.withIndex()) {
            val billIndex = index + 1
            val actionPassed = true
            AppLogger.d(requestId, "计划校验", "动作校验：${item.action}，结果：${actionPassed}", billIndex)

            when (item.action) {
                PlanAction.ADD -> {
                    val timePassed = item.billTime > 0
                    val amountPassed = item.amount > 0
                    val categoryPassed = item.category.isNotBlank()

                    AppLogger.d(requestId, "计划校验", "时间校验：${item.billTime}，结果：${timePassed}", billIndex)
                    AppLogger.d(requestId, "计划校验", "金额校验：${item.amount}，结果：${amountPassed}", billIndex)
                    AppLogger.d(requestId, "计划校验", "分类校验：${item.category}，结果：${categoryPassed}", billIndex)

                    if (!amountPassed) {
                        return ValidationResult.Failure("第${billIndex}笔金额必须大于0")
                    }
                    if (!categoryPassed) {
                        return ValidationResult.Failure("第${billIndex}笔分类不能为空")
                    }
                    if (!timePassed) {
                        return ValidationResult.Failure("第${billIndex}笔时间无效")
                    }
                }
                PlanAction.UPDATE -> {
                    val targetBillIdPassed = item.targetBillId != null
                    AppLogger.d(requestId, "计划校验", "targetBillId校验：${item.targetBillId}，结果：${targetBillIdPassed}", billIndex)
                    if (!targetBillIdPassed) {
                        return ValidationResult.Failure("第${billIndex}笔缺少目标账单ID")
                    }
                }
                PlanAction.DELETE -> {
                    val targetBillIdPassed = item.targetBillId != null
                    AppLogger.d(requestId, "计划校验", "targetBillId校验：${item.targetBillId}，结果：${targetBillIdPassed}", billIndex)
                    if (!targetBillIdPassed) {
                        return ValidationResult.Failure("第${billIndex}笔缺少目标账单ID")
                    }
                }
            }
        }

        return ValidationResult.Success
    }
}