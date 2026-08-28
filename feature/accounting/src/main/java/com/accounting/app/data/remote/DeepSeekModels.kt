package com.accounting.app.data.remote

/**
 * DeepSeek 可用模型常量。
 * 不同业务场景强制绑定不同模型，不可动态篡改。
 */
object DeepSeekModels {
    /** 完整记账解析用，高精度模型 */
    const val PRO = "deepseek-v4-pro"

    /** 记忆补全、对话查询用，低成本模型 */
    const val FLASH = "deepseek-v4-flash"
}
