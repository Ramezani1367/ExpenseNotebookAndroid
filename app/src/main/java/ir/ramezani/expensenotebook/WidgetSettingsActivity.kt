package ir.ramezani.expensenotebook

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class WidgetSettingsActivity : Activity() {
    private val vazir: Typeface? by lazy { runCatching { Typeface.createFromAsset(assets, "fonts/vazirmatn_regular.ttf") }.getOrNull() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.attributes = window.attributes.apply { y = dp(100) }
        buildUi()
    }

    private fun buildUi() {
        val green = Color.rgb(26, 94, 58)
        val border = Color.rgb(226, 226, 226)
        val mode = ExpenseDataUtils.currentMode(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(dp(1), border)
            }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(green)
        }
        val title = TextView(this).apply {
            text = "نمایش ویجت"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = vazir ?: Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 13f
            vazir?.let { typeface = it }
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(26), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(34), dp(26)))
        root.addView(header)

        addOption(root, "خرج کرد امروز", ExpenseDataUtils.MODE_DAY, mode)
        addOption(root, "خرج کرد این هفته", ExpenseDataUtils.MODE_WEEK, mode)
        addOption(root, "خرج کرد این ماه", ExpenseDataUtils.MODE_MONTH, mode)

        setContentView(root, ViewGroup.LayoutParams(dp(230), ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun addOption(root: LinearLayout, label: String, optionMode: String, currentMode: String) {
        val green = Color.rgb(26, 94, 58)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.WHITE)
            setOnClickListener {
                ExpenseDataUtils.setMode(this@WidgetSettingsActivity, optionMode)
                ExpenseDataUtils.updateAllWidgets(this@WidgetSettingsActivity)
                finish()
            }
        }
        val labelView = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(if (optionMode == currentMode) green else Color.rgb(85, 85, 85))
            if (optionMode == currentMode) typeface = vazir ?: Typeface.DEFAULT_BOLD else vazir?.let { typeface = it }
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        val tick = TextView(this).apply {
            text = if (optionMode == currentMode) "✓" else ""
            textSize = 12f
            setTextColor(green)
            gravity = Gravity.CENTER
            vazir?.let { typeface = it }
        }
        row.addView(labelView, LinearLayout.LayoutParams(0, dp(44), 1f))
        row.addView(tick, LinearLayout.LayoutParams(dp(28), dp(44)))
        root.addView(row)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
