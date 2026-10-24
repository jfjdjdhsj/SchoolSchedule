package com.caoxian.schoolschedule.widget

import android.content.Context
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.data.allTimetables
import com.caoxian.schoolschedule.data.computeBreaks
import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.ui.computeStatus
import com.caoxian.schoolschedule.util.currentMinutes
import com.caoxian.schoolschedule.util.currentSecondOfDay
import com.caoxian.schoolschedule.util.effectiveRound
import com.caoxian.schoolschedule.util.fmtCountdown
import com.caoxian.schoolschedule.util.fmtTime

/**
 * 桌面小组件数据构建：纯函数，供 WidgetProvider 渲染 RemoteViews。
 *
 * 逻辑与首页一致：按设置的时间表 + 轮次过滤排序节点 → 推导休息段 →
 * computeStatus 计算当前状态 → 生成三行文本（状态 / 详情 / 倒计时）。
 */
object WidgetData {

    /** 小组件展示文本 */
    data class Texts(
        val stateLabel: String,
        val detail: String,
        val countdown: String,
    )

    fun build(context: Context): Texts {
        val settings = SettingsStore(context)
        val timetable = allTimetables.firstOrNull { it.id == settings.timetableId }
            ?: allTimetables.first()
        val round = effectiveRound(settings.round, settings.showAllRound)
        val entries = timetable.entries
            .filter { round == Round.ALL || it.round == round || it.round == Round.ALL }
            .sortedWith(compareBy({ it.startMinute }, { it.round.ordinal }))
        val breaks = computeBreaks(entries)
        val nowMinute = currentMinutes()
        val status = computeStatus(entries, breaks, nowMinute)
        val nowSec = currentSecondOfDay()

        // 进行中
        status.currentEntries.firstOrNull()?.let { entry ->
            val remainingSec = entry.endForStatus * 60 - nowSec
            val countdown = if (entry.endMinute == null) {
                "进行中"
            } else {
                "剩 ${fmtCountdown(remainingSec)}"
            }
            return Texts(
                stateLabel = "进行中 · ${entry.activity}",
                detail = "${entry.section} ${entry.timeText}",
                countdown = countdown,
            )
        }

        // 休息中
        status.currentBreaks.firstOrNull()?.let { breakPeriod ->
            val next = status.nextEntry
            val countdown = next?.let { "距${it.activity} ${fmtCountdown(it.startMinute * 60 - nowSec)}" } ?: ""
            return Texts(
                stateLabel = "休息中 · ${breakPeriod.kind.label}",
                detail = "${breakPeriod.section} ${breakPeriod.timeText}",
                countdown = countdown,
            )
        }

        // 空档期：距离更近的下一个事件（节点或休息开始）
        val nextEntry = status.nextEntry
        val nextBreak = status.nextBreak
        val entryStart = nextEntry?.startMinute ?: Int.MAX_VALUE
        val breakStart = nextBreak?.startMinute ?: Int.MAX_VALUE
        return when {
            nextEntry == null && nextBreak == null -> Texts(
                stateLabel = "已结束",
                detail = "今日作息已全部结束",
                countdown = "明日再会",
            )
            breakStart < entryStart -> Texts(
                stateLabel = "空闲 · 休息将至",
                detail = "休息 ${fmtTime(breakStart)}（${nextBreak!!.kind.label}）",
                countdown = "距休息 ${fmtCountdown(breakStart * 60 - nowSec)}",
            )
            else -> Texts(
                stateLabel = "空闲 · 待上课",
                detail = "下一节 ${nextEntry!!.activity} ${nextEntry.timeText}",
                countdown = "距开始 ${fmtCountdown(entryStart * 60 - nowSec)}",
            )
        }
    }
}
