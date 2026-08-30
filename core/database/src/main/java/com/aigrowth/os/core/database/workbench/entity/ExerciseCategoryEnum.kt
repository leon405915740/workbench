package com.aigrowth.os.core.database.workbench.entity

/**
 * 运动类别统一枚举：创建习惯、打卡编辑、洞察过滤三处共用。
 */
enum class ExerciseCategoryEnum(val label: String) {
    CARDIO("有氧"),
    UPPER("上肢"),
    LOWER("下肢"),
    CORE("核心"),
    FUNCTIONAL("功能训练"),
    OTHER("其他")
}