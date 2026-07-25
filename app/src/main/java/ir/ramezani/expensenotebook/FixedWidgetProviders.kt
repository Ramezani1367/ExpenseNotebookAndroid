package ir.ramezani.expensenotebook
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews

abstract class FixedWidgetProvider(private val layout: Int) : AppWidgetProvider() {
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ update(c,m,ids) }
 override fun onAppWidgetOptionsChanged(c:Context,m:AppWidgetManager,id:Int,o:Bundle){ update(c,m,intArrayOf(id)) }
 private fun update(c:Context,m:AppWidgetManager,ids:IntArray){
  val s=ExpenseDataUtils.summary(c); ids.forEach { id ->
   val v=RemoteViews(c.packageName,layout); ExpenseWidgetProvider.bindFixed(c,v,s,layout); m.updateAppWidget(id,v)
  }
 }
}
class Widget2x2Provider:FixedWidgetProvider(R.layout.widget_2x2)
class Widget3x2Provider:FixedWidgetProvider(R.layout.widget_3x2)
class Widget4x1Provider:FixedWidgetProvider(R.layout.widget_4x1)
class Widget5x2Provider:FixedWidgetProvider(R.layout.widget_5x2)
