package ir.ramezani.expensenotebook

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable

class QuickAddActivity : Activity() {
    private val vazir: Typeface? by lazy { runCatching { Typeface.createFromAsset(assets, "fonts/vazirmatn_regular.ttf") }.getOrNull() }
    private lateinit var titleInput: EditText
    private lateinit var priceInput: EditText
    private var formattingPrice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.attributes = window.attributes.apply { y = dp(120) }
        buildUi()
        setupPriceFormatting()
        setupKeyboardActions()
        titleInput.requestFocus()
        titleInput.postDelayed({
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun buildUi() {
        val green = Color.rgb(26, 94, 58)
        val border = Color.rgb(226, 226, 226)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            background = rounded(Color.WHITE, 0f, border)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setBackgroundColor(green)
        }
        val headerText = TextView(this).apply {
            text = "ثبت سریع هزینه"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            typeface = vazir ?: android.graphics.Typeface.DEFAULT_BOLD
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            vazir?.let { typeface = it }
            setOnClickListener { finish() }
        }
        header.addView(headerText, LinearLayout.LayoutParams(0, dp(24), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(34), dp(24)))
        root.addView(header, LinearLayout.LayoutParams.MATCH_PARENT, dp(44))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }
        titleInput = makeInput("عنوان هزینه", EditorInfo.IME_ACTION_NEXT)
        priceInput = makeInput("هزار تومان", EditorInfo.IME_ACTION_DONE).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val add = TextView(this).apply {
            text = "+"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(green)
            vazir?.let { typeface = it }
            setOnClickListener { submit() }
        }
        row.addView(titleInput, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) })
        row.addView(priceInput, LinearLayout.LayoutParams(dp(92), dp(42)).apply { marginEnd = dp(6) })
        row.addView(add, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(row)

        val foot = TextView(this).apply {
            text = "مبلغ به هزار تومان وارد شود ×۱۰۰۰ ⚡"
            setTextColor(Color.rgb(160, 160, 160))
            textSize = 9f
            gravity = Gravity.CENTER
            vazir?.let { typeface = it }
            setPadding(dp(8), 0, dp(8), dp(10))
        }
        root.addView(foot)

        setContentView(root, ViewGroup.LayoutParams(dp(340), ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun makeInput(hintText: String, imeAction: Int) = EditText(this).apply {
        hint = hintText
        textSize = 13f
        setSingleLine(true)
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        setPadding(dp(10), 0, dp(10), 0)
        background = rounded(Color.rgb(250, 250, 250), 0f, Color.rgb(226, 226, 226))
        vazir?.let { typeface = it }
        imeOptions = imeAction
    }

    private fun setupKeyboardActions() {
        titleInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                priceInput.requestFocus()
                true
            } else false
        }

        priceInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                submit()
                true
            } else false
        }
    }

    private fun setupPriceFormatting() {
        priceInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (formattingPrice) return
                val raw = s?.toString().orEmpty()
                if (raw.isBlank()) return
                val price = ExpenseDataUtils.parsePrice(raw)
                if (price <= 0) return
                val formatted = ExpenseDataUtils.formatNumber(price)
                if (formatted != raw) {
                    formattingPrice = true
                    priceInput.setText(formatted)
                    priceInput.setSelection(formatted.length)
                    formattingPrice = false
                }
                priceInput.background = rounded(Color.rgb(250, 250, 250), 0f, Color.rgb(226, 226, 226))
            }
        })
    }

    private fun submit() {
        val rawPrice = ExpenseDataUtils.parsePrice(priceInput.text.toString())
        val price = rawPrice * 1000
        if (price <= 0) {
            priceInput.background = rounded(Color.rgb(255, 250, 250), 0f, Color.rgb(211, 47, 47))
            priceInput.requestFocus()
            Toast.makeText(this, "مبلغ معتبر نیست", Toast.LENGTH_SHORT).show()
            return
        }
        ExpenseDataUtils.addExpense(this, titleInput.text.toString().trim().ifBlank { "هزینه" }, price)
        Toast.makeText(this, "ثبت شد: " + ExpenseDataUtils.formatNumber(price) + " تومان", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun rounded(color: Int, radius: Float, stroke: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(dp(1), stroke)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
