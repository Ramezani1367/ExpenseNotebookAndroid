package ir.ramezani.expensenotebook

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews

abstract class FixedWidgetProvider(private val layout: Int) : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        update(c, m, ids)
    }

    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: Bundle) {
        update(c, m, intArrayOf(id))
    }

    private fun update(c: Context, m: AppWidgetManager, ids: IntArray) {
        updateLayout(c, m, ids, layout)
    }

    companion object {
        /** Refreshes instances belonging to one of the fixed-size receivers. */
        fun updateWidgets(
            c: Context,
            m: AppWidgetManager,
            ids: IntArray,
            provider: Class<*>
        ) {
            val layout = when (provider) {
                Widget2x2Provider::class.java -> R.layout.widget_2x2
                Widget3x2Provider::class.java -> R.layout.widget_3x2
                Widget4x1Provider::class.java -> R.layout.widget_4x1
                Widget5x2Provider::class.java -> R.layout.widget_5x2
                else -> return
            }
            updateLayout(c, m, ids, layout)
        }

        private fun updateLayout(c: Context, m: AppWidgetManager, ids: IntArray, layout: Int) {
            val summary = ExpenseDataUtils.summary(c)
            ids.forEach { id ->
                val views = RemoteViews(c.packageName, layout)
                ExpenseWidgetProvider.bindFixed(c, views, summary, layout)
                m.updateAppWidget(id, views)
            }
        }
    }
}

class Widget2x2Provider : FixedWidgetProvider(R.layout.widget_2x2)
class Widget3x2Provider : FixedWidgetProvider(R.layout.widget_3x2)
class Widget4x1Provider : FixedWidgetProvider(R.layout.widget_4x1)
class Widget5x2Provider : FixedWidgetProvider(R.layout.widget_5x2)
