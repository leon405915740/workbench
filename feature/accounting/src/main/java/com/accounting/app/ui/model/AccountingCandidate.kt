package com.accounting.app.ui.model

/**
 * 记账候选模型 — 规则匹配和 AI 解析结果的统一中间表示。
 *
 * 所有记账结果（无论来源是规则还是 AI）必须先生成 AccountingCandidate，
 * 经用户确认后才入库，禁止任何结果直接写入数据库。
 *
 * @param type 收支类型（"income" / "expense"）
 * @param category 分类名称
 * @param amount 金额（分）
 * @param confidence 置信度（0.0-1.0）
 * @param source 来源（"rule" / "ai"）
 * @param description 原始描述
 * @param timeHint 时间提示（自然语言，如"今天""昨天中午"，可为 null）
 * @param note 备注（可为 null）
 */
data class AccountingCandidate(
    val type: String,
    val category: String,
    val amount: Long,
    val confidence: Float,
    val source: String,
    val description: String,
    val timeHint: String? = null,
    val note: String? = null
)
