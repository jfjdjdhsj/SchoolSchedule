package com.caoxian.schoolschedule.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.caoxian.schoolschedule.R

/**
 * 作息小组件基类：每分钟对齐整分钟刷新（AlarmManager 广播），
 * 开机 / 时间变更自动恢复；点击任意区域直达 APP 首页。
 *
 * 子类只提供布局资源（4×1 单行 / 4×2 详细）。
 */
abstract class BaseScheduleWidgetProvider : AppWidgetProvider() {

    abstract fun layoutRes(): Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, manager, appWidgetIds)
        scheduleTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java))
                render(context, manager, ids)
                scheduleTick(context)
            }
        }
    }

    override fun onEnabled(context: Context) = scheduleTick(context)

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(tickPendingIntent(context))
    }

    /** 渲染全部实例 */
    private fun render(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val texts = WidgetData.build(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, layoutRes()).apply {
                setTextViewText(R.id.widget_state, texts.stateLabel)
                setTextViewText(R.id.widget_detail, texts.detail)
                setTextViewText(R.id.widget_countdown, texts.countdown)
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
            manager.updateAppWidget(id, views)
        }
    }

    /** 下一分钟整点触发刷新（对齐分界，保证倒计时从满分钟开始） */
    private fun scheduleTick(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextMinute = System.currentTimeMillis() + 60_000L
        am.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextMinute - (nextMinute % 60_000L),
            tickPendingIntent(context),
        )
    }

    private fun tickPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, this::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_TICK = "com.caoxian.schoolschedule.WIDGET_TICK"
    }
}

/** 4×1 单行小组件 */
class ScheduleWidget4x1 : BaseScheduleWidgetProvider() {
    override fun layoutRes(): Int = R.layout.widget_schedule_4x1
}

/** 4×2 详细小组件 */
class ScheduleWidget4x2 : BaseScheduleWidgetProvider() {
    override fun layoutRes(): Int = R.layout.widget_schedule_4x2
}
