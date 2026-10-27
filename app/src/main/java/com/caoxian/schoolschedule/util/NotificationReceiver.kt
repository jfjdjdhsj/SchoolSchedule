package com.caoxian.schoolschedule.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 常驻通知刷新：与小组件共用同一分钟级广播（WIDGET_TICK / 开机 / 时间变化），
 * 收到后按「上课期间静默、其他时间提醒」的规则刷新通知。
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.refresh(context)
    }
}
