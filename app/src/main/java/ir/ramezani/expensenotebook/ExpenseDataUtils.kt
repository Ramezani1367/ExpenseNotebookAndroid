package ir.ramezani.expensenotebook

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object ExpenseDataUtils {
    const val PREFS = "expense_notebook_widget"
    const val KEY_EXPENSES_JSON = "expenses_json"
    const val KEY_WIDGET_MODE = "widget_mode"

    const val MODE_DAY = "day"
    const val MODE_WEEK = "week"
    const val MODE_MONTH = "month"

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val jalaliMonths = arrayOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val weekdaysFull = arrayOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    data class JalaliDate(val y: Int, val m: Int, val d: Int) {
        fun key(): String = "$y-$m-$d"
    }

    data class GregorianDate(val y: Int, val m: Int, val d: Int)

    data class Expense(
        val title: String,
        val price: Long,
        val dateKey: String,
        val idx: Int
    )

    data class TodayInfo(
        val j: JalaliDate,
        val key: String,
        val weekdayIndex: Int,
        val weekday: String,
        val dateYear: String,
        val full: String
    )

    data class WidgetSummary(
        val mode: String,
        val label: String,
        val total: Long,
        val count: Int,
        val primaryDate: String,
        val secondaryDate: String,
        val itemsLine: String,
        val expenses: List<Expense>
    )

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentMode(context: Context): String {
        val mode = prefs(context).getString(KEY_WIDGET_MODE, MODE_DAY) ?: MODE_DAY
        return if (mode in setOf(MODE_DAY, MODE_WEEK, MODE_MONTH)) mode else MODE_DAY
    }

    fun setMode(context: Context, mode: String) {
        val clean = if (mode in setOf(MODE_DAY, MODE_WEEK, MODE_MONTH)) mode else MODE_DAY
        prefs(context).edit().putString(KEY_WIDGET_MODE, clean).apply()
    }

    fun nextMode(mode: String): String = when (mode) {
        MODE_DAY -> MODE_WEEK
        MODE_WEEK -> MODE_MONTH
        else -> MODE_DAY
    }

    fun syncExpensesJson(context: Context, json: String) {
        // Validate loosely before saving so broken strings do not kill the widget renderer.
        runCatching { JSONArray(json) }.getOrNull() ?: return
        prefs(context).edit().putString(KEY_EXPENSES_JSON, json).apply()
        updateAllWidgets(context)
    }

    fun getExpensesJson(context: Context): String = prefs(context).getString(KEY_EXPENSES_JSON, "") ?: ""

    fun readExpenses(context: Context): List<Expense> = parseExpenses(getExpensesJson(context))

    fun parseExpenses(json: String): List<Expense> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            val out = ArrayList<Expense>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val title = o.optString("t", "هزینه").ifBlank { "هزینه" }
                val price = when (val any = o.opt("p")) {
                    is Number -> any.toLong()
                    else -> parsePrice(any?.toString().orEmpty())
                }
                val date = o.optString("d", "")
                val idx = if (o.has("idx")) o.optInt("idx") else if (o.has("id")) o.optInt("id") else i
                if (price > 0 && date.matches(Regex("\\d{3,4}-\\d{1,2}-\\d{1,2}"))) {
                    out.add(Expense(title, price, date, idx))
                }
            }
            out
        }.getOrElse { emptyList() }
    }

    fun addExpense(context: Context, title: String, price: Long) {
        val current = getExpensesJson(context)
        val arr = runCatching { JSONArray(current) }.getOrElse { JSONArray() }
        var maxIdx = -1
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            maxIdx = maxOf(maxIdx, o.optInt("idx", o.optInt("id", i)))
        }
        val item = JSONObject().apply {
            put("t", title.ifBlank { "هزینه" })
            put("p", price)
            put("d", todayInfo().key)
            put("idx", maxIdx + 1)
        }
        arr.put(item)
        prefs(context).edit().putString(KEY_EXPENSES_JSON, arr.toString()).apply()
        updateAllWidgets(context)
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ExpenseWidgetProvider::class.java))
        if (ids.isNotEmpty()) ExpenseWidgetProvider.updateWidgets(context, manager, ids)
    }

    fun summary(context: Context): WidgetSummary {
        val mode = currentMode(context)
        val info = todayInfo()
        val all = readExpenses(context)
        val period = periodItems(all, mode, info).sortedWith(compareBy<Expense> { dateKeyNumber(it.dateKey) }.thenBy { it.idx })
        val total = period.sumOf { it.price }
        val rng = modeRange(info, mode)
        val primary: String
        val secondary: String
        if (rng != null) {
            primary = "${info.weekday} | ${info.dateYear}"
            secondary = rng
        } else {
            primary = info.weekday
            secondary = info.dateYear
        }
        val label = when (mode) {
            MODE_WEEK -> "خرج کرد این هفته"
            MODE_MONTH -> "خرج کرد این ماه"
            else -> "خرج کرد امروز"
        }
        return WidgetSummary(
            mode = mode,
            label = label,
            total = total,
            count = period.size,
            primaryDate = primary,
            secondaryDate = secondary,
            itemsLine = buildItemsLine(period, mode),
            expenses = period
        )
    }

    private fun buildItemsLine(items: List<Expense>, mode: String): String {
        if (items.isEmpty()) {
            return if (mode == MODE_DAY) "هنوز چیزی ثبت نشده — اولین هزینه رو بزن 👇"
            else "تو ${if (mode == MODE_WEEK) "این هفته" else "این ماه"} چیزی ثبت نشده — از ➕ شروع کن"
        }
        val latest = items.sortedByDescending { it.idx }.take(3)
        val parts = latest.map { trimForWidget(it.title, 12) + " " + formatNumber(it.price) }
        val remaining = items.size - latest.size
        return buildString {
            append(parts.joinToString(" | "))
            if (remaining > 0) append(" | +").append(toPersianDigits(remaining.toString()))
        }
    }

    private fun trimForWidget(s: String, max: Int): String = if (s.length <= max) s else s.take(max - 1) + "…"

    fun periodItems(items: List<Expense>, mode: String, info: TodayInfo = todayInfo()): List<Expense> = when (mode) {
        MODE_MONTH -> items.filter {
            val p = parseDateKey(it.dateKey)
            p != null && p.y == info.j.y && p.m == info.j.m
        }
        MODE_WEEK -> {
            val todayNumber = dateKeyNumber(info.key)
            val span = info.weekdayIndex
            items.filter {
                val diff = todayNumber - dateKeyNumber(it.dateKey)
                diff in 0..span
            }
        }
        else -> items.filter { it.dateKey == info.key }
    }

    fun todayInfo(): TodayInfo {
        val cal = Calendar.getInstance()
        val j = gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val weekdayIndex = cal.get(Calendar.DAY_OF_WEEK) % 7 // Saturday=0, Sunday=1, ... Friday=6
        val weekday = weekdaysFull[weekdayIndex]
        val dateYear = "${toPersianDigits(j.d.toString())} ${jalaliMonths[j.m - 1]} ${toPersianDigits(j.y.toString())}"
        return TodayInfo(j, j.key(), weekdayIndex, weekday, dateYear, "$weekday $dateYear")
    }

    fun modeRange(info: TodayInfo, mode: String): String? {
        val start = when (mode) {
            MODE_WEEK -> shiftJalali(info.j, -info.weekdayIndex)
            MODE_MONTH -> JalaliDate(info.j.y, info.j.m, 1)
            else -> null
        } ?: return null
        return rangeText(start, info.j)
    }

    private fun rangeText(a: JalaliDate, b: JalaliDate): String {
        val start = "از ${toPersianDigits(a.d.toString())} ${jalaliMonths[a.m - 1]}"
        if (a == b) return "$start (امروز)"
        return "$start تا ${toPersianDigits(b.d.toString())} ${jalaliMonths[b.m - 1]}"
    }

    fun parseDateKey(key: String): JalaliDate? {
        val p = key.split('-')
        if (p.size != 3) return null
        return runCatching { JalaliDate(p[0].toInt(), p[1].toInt(), p[2].toInt()) }.getOrNull()
    }

    fun dateKeyNumber(key: String): Long {
        val j = parseDateKey(key) ?: return Long.MIN_VALUE
        val g = jalaliToGregorian(j.y, j.m, j.d)
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(g.y, g.m - 1, g.d, 0, 0, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    fun shiftJalali(j: JalaliDate, days: Int): JalaliDate {
        val g = jalaliToGregorian(j.y, j.m, j.d)
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(g.y, g.m - 1, g.d, 0, 0, 0)
            add(Calendar.DAY_OF_MONTH, days)
        }
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun gregorianToJalali(y: Int, m: Int, d: Int): JalaliDate {
        val g = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val y2 = if (m > 2) y + 1 else y
        var days = 355666 + 365 * y + ((y2 + 3) / 4) - ((y2 + 99) / 100) + ((y2 + 399) / 400) + d + g[m - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(y0: Int, m: Int, d: Int): GregorianDate {
        var y = y0 + 1595
        var days = -355668 + 365 * y + (y / 33) * 8 + (((y % 33) + 3) / 4) + d
        days += if (m < 7) (m - 1) * 31 else (m - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return GregorianDate(gy, gm, gd)
    }

    fun formatNumber(n: Long): String {
        val formatted = java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(Locale.US)).format(n)
        return toPersianDigits(formatted)
    }

    fun toPersianDigits(input: String): String = buildString(input.length) {
        for (ch in input) append(if (ch in '0'..'9') persianDigits[ch - '0'] else ch)
    }

    fun parsePrice(text: String): Long {
        val latin = buildString {
            text.forEach { ch ->
                val idx = persianDigits.indexOf(ch)
                append(if (idx >= 0) ('0'.code + idx).toChar() else ch)
            }
        }.replace(Regex("[٬,\\s]"), "")
        return latin.toLongOrNull() ?: 0L
    }
}
