package com.caoxian.schoolschedule.data

import com.caoxian.schoolschedule.util.fmtTime

/**
 * 作息轮次。
 * 学校分两个轮次组织早晨活动与就餐（第一轮 / 第二轮），[ALL] 表示同时展示两套安排。
 */
enum class Round(val displayName: String, val shortName: String) {
    ALL("全部", "全部"),
    FIRST("第一轮", "一轮"),
    SECOND("第二轮", "二轮"),
}

/**
 * 一天中的单个作息节点。
 *
 * @param section 所属时段，如「早晨」「上午」「下午」「晚自习」「就寝」
 * @param activity 活动名称，如「第一节」「早饭」
 * @param startMinute 开始时间（分钟，从 0 点算起）
 * @param endMinute 结束时间（分钟）；单点活动（如「起床 6:00」）为 null
 * @param round 适用轮次
 * @param note 备注，如「不能提前下课」
 */
data class ScheduleEntry(
    val section: String,
    val activity: String,
    val startMinute: Int,
    val endMinute: Int? = null,
    val round: Round = Round.ALL,
    val note: String? = null,
) {
    /** 格式化的时间段文本，如「6:20—6:50」 */
    val timeText: String
        get() = if (endMinute != null) {
            "${fmtTime(startMinute)}—${fmtTime(endMinute)}"
        } else {
            fmtTime(startMinute)
        }

    /** 计算「进行中」状态时的结束边界；单点活动按 10 分钟展示窗口处理 */
    val endForStatus: Int
        get() = endMinute ?: (startMinute + 10)
}

/**
 * 一套作息时间表（冬季 / 夏季）。
 */
data class Timetable(
    val id: String,
    val name: String,
    val shortName: String,
    val schoolName: String,
    val effectiveText: String,
    val entries: List<ScheduleEntry>,
)
