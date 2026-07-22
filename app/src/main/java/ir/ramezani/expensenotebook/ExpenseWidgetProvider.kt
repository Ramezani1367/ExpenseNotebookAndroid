package ir.ramezani.expensenotebook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.RemoteViews
import kotlin.math.max

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        private val GREEN = Color.rgb(26, 94, 58)
        private val TEXT_DARK = Color.rgb(77, 77, 77)
        private val MUTED = Color.rgb(136, 136, 136)
        private val LIGHT = Color.rgb(210, 210, 210)
        private val DATE_GREY = Color.rgb(176, 176, 176)

        private var cachedFont: Typeface? = null

        private fun getFont(context: Context): Typeface? {
            if (cachedFont != null) return cachedFont
            cachedFont = try {
                Typeface.createFromAsset(context.assets, "fonts/vazirmatn_regular.ttf")
            } catch (_: Exception) {
                try { context.resources.getFont(R.font.vazirmatn_regular) } catch (_: Exception) { null }
            }
            return cachedFont
        }

        private fun textToBitmap(
            text: CharSequence,
            textSizePx: Float,
            textColor: Int,
            maxWidthPx: Int,
            bold: Boolean = false
        ): Bitmap {
            val font = cachedFont
            val typeface = when {
                bold && font != null -> Typeface.create(font, Typeface.BOLD)
                font != null -> font
                bold -> Typeface.DEFAULT_BOLD
                else -> Typeface.DEFAULT
            }

            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                this.textSize = textSizePx
                this.color = textColor
            }

            val width = minOf(paint.measureText(text, 0, text.length).toInt() + 4, maxWidthPx)
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, max(width, 1))
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setMaxLines(1)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()

            val bmp = Bitmap.createBitmap(max(layout.width, 1), max(layout.height, 1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            layout.draw(canvas)
            return bmp
        }

        private fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        private fun spToPx(context: Context, sp: Float): Float {
            return sp * context.resources.displayMetrics.scaledDensity
        }

        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            getFont(context) // preload font
            val summary = ExpenseDataUtils.summary(context)
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val layout = chooseLayout(options)
                val views = RemoteViews(context.packageName, layout)
                bindCommon(context, views, summary)
                when (layout) {
                    R.layout.widget_4x1 -> bind4x1(context, views, summary, options)
                    R.layout.widget_2x2 -> bind2x2(context, views, summary, options)
                    R.layout.widget_3x2 -> bindFull(context, views, summary, maxItems = 2, options)
                    R.layout.widget_5x2 -> bindFull(context, views, summary, maxItems = 4, options)
                    else -> bindFull(context, views, summary, maxItems = 3, options)
                }
                applyFont(context, views)
                manager.updateAppWidget(id, views)
            }
        }

        private fun applyFont(context: Context, views: RemoteViews) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val font = context.resources.getFont(R.font.vazirmatn_regular)
                    val ids = intArrayOf(
                        R.id.widget_date_primary, R.id.widget_date_secondary,
                        R.id.widget_label, R.id.widget_total,
                        R.id.widget_items, R.id.widget_count,
                        R.id.widget_plus, R.id.widget_gear
                    )
                    for (id in ids) {
                        try {
                            val method = RemoteViews::class.java.getMethod(
                                "setTextViewTypeface", Int::class.javaPrimitiveType, Typeface::class.java
                            )
                            method.invoke(views, id, font)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }

        private fun chooseLayout(options: Bundle?): Int {
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) ?: 320
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160
            return when {
                minH <= 95 && minW >= 220 -> R.layout.widget_4x1
                minW <= 210 -> R.layout.widget_2x2
                minW < 300 -> R.layout.widget_3x2
                minW >= 390 -> R.layout.widget_5x2
                else -> R.layout.widget_4x2
            }
        }

        private fun getWidgetWidthPx(options: Bundle?, context: Context): Int {
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) ?: 320
            return (minW * context.resources.displayMetrics.density).toInt()
        }

        private fun bindCommon(context: Context, views: RemoteViews, summary: ExpenseDataUtils.WidgetSummary) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.OPEN_FROM_WIDGET_${summary.mode}"
                putExtra(MainActivity.EXTRA_OPEN_MODE, summary.mode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 10 + summary.mode.hashCode(),
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.QUICK_ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val addPending = PendingIntent.getActivity(
                context, 20, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            val settingsIntent = Intent(context, WidgetSettingsActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.WIDGET_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val settingsPending = PendingIntent.getActivity(
                context, 30, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            views.setOnClickPendingIntent(R.id.widget_plus, addPending)
            views.setOnClickPendingIntent(R.id.widget_gear, settingsPending)
        }

        private fun bindFull(context: Context, views: RemoteViews, s: ExpenseDataUtils.WidgetSummary, maxItems: Int, options: Bundle?) {
            val w = getWidgetWidthPx(options, context)
            val totalW = (w * 0.45).toInt()

            views.setTextViewText(R.id.widget_date_primary, richPrimaryDate(s))
            views.setTextViewText(R.id.widget_date_secondary, s.secondaryDate)
            views.setTextViewText(R.id.widget_label, s.label)
            views.setTextViewText(R.id.widget_total, ExpenseDataUtils.formatNumber(s.total))
            views.setTextViewText(R.id.widget_items, richItemsLine(s, maxItems))

            // Bitmap fallback for total (most important custom-font element)
            try {
                val totalBmp = textToBitmap(
                    ExpenseDataUtils.formatNumber(s.total), spToPx(context, 25f), GREEN, totalW, true
                )
                views.setImageViewBitmap(R.id.widget_total_img, totalBmp)
            } catch (_: Exception) {}
        }

        private fun bind4x1(context: Context, views: RemoteViews, s: ExpenseDataUtils.WidgetSummary, options: Bundle?) {
            val w = getWidgetWidthPx(options, context)
            val totalW = (w * 0.4).toInt()

            views.setTextViewText(R.id.widget_date_primary, richPrimaryDate(s))
            views.setTextViewText(R.id.widget_date_secondary, s.secondaryDate)
            views.setTextViewText(R.id.widget_label, s.label)
            views.setTextViewText(R.id.widget_total, ExpenseDataUtils.formatNumber(s.total))

            try {
                val totalBmp = textToBitmap(
                    ExpenseDataUtils.formatNumber(s.total), spToPx(context, 20f), GREEN, totalW, true
                )
                views.setImageViewBitmap(R.id.widget_total_img, totalBmp)
            } catch (_: Exception) {}
        }

        private fun bind2x2(context: Context, views: RemoteViews, s: ExpenseDataUtils.WidgetSummary, options: Bundle?) {
            val w = getWidgetWidthPx(options, context)
            val totalW = (w * 0.7).toInt()

            views.setTextViewText(R.id.widget_date_primary, richPrimaryDate(s))
            views.setTextViewText(R.id.widget_date_secondary, s.secondaryDate)
            views.setTextViewText(R.id.widget_label, s.label)
            views.setTextViewText(R.id.widget_total, ExpenseDataUtils.formatNumber(s.total))
            views.setTextViewText(R.id.widget_count, ExpenseDataUtils.toPersianDigits(s.count.toString()) + " مورد")

            try {
                val totalBmp = textToBitmap(
                    ExpenseDataUtils.formatNumber(s.total), spToPx(context, 21f), GREEN, totalW, true
                )
                views.setImageViewBitmap(R.id.widget_total_img, totalBmp)
            } catch (_: Exception) {}
        }

        private fun richPrimaryDate(s: ExpenseDataUtils.WidgetSummary): CharSequence {
            if (s.mode == ExpenseDataUtils.MODE_DAY || !s.primaryDate.contains("|")) return s.primaryDate
            val b = SpannableStringBuilder()
            val parts = s.primaryDate.split("|", limit = 2)
            val startDay = b.length
            b.append(parts[0].trim())
            b.setSpan(ForegroundColorSpan(DATE_GREY), startDay, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val sepStart = b.length
            b.append("  |  ")
            b.setSpan(ForegroundColorSpan(LIGHT), sepStart, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            b.append(parts.getOrElse(1) { "" }.trim())
            return b
        }

        private fun richItemsLine(s: ExpenseDataUtils.WidgetSummary, maxItems: Int): CharSequence {
            if (s.expenses.isEmpty()) return s.itemsLine
            val latest = s.expenses.sortedByDescending { it.idx }.take(maxItems)
            val b = SpannableStringBuilder()
            latest.forEachIndexed { index, item ->
                if (index > 0) appendSeparator(b)
                val titleStart = b.length
                b.append(trimForWidget(item.title, 12)).append(' ')
                b.setSpan(ForegroundColorSpan(Color.rgb(102, 102, 102)), titleStart, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val priceStart = b.length
                b.append(ExpenseDataUtils.formatNumber(item.price))
                b.setSpan(ForegroundColorSpan(TEXT_DARK), priceStart, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                b.setSpan(StyleSpan(Typeface.BOLD), priceStart, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val remaining = s.expenses.size - latest.size
            if (remaining > 0) {
                appendSeparator(b)
                val rStart = b.length
                b.append('⁺').append(ExpenseDataUtils.toPersianDigits(remaining.toString()))
                b.setSpan(ForegroundColorSpan(MUTED), rStart, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return b
        }

        private fun appendSeparator(b: SpannableStringBuilder) {
            val st = b.length
            b.append("  |  ")
            b.setSpan(ForegroundColorSpan(LIGHT), st, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun trimForWidget(s: String, max: Int): String = if (s.length <= max) s else s.take(max - 1) + "…"

        private fun immutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
