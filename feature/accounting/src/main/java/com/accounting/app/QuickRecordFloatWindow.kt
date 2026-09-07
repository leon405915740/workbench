package com.accounting.app

import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.accounting.app.data.local.entity.ExpenseEntity
import com.accounting.app.log.AppLogger
import com.accounting.app.util.AmountUtils
import com.accounting.app.util.CategoryConstants
import com.accounting.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TYPE_APPLICATION_OVERLAY 全局悬浮窗：付款通知唤起的快捷记账小卡片。
 * 任何 App 内都能弹出，不切应用前台。
 *
 * ponytail: 用传统 Android View（非 Compose）实现，避免 ComposeView 悬浮窗
 * 需要的 LifecycleOwner + SavedStateRegistryOwner 等一堆 overhead。
 * 直接 insert 到 repository，不经过 ViewModel。
 */
class QuickRecordFloatWindow private constructor() {

    companion object {
        @Volatile private var instance: QuickRecordFloatWindow? = null

        @JvmStatic
        fun getInstance(): QuickRecordFloatWindow = instance ?: synchronized(this) {
            instance ?: QuickRecordFloatWindow().also { instance = it }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wm: WindowManager? = null
    private var rootView: View? = null

    fun show(context: android.content.Context, amountFen: Long, merchant: String) {
        // WindowManager.addView 必须在主线程
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { show(context, amountFen, merchant) }
            return
        }
        if (rootView != null) return // 已显示，不重复弹

        val appContext = context.applicationContext
        val windowManager = appContext.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        wm = windowManager

        val view = LayoutInflater.from(appContext)
            .inflate(R.layout.quick_record_float, null)

        val etAmount = view.findViewById<EditText>(R.id.et_amount)
        val etMerchant = view.findViewById<EditText>(R.id.et_merchant)
        val etNote = view.findViewById<EditText>(R.id.et_note)
        val tvCategoryLayout = view.findViewById<LinearLayout>(R.id.tv_category)
        val tvCategoryText = view.findViewById<TextView>(R.id.tv_category_text)
        val btnClose = view.findViewById<TextView>(R.id.btn_close)
        val btnCancel = view.findViewById<TextView>(R.id.btn_cancel)
        val btnSubmit = view.findViewById<TextView>(R.id.btn_submit)
        val categoryGrid = view.findViewById<LinearLayout>(R.id.category_grid)

        val categories = CategoryConstants.getCategories("expense")
        var currentCategory = CategoryConstants.DEFAULT_QUICK_PAYMENT_CATEGORY
        tvCategoryText.text = currentCategory
        etAmount.setText(AmountUtils.fenToYuan(amountFen))
        etMerchant.setText(merchant)

        // 分类网格初始化
        val gridRows = mutableListOf<LinearLayout>()
        for (i in categories.indices step 4) {
            val row = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(appContext, 8) }
            }
            for (j in 0 until 4) {
                val idx = i + j
                if (idx < categories.size) {
                    val cat = categories[idx]
                    val tv = TextView(appContext).apply {
                        text = cat
                        textSize = 12f
                        setPadding(dp(appContext, 10), dp(appContext, 8), dp(appContext, 10), dp(appContext, 8))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = if (j == 0) 0 else dp(appContext, 8)
                        }
                        gravity = Gravity.CENTER
                        setBackgroundResource(R.drawable.quick_chip_selector)
                        setTextColor(if (cat == currentCategory) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
                        isSelected = cat == currentCategory
                        setOnClickListener {
                            currentCategory = cat
                            tvCategoryText.text = cat
                            // 更新选中态
                            gridRows.forEach { row ->
                                for (k in 0 until row.childCount) {
                                    val child = row.getChildAt(k) as? TextView ?: continue
                                    val isSel = child.text == cat
                                    child.isSelected = isSel
                                    child.setTextColor(if (isSel) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
                                }
                            }
                            categoryGrid.visibility = View.GONE
                        }
                    }
                    row.addView(tv)
                } else {
                    // 占位
                    val space = View(appContext).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    }
                    row.addView(space)
                }
            }
            gridRows.add(row)
            categoryGrid.addView(row)
        }

        // 分类点击展开/收起
        tvCategoryLayout.setOnClickListener {
            categoryGrid.visibility = if (categoryGrid.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 更新按钮启用态
        fun updateSubmitEnabled() {
            val amt = runCatching { AmountUtils.yuanToFen(etAmount.text.toString()) }.getOrDefault(0L)
            val enabled = amt > 0 && currentCategory.isNotBlank()
            btnSubmit.alpha = if (enabled) 1f else 0.4f
            btnSubmit.isClickable = enabled
        }
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateSubmitEnabled() }
        })
        updateSubmitEnabled()

        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }
        btnSubmit.setOnClickListener {
            val amtFen = runCatching { AmountUtils.yuanToFen(etAmount.text.toString()) }.getOrDefault(0L)
            val m = etMerchant.text.toString().trim().ifBlank { null }
            val note = etNote.text.toString().trim().ifBlank { null }
            submit(appContext, amtFen, currentCategory, m, note)
            dismiss()
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // 输入框需要聚焦弹键盘，不能用 FLAG_NOT_FOCUSABLE；
            // 用 FLAG_NOT_TOUCH_MODAL 让触摸不被子窗口拦截
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        windowManager.addView(view, lp)
        rootView = view
    }

    fun dismiss() {
        try { rootView?.let { wm?.removeViewImmediate(it) } } catch (_: Exception) {}
        rootView = null
    }

    private fun submit(
        appContext: android.content.Context,
        amountFen: Long, category: String, merchant: String?, note: String?
    ) {
        val repository = AccountingApp.getInstance().appRepository
        val requestId = AppLogger.generateRequestId()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insertExpense(
                        ExpenseEntity(
                            amount = amountFen, category = category, subcategory = null,
                            merchant = merchant, time = TimeUtils.now(), note = note,
                            confidence = 1.0f, rawInput = "快捷记账 ${merchant.orEmpty()}",
                            createdAt = TimeUtils.now()
                        ), requestId, 1
                    )
                }
                Toast.makeText(appContext, "记账成功", Toast.LENGTH_SHORT).show()
                AppLogger.d(requestId, "悬浮窗记账", "提交成功")
            } catch (e: Exception) {
                Toast.makeText(appContext, "记账失败：${e.message}", Toast.LENGTH_SHORT).show()
                AppLogger.e(requestId, "悬浮窗记账", "提交失败: ${e.message}", e)
            }
        }
    }

    private fun dp(ctx: android.content.Context, value: Int): Int =
        (value * ctx.resources.displayMetrics.density).toInt()
}
