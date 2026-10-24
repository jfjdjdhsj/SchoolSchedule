package com.caoxian.schoolschedule.data

import com.caoxian.schoolschedule.util.fmtTime

/**
 * 休息段类型。
 * 由相邻教学活动之间的空隙自动推导：
 *  - [CLASS_BREAK] 课间休息（15 分钟以内，含课间换场）
 *  - [LONG_BREAK] 长间休（15 分钟以上，如大课间、午间休息）
 */
enum class BreakKind(val label: String) {
    CLASS_BREAK("课间休息"),
    LONG_BREAK("长间休"),
}

/**
 * 一段由时间表推导出的休息时间。
 *
 * @param startMinute 开始时间（上一节点结束时刻）
 * @param endMinute 结束时间（下一节点开始时刻）
 * @param kind 休息类型
 * @param section 归属时段（取上一节点所在时段，如「上午」）
 */
data class BreakPeriod(
    val startMinute: Int,
    val endMinute: Int,
    val kind: BreakKind,
    val section: String,
) {
    /** 休息时长（分钟） */
    val durationMinutes: Int
        get() = endMinute - startMinute

    /** 格式化的时间段文本，如「9:05—9:15」 */
    val timeText: String
        get() = "${fmtTime(startMinute)}—${fmtTime(endMinute)}"
}
