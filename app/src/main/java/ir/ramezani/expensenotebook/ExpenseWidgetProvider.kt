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
 * A conservative RemoteViews widget implementation.
 * It intentionally uses only standard TextView/LinearLayout operations
 * for maximum compatibility with Android launchers, including Samsung One UI.
 */
class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateOneWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            appWidgetId,
            newOptions
        )
        updateOneWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {

        fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            ids.forEach { id ->
                updateOneWidget(context, manager, id)
            }
        }

        private fun updateOneWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val summary = ExpenseDataUtils.summary(context)
            val options = manager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                250
            )
            val height = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                70
            )

            val views = createViews(
                context = context,
                summary = summary,
                width = width,
                height = height
            )

            // Always send one complete RemoteViews tree.
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun createViews(
            context: Context,
            summary: ExpenseDataUtils.WidgetSummary,
            width: Int,
            height: Int
        ): RemoteViews {
            val layout = when {
                width < 155 -> R.layout.widget_2x2
                height <= 92 -> R.layout.widget_4x1
                width < 235 -> R.layout.widget_3x2
                width >= 300 -> R.layout.widget_5x2
                else -> R.layout.widget_4x2
            }

            val views = RemoteViews(context.packageName, layout)
            bindClicks(context, views, summary)

            when (layout) {
                R.layout.widget_4x1 -> bindCompact(views, summary)
                R.layout.widget_2x2 -> bindCompact(views, summary)
                R.layout.widget_3x2 -> bindList(views, summary, 2)
                R.layout.widget_5x2 -> bindList(views, summary, 3)
                else -> bindList(views, summary, 4)
            }

            return views
        }

        private fun bindClicks(
            context: Context,
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = "ir.ramezani.expensenotebook.OPEN_WIDGET"
                putExtra(MainActivity.EXTRA_OPEN_MODE, summary.mode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
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
                    pendingIntentFlags()
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_plus,
                PendingIntent.getActivity(
                    context,
                    101,
                    addIntent,
                    pendingIntentFlags()
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_gear,
                PendingIntent.getActivity(
                    context,
                    102,
                    settingsIntent,
                    pendingIntentFlags()
                )
            )
        }

        private fun bindCompact(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            setCommonText(views, summary)
            views.setTextViewText(
                R.id.widget_items,
                ""
            )
        }

        private fun bindList(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary,
            maxItems: Int
        ) {
            setCommonText(views, summary)

            val text = if (summary.expenses.isEmpty()) {
                summary.itemsLine
            } else {
                val latest = summary.expenses
                    .sortedByDescending { it.idx }
                    .take(maxItems)

                val lines = latest.map { item ->
                    val title = trimTitle(item.title, 12)
                    "$title — ${ExpenseDataUtils.formatNumber(item.price)}"
                }.toMutableList()

                val remaining = summary.expenses.size - latest.size
                if (remaining > 0) {
                    lines.add(
                        "+${ExpenseDataUtils.toPersianDigits(remaining.toString())}"
                    )
                }

                lines.joinToString("\n")
            }

            views.setTextViewText(R.id.widget_items, text)
        }

        private fun setCommonText(
            views: RemoteViews,
            summary: ExpenseDataUtils.WidgetSummary
        ) {
            views.setTextViewText(
                R.id.widget_label,
                summary.label
            )
            views.setTextViewText(
                R.id.widget_date_primary,
                plainDate(summary)
            )
            views.setTextViewText(
                R.id.widget_date_secondary,
                summary.secondaryDate
            )
            views.setTextViewText(
                R.id.widget_total,
                ExpenseDataUtils.formatNumber(summary.total)
            )
        }

        private fun plainDate(
            summary: ExpenseDataUtils.WidgetSummary
        ): String {
            return summary.primaryDate
                .replace("|", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun trimTitle(
            title: String,
            maxLength: Int
        ): String {
            return if (title.length <= maxLength) {
                title
            } else {
                title.take(maxLength - 1) + "…"
            }
        }

        private fun pendingIntentFlags(): Int {
            return PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
        }
    }
}
