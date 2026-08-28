package com.accounting.app.data.model

import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.data.local.entity.IncomeEntity
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class BillPlanItem(
    val action: PlanAction,
    val type: String,
    val amount: Long,
    val category: String,
    val subCategory: String?,
    val merchant: String?,
    val billTime: Long,
    val remark: String?,
    val confidence: Float,
    val source: String,
    val matchedMemory: Boolean,
    val memoryId: Long? = null,
    val targetBillId: Long? = null
) : Parcelable

fun ExpenseEntity.toPlanItem(action: PlanAction = PlanAction.ADD): BillPlanItem {
    return BillPlanItem(
        action = action,
        type = "expense",
        amount = amount,
        category = category,
        subCategory = subcategory,
        merchant = merchant,
        billTime = time,
        remark = note,
        confidence = confidence,
        source = "local",
        matchedMemory = false,
        targetBillId = if (action != PlanAction.ADD) id else null
    )
}

fun IncomeEntity.toPlanItem(action: PlanAction = PlanAction.ADD): BillPlanItem {
    return BillPlanItem(
        action = action,
        type = "income",
        amount = amount,
        category = category,
        subCategory = subcategory,
        merchant = merchant,
        billTime = time,
        remark = note,
        confidence = confidence,
        source = "local",
        matchedMemory = false,
        targetBillId = if (action != PlanAction.ADD) id else null
    )
}