package com.caoxian.schoolschedule.ui

import com.caoxian.schoolschedule.data.BreakPeriod
import com.caoxian.schoolschedule.data.ScheduleEntry
import com.caoxian.schoolschedule.data.computeBreaks

/**
 * 当前时刻对应的作息状态。
 *
 * @param nowMinute 当前时刻（分钟）
 * @param currentEntries 正在进行的节点（「全部」轮次下可能同时有多个）
 * @param currentBreaks 正处于其中的休息段（课间休息 / 长间休）
 * @param nextEntry 下一个节点
 * @param nextBreak 下一个休息段
 */
data class ScheduleStatus(
    val nowMinute: Int,
    val currentEntries: List<ScheduleEntry>,
    val currentBreaks: List<BreakPeriod>,
    val nextEntry: ScheduleEntry?,
    val nextBreak: BreakPeriod?,
)

/**
 * 根据排序后的作息列表与当前分钟数计算状态（休息段由时间表自动推导）。
 *
 * @param sortedEntries 按开始时间排序（同刻按轮次稳定排序）的节点列表
 * @param nowMinute 当前时刻（分钟，从 0 点算起）
 */
fun computeStatus(sortedEntries: List<ScheduleEntry>, nowMinute: Int): ScheduleStatus =
    computeStatus(sortedEntries, computeBreaks(sortedEntries), nowMinute)

/**
 * 根据排序后的作息列表、推导出的休息段与当前分钟数计算状态。
 *
 * @param sortedEntries 按开始时间排序的节点列表
 * @param breaks 由 [computeBreaks] 推导的休息段（须与 sortedEntries 同视图）
 * @param nowMinute 当前时刻（分钟，从 0 点算起）
 */
fun computeStatus(
    sortedEntries: List<ScheduleEntry>,
    breaks: List<BreakPeriod>,
    nowMinute: Int,
): ScheduleStatus {
    val current = sortedEntries.filter { nowMinute in it.startMinute until it.endForStatus }
    val currentBreaks = breaks.filter { nowMinute in it.startMinute until it.endMinute }
    val next = sortedEntries.firstOrNull { it.startMinute > nowMinute }
    val nextBreak = breaks.firstOrNull { it.startMinute > nowMinute }
    return ScheduleStatus(
        nowMinute = nowMinute,
        currentEntries = current,
        currentBreaks = currentBreaks,
        nextEntry = next,
        nextBreak = nextBreak,
    )
}
