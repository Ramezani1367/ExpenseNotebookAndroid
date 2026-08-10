package ir.ramezani.expensenotebook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.RemoteViews

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgets(
            context,
            appWidgetManager,
            intArrayOf(appWidgetId)
        )
    }

    companion object {

        private val TEXT_DARK = Color.rgb(77, 77, 77)
        private val MUTED = Color.rgb(136, 136, 136)
        private val LIGHT = Color.rgb(210, 210, 210)
        private val DATE_GREY = Color.rgb(176, 176, 176)

        fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            val summary = ExpenseDataUtils.summary(context)

            ids.forEach { id ->

                val options = manager.getAppWidgetOptions(id)
                val layout = chooseLayout(options)
                val views = RemoteViews(context.packageName, layout)

                bindCommon(context, views, summary)

                when (layout) {

                    R.layout.widget_4x1 -> {
                        bind4x1(views, summary)
                    }

                    R.layout.widget_2x2 -> {
                        bind2x2(views, summary)
                    }

                    R.layout.widget_3x2 -> {
                        bindFull(
                            views = views,
                            summary = summary,
                            maxItems = 2
                        )
                    }

                    R.layout.widget_5x2 -> {
                        bindFull(
                            views = views,
                            summary = summary,
                            maxItems = 3
                        )
                    }

                    else -> {
                        bindFull(
                            views = views,
                            summary = summary,
                            maxItems = 4
                        )
                    }
                }

                manager.updateAppWidget(id, views)
            }
        }

        private fun chooseLayout(options: Bundle?): Int {

            val minWidth = options?.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                250
            ) ?: 250

            val minHeight = options?.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                70
            ) ?: 70

            return when {

                // ویجت باریک و کم‌عرض
                minWidth < 155 -> {
                    R.layout.widget_2x2
                }

                // حالت پیش‌فرض ۴×۱
                minHeight <= 92 -> {
                    R.layout.widget_4x1
                }

                // حالت متوسط
                minWidth < 235 -> {
                    R.layout.widget_3x2
                }

                // حالت عریض
                minWidth >= 300 -> {
                    R.layout.widget_5x2
                }

                // حالت استاندارد
                else -> {
                    R.layout.widget_4x2
                }
            }
        }

        private fun bindCommon(
            context: Context,
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            val openIntent = Intent(
                context,
                MainActivity::class.java
            ).apply {
                action =
                    "ir.ramezani.expensenotebook.OPEN_FROM_WIDGET_${summary.mode}"

                putExtra(
                    MainActivity.EXTRA_OPEN_MODE,
                    summary.mode
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val openPendingIntent = PendingIntent.getActivity(
                context,
                10 + summary.mode.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            val addIntent = Intent(
                context,
                QuickAddActivity::class.java
            ).apply {
                action =
                    "ir.ramezani.expensenotebook.QUICK_ADD"

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val addPendingIntent = PendingIntent.getActivity(
                context,
                20,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            val settingsIntent = Intent(
                context,
                WidgetSettingsActivity::class.java
            ).apply {
                action =
                    "ir.ramezani.expensenotebook.WIDGET_SETTINGS"

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val settingsPendingIntent = PendingIntent.getActivity(
                context,
                30,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )

            views.setOnClickPendingIntent(
                R.id.widget_root,
                openPendingIntent
            )

            views.setOnClickPendingIntent(
                R.id.widget_plus,
                addPendingIntent
            )

            views.setOnClickPendingIntent(
                R.id.widget_gear,
                settingsPendingIntent
            )
        }

        private fun bindFull(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary,
            maxItems: Int
        ) {
            views.setTextViewText(
                R.id.widget_date_primary,
                richPrimaryDate(summary)
            )

            views.setTextViewText(
                R.id.widget_date_secondary,
                summary.secondaryDate
            )

            views.setTextViewText(
                R.id.widget_label,
                summary.label
            )

            views.setTextViewText(
                R.id.widget_total,
                ExpenseDataUtils.formatNumber(summary.total)
            )

            views.setTextViewText(
                R.id.widget_items,
                richItemsLine(summary, maxItems)
            )
        }

        private fun bind4x1(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            views.setTextViewText(
                R.id.widget_date_primary,
                richPrimaryDate(summary)
            )

            views.setTextViewText(
                R.id.widget_date_secondary,
                summary.secondaryDate
            )

            views.setTextViewText(
                R.id.widget_label,
                summary.label
            )

            views.setTextViewText(
                R.id.widget_total,
                ExpenseDataUtils.formatNumber(summary.total)
            )
        }

        private fun bind2x2(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            views.setTextViewText(
                R.id.widget_date_primary,
                richPrimaryDate(summary)
            )

            views.setTextViewText(
                R.id.widget_date_secondary,
                summary.secondaryDate
            )

            views.setTextViewText(
                R.id.widget_label,
                summary.label
            )

            views.setTextViewText(
                R.id.widget_total,
                ExpenseDataUtils.formatNumber(summary.total)
            )
        }

        private fun richPrimaryDate(
            summary: ExpenseDataUtils.WidgetSummary
        ): CharSequence {
            val builder = SpannableStringBuilder()

            val parts = summary.primaryDate.split(
                "|",
                limit = 2
            )

            val start = builder.length

            builder.append(
                parts.firstOrNull()?.trim().orEmpty()
            )

            builder.setSpan(
                ForegroundColorSpan(DATE_GREY),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (
                summary.mode == ExpenseDataUtils.MODE_DAY ||
                !summary.primaryDate.contains("|")
            ) {
                return builder
            }

            val separatorStart = builder.length

            builder.append("  |  ")

            builder.setSpan(
                ForegroundColorSpan(LIGHT),
                separatorStart,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.append(
                parts.getOrElse(1) {
                    ""
                }.trim()
            )

            return builder
        }

        private fun richItemsLine(
            summary: ExpenseDataUtils.WidgetSummary,
            maxItems: Int
        ): CharSequence {
            if (summary.expenses.isEmpty()) {
                return summary.itemsLine
            }

            val latestItems = summary.expenses
                .sortedByDescending { it.idx }
                .take(maxItems)

            val builder = SpannableStringBuilder()

            latestItems.forEachIndexed { index, item ->

                if (index > 0) {
                    if (index % 3 == 0) {
                        builder.append("\n")
                    } else {
                        appendSeparator(builder)
                    }
                }

                val titleStart = builder.length

                builder
                    .append(trimForWidget(item.title, 7))
                    .append(" ")

                builder.setSpan(
                    ForegroundColorSpan(
                        Color.rgb(102, 102, 102)
                    ),
                    titleStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                val priceStart = builder.length

                builder.append(
                    ExpenseDataUtils.formatNumber(item.price)
                )

                builder.setSpan(
                    ForegroundColorSpan(TEXT_DARK),
                    priceStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    priceStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (index == 0) {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        titleStart,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            val remaining =
                summary.expenses.size - latestItems.size

            if (remaining > 0) {
                appendSeparator(builder)

                val remainingStart = builder.length

                builder
                    .append("+")
                    .append(
                        ExpenseDataUtils.toPersianDigits(
                            remaining.toString()
                        )
                    )

                builder.setSpan(
                    ForegroundColorSpan(MUTED),
                    remainingStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            return builder
        }

        private fun appendSeparator(
            builder: SpannableStringBuilder
        ) {
            val start = builder.length

            builder.append("  |  ")

            builder.setSpan(
                ForegroundColorSpan(LIGHT),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        private fun trimForWidget(
            text: String,
            maxLength: Int
        ): String {
            return if (text.length <= maxLength) {
                text
            } else {
                text.take(maxLength - 1) + "…"
            }
        }

        private fun immutableFlag(): Int {
            return if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        }
    }
}
