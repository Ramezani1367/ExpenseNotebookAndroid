package ir.ramezani.expensenotebook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews

/**
 * Stable single-layout widget implementation.
 * The same RemoteViews tree is used at every size; content is reduced or
 * expanded according to the size reported by the launcher.
 */
class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { updateOne(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        updateOne(context, manager, appWidgetId)
    }

    companion object {
        private const val DEFAULT_WIDTH = 250
        private const val DEFAULT_HEIGHT = 70

        fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                DEFAULT_WIDTH
            )
            val height = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                DEFAULT_HEIGHT
            )

            val summary = ExpenseDataUtils.summary(context)
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_4x1
            )

            bindData(views, summary, width, height)
            bindActions(context, views, summary)
            manager.updateAppWidget(id, views)
        }

        private fun bindData(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary,
            width: Int,
            height: Int
        ) {
            views.setTextViewText(R.id.widget_label, summary.label)
            views.setTextViewText(
                R.id.widget_date_primary,
                summary.primaryDate.replace("|", " ").replace(Regex("\\s+"), " ").trim()
            )
            views.setTextViewText(R.id.widget_date_secondary, summary.secondaryDate)
            views.setTextViewText(
                R.id.widget_total,
                ExpenseDataUtils.formatNumber(summary.total)
            )

            // Keep the default 4×1 compact. Reveal the list only when the
            // launcher gives the widget enough vertical space.
            val showList = height > 92
            val narrow = width < 180

            views.setViewVisibility(
                R.id.widget_items,
                if (showList) android.view.View.VISIBLE else android.view.View.GONE
            )
            views.setViewVisibility(
                R.id.widget_divider,
                if (showList) android.view.View.VISIBLE else android.view.View.GONE
            )
            views.setViewVisibility(
                R.id.widget_date_secondary,
                if (narrow) android.view.View.GONE else android.view.View.VISIBLE
            )

            val maxItems = when {
                width >= 300 -> 3
                width >= 220 -> 4
                else -> 2
            }

            val listText = if (summary.expenses.isEmpty()) {
                summary.itemsLine
            } else {
                val latest = summary.expenses
                    .sortedByDescending { it.idx }
                    .take(maxItems)

                val lines = latest.map {
                    "${trim(it.title, 14)} — ${ExpenseDataUtils.formatNumber(it.price)}"
                }.toMutableList()

                val remaining = summary.expenses.size - latest.size
                if (remaining > 0) {
                    lines.add(
                        "+${ExpenseDataUtils.toPersianDigits(remaining.toString())}"
                    )
                }

                lines.joinToString("\n")
            }

            views.setTextViewText(R.id.widget_items, listText)
        }

        private fun bindActions(
            context: Context,
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.OPEN_WIDGET"
                putExtra(MainActivity.EXTRA_OPEN_MODE, summary.mode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val addIntent = Intent(context, QuickAddActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.QUICK_ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val settingsIntent = Intent(
                context,
                WidgetSettingsActivity::class.java
            ).apply {
                action = "ir.ramezani.expensenotebook.WIDGET_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    100,
                    openIntent,
                    pendingFlags()
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_plus,
                PendingIntent.getActivity(
                    context,
                    101,
                    addIntent,
                    pendingFlags()
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_gear,
                PendingIntent.getActivity(
                    context,
                    102,
                    settingsIntent,
                    pendingFlags()
                )
            )
        }

        private fun trim(value: String, max: Int): String {
            return if (value.length <= max) value else value.take(max - 1) + "…"
        }

        private fun pendingFlags(): Int {
            return PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
        }
    }
}
