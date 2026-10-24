package com.caoxian.schoolschedule.util

import java.util.Calendar

/** 分钟数格式化为「H:mm」，如 380 -> 「6:20」 */
fun fmtTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "$h:${if (m < 10) "0$m" else "$m"}"
}

/** 当前时刻（分钟，从 0 点算起） */
fun currentMinutes(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

/** 当前时刻的时:分:秒文本，如「17:44:09」 */
fun currentClockText(): String {
    val calendar = Calendar.getInstance()
    val h = calendar.get(Calendar.HOUR_OF_DAY)
    val m = calendar.get(Calendar.MINUTE)
    val s = calendar.get(Calendar.SECOND)
    return "%02d:%02d:%02d".format(h, m, s)
}

/** 时长格式化为「25分钟」「1小时30分钟」 */
fun formatDuration(totalMinutes: Int): String = when {
    totalMinutes <= 0 -> "0分钟"
    totalMinutes < 60 -> "${totalMinutes}分钟"
    else -> {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        if (m == 0) "${h}小时" else "${h}小时${m}分钟"
    }
}
