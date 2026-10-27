package com.caoxian.schoolschedule.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.caoxian.schoolschedule.MainActivity
import com.caoxian.schoolschedule.R
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.widget.WidgetData

/**
 * 常驻状态通知：
 * - 上课 / 活动进行期间不提醒（自动静默撤销）；
 * - 课间、即将开始、放学后等其他时间展示当前状态与倒计时；
 * - 跟随小组件的分钟级刷新节奏（WIDGET_TICK 广播）同步更新。
 */
object NotificationHelper {

    private const val CHANNEL_ID = "schedule_status"
    private const val NOTIFICATION_ID = 1001

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val settings = SettingsStore(appContext)
        if (!settings.notificationEnabled) {
            cancel(appContext)
            return
        }
        val texts = WidgetData.build(appContext)
        if (texts.inClass) {
            // 上课期间不要提醒：撤销已有通知
            cancel(appContext)
            return
        }
        val manager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val contentText = buildString {
            append(texts.detail)
            if (texts.countdownPrefix != null && texts.countdownSec != null && texts.countdownSec > 0) {
                val minutes = (texts.countdownSec + 59) / 60
                append(" · ").append(texts.countdownPrefix).append(" ")
                    .append(ScheduleI18n.duration(appContext, minutes))
            }
        }
        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(appContext, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(appContext)
        }
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(texts.stateLabel)
            .setContentText(contentText)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT < 26) {
            builder.setPriority(Notification.PRIORITY_LOW)
        }
        runCatching { manager.notify(NOTIFICATION_ID, builder.build()) }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val manager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
