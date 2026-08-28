package com.aigrowth.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.app.AccountingApp
import com.accounting.app.AccountingBridge
import com.accounting.app.log.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardAccountingViewModel : ViewModel() {

    private val bridge: AccountingBridge = AccountingApp.getBridge()

    private val _monthlyExpense = MutableStateFlow<Double?>(null)
    val monthlyExpense: StateFlow<Double?> = _monthlyExpense.asStateFlow()

    init {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, "DashboardAccountingVM", "init 开始加载月支出数据")
        viewModelScope.launch {
            try {
                bridge.getMonthlyExpense().collect { amount ->
                    _monthlyExpense.value = amount
                    AppLogger.d(requestId, "DashboardAccountingVM", "月支出数据更新: ${amount ?: "null"}")
                }
            } catch (e: Exception) {
                AppLogger.e(requestId, "DashboardAccountingVM", "月支出数据加载异常", e)
            }
        }
    }
}
