package com.caoxian.schoolschedule.util

import android.content.Context
import com.caoxian.schoolschedule.R
import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.data.ScheduleEntry

/**
 * 作息表数据本地化：数据文件中的时段 / 活动名 / 轮次名 / 时间表名均为中文原文，
 * 展示时按当前语言映射为对应资源；无映射时原样返回（未知数据兜底）。
 */
object ScheduleI18n {

    private val sectionMap = mapOf(
        "早晨" to R.string.sect_morning,
        "上午" to R.string.sect_am,
        "下午" to R.string.sect_pm,
        "晚自习" to R.string.sect_evening,
        "就寝" to R.string.sect_bedtime,
    )

    private val activityMap = mapOf(
        "起床" to R.string.act_wake_up,
        "晨读" to R.string.act_morning_read,
        "早操、晨会" to R.string.act_morning_ex,
        "早饭" to R.string.act_breakfast,
        "预备" to R.string.act_prepare,
        "第一节" to R.string.act_p1,
        "第二节" to R.string.act_p2,
        "第三节" to R.string.act_p3,
        "第四节" to R.string.act_p4,
        "第五节" to R.string.act_p5,
        "第六节" to R.string.act_p6,
        "第七节" to R.string.act_p7,
        "午饭" to R.string.act_lunch,
        "午休" to R.string.act_noon_break,
        "素质提升" to R.string.act_enrichment,
        "宿舍检查" to R.string.act_dorm_check,
        "晚饭" to R.string.act_dinner,
        "晚自习" to R.string.act_evening_study,
        "开始进入餐厅" to R.string.act_to_canteen,
        "开始从集合点出发" to R.string.act_depart,
        "就寝" to R.string.sect_bedtime,
        "熄灯" to R.string.act_lights_out,
    )

    private val roundMap = mapOf(
        "第一轮" to R.string.round_first,
        "第二轮" to R.string.round_second,
        "全部" to R.string.round_all,
    )

    private val roundShortMap = mapOf(
        "一轮" to R.string.round_first_short,
        "二轮" to R.string.round_second_short,
        "全部" to R.string.round_all,
    )

    private val timetableMap = mapOf(
        "冬季作息" to R.string.tt_winter,
        "夏季作息" to R.string.tt_summer,
        "冬季作息时间表" to R.string.tt_winter_full,
        "夏季作息时间表" to R.string.tt_summer_full,
        "曹县高级技工学校" to R.string.school_name,
    )

    private val effectiveMap = mapOf(
        "2026年10月1日起执行" to R.string.effective_winter,
        "2026年5月1日起执行" to R.string.effective_summer,
    )

    private fun Map<String, Int>.localize(context: Context, name: String): String =
        this[name]?.let { context.getString(it) } ?: name

    fun section(context: Context, name: String): String = sectionMap.localize(context, name)

    fun activity(context: Context, name: String): String = activityMap.localize(context, name)

    fun roundName(context: Context, displayName: String): String = roundMap.localize(context, displayName)

    fun roundShort(context: Context, shortName: String): String = roundShortMap.localize(context, shortName)

    fun timetable(context: Context, name: String): String = timetableMap.localize(context, name)

    fun effective(context: Context, text: String): String = effectiveMap.localize(context, text)

    /** 备注本地化（如「不能提前下课」） */
    fun note(context: Context, text: String): String =
        if (text == "不能提前下课") context.getString(R.string.note_no_leave) else text

    /** 展示用名称：非「全部」轮次带轮次前缀，与首页 / 小组件一致 */
    fun entryLabel(context: Context, entry: ScheduleEntry): String =
        if (entry.round == Round.ALL) {
            activity(context, entry.activity)
        } else {
            "${roundName(context, entry.round.displayName)} ${activity(context, entry.activity)}"
        }
}
