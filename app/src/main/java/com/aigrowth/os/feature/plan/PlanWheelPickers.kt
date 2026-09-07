package com.aigrowth.os.feature.plan

import android.widget.NumberPicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
// ===== Wheel pickers =====

@Composable
internal fun DateWheelPicker(date: String, onChange: (String) -> Unit) {
    val init = runCatching { LocalDate.parse(date.trim()) }.getOrDefault(LocalDate.now())
    AndroidView(
        factory = { ctx ->
            android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setBackgroundColor(0xFFF7F8FA.toInt())
                setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
            }
        },
        update = { container ->
            container.removeAllViews()
            val now = LocalDate.now()
            val npYear = NumberPicker(container.context).apply {
                minValue = now.year - 50
                maxValue = now.year + 50
                value = init.year
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val npMonth = NumberPicker(container.context).apply {
                minValue = 1; maxValue = 12
                value = init.monthValue
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val npDay = NumberPicker(container.context).apply {
                minValue = 1; maxValue = init.lengthOfMonth()
                value = init.dayOfMonth
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val onChanged = NumberPicker.OnValueChangeListener { _, _, _ ->
                val y = npYear.value
                val m = npMonth.value
                val dayMax = LocalDate.of(y, m, 1).lengthOfMonth()
                val d = npDay.value.coerceAtMost(dayMax)
                npDay.maxValue = dayMax
                onChange(String.format(Locale.US, "%04d-%02d-%02d", y, m, d))
            }
            npYear.setOnValueChangedListener(onChanged)
            npMonth.setOnValueChangedListener(onChanged)
            npDay.setOnValueChangedListener(onChanged)
            container.addView(npYear)
            container.addView(makeLabel(container.context, "年"))
            container.addView(npMonth)
            container.addView(makeLabel(container.context, "月"))
            container.addView(npDay)
            container.addView(makeLabel(container.context, "日"))
        }
    )
}

private fun makeLabel(ctx: android.content.Context, text: String): android.widget.TextView =
    android.widget.TextView(ctx).apply {
        this.text = text
        textSize = 12f
        setTextColor(0xFF8E8E93.toInt())
        setPadding(dp(ctx, 2), 0, dp(ctx, 2), 0)
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gravity = android.view.Gravity.CENTER_VERTICAL
    }

@Composable
internal fun TimeWheelPicker(time: String?, onChange: (String?) -> Unit) {
    val init = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.now()
    val valid = time != null
    AndroidView(
        factory = { ctx ->
            android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setBackgroundColor(0xFFF7F8FA.toInt())
                setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { container ->
            container.removeAllViews()
            val npHour = NumberPicker(container.context).apply {
                minValue = 0; maxValue = 23
                value = if (valid) init.hour else LocalTime.now().hour
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val npMin = NumberPicker(container.context).apply {
                minValue = 0; maxValue = 59
                value = if (valid) init.minute else LocalTime.now().minute
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
                onChange(String.format(Locale.US, "%02d:%02d", npHour.value, npMin.value))
            }
            npHour.setOnValueChangedListener(listener)
            npMin.setOnValueChangedListener(listener)
            container.addView(npHour)
            container.addView(makeLabel(container.context, "时"))
            container.addView(npMin)
            container.addView(makeLabel(container.context, "分"))
        }
    )
}

private fun dp(ctx: android.content.Context, v: Int): Int =
    (v * ctx.resources.displayMetrics.density).toInt()


