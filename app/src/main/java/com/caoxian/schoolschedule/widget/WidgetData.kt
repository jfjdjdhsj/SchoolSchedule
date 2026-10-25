package com.caoxian.schoolschedule.widget

import android.content.Context
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.data.allTimetables
import com.caoxian.schoolschedule.data.computeBreaks
import com.caoxian.schoolschedule.R
import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.data.label
import com.caoxian.schoolschedule.ui.computeStatus
import com.caoxian.schoolschedule.util.currentMinutes
import com.caoxian.schoolschedule.util.currentSecondOfDay
import com.caoxian.schoolschedule.util.effectiveRound
import com.caoxian.schoolschedule.util.fmtTime
import com.caoxian.schoolschedule.util.formatDuration

/**
 * 桌面小组件数据构建：纯函数，供 WidgetProvider 渲染 RemoteViews。
 *
 * 文案与首页「现在状态卡片」（NowStatusCard）逐行对齐：
 * 进行中 / 休息中 / 即将开始 / 即将休息 / 今日作息已结束，
 * 倒计时使用秒级精度（比 APP 的分钟级更细，信息不冲突）。
 */
object WidgetData {

    /** 小组件展示文本 */
    data class Texts(
        val stateLabel: String,
        val detail: String,
        /** 倒计时前缀（如「距下课」）；null 表示无倒计时 */
        val countdownPrefix: String?,
        /** 倒计时剩余秒数；由 Chronometer 在桌面端每秒自走，不依赖后台广播 */
        val countdownSec: Int?,
    )

    fun build(context: Context): Texts {
        val settings = SettingsStore(context)
        val timetable = allTimetables.firstOrNull { it.id == settings.timetableId }
            ?: allTimetables.first()
        val round = effectiveRound(settings.round, settings.showAllRound)
        // 与首页一致：过滤轮次 + 排序
        val entries = timetable.entries
            .filter { round == Round.ALL || it.round == round || it.round == Round.ALL }
            .sortedWith(compareBy({ it.startMinute }, { it.round.ordinal }))
        val breaks = computeBreaks(entries)
        val nowMinute = currentMinutes()
        val status = computeStatus(entries, breaks, nowMinute)
        val nowSec = currentSecondOfDay()

        // 进行中：标题 = 多节点名称拼接；详情 = 时间段；倒计时 = 距最近结束
        if (status.currentEntries.isNotEmpty()) {
            val current = status.currentEntries
            val state = current.joinToString(" · ") { it.label }
            val times = current.joinToString(" · ") { it.timeText }
            val latestEnd = current.maxOf { it.endForStatus }
            val remainSec = latestEnd * 60 - nowSec
            return if (current.any { it.endMinute != null } && remainSec > 0) {
                Texts(state, times, context.getString(R.string.widget_countdown_class_over), remainSec)
            } else {
                Texts(state, times, null, null)
            }
        }

        // 休息中
        status.currentBreaks.firstOrNull()?.let { br ->
            val detail = status.nextEntry?.let {
                context.getString(R.string.status_next_distance, it.label, formatDuration(it.startMinute - nowMinute))
            } ?: br.timeText
            return Texts(
                stateLabel = context.getString(R.string.status_in_break, br.kind.label(context)),
                detail = detail,
                countdownPrefix = context.getString(R.string.widget_countdown_class_start),
                countdownSec = br.endMinute * 60 - nowSec,
            )
        }

        // 即将开始（与 APP 分支顺序一致：先节点后休息）
        status.nextEntry?.let { next ->
            return Texts(
                stateLabel = context.getString(R.string.status_upcoming_class),
                detail = "${next.label} ${next.timeText}",
                countdownPrefix = context.getString(R.string.widget_countdown_start),
                countdownSec = next.startMinute * 60 - nowSec,
            )
        }

        // 即将休息
        status.nextBreak?.let { br ->
            return Texts(
                stateLabel = context.getString(R.string.status_upcoming_break),
                detail = "${br.kind.label} ${br.timeText}",
                countdownPrefix = context.getString(R.string.widget_countdown_break),
                countdownSec = br.startMinute * 60 - nowSec,
            )
        }

        // 今日已结束
        return Texts(
            stateLabel = context.getString(R.string.status_day_over),
            detail = entries.firstOrNull()?.let { context.getString(R.string.widget_tomorrow, fmtTime(it.startMinute), it.activity) } ?: context.getString(R.string.status_rest_early),
            countdownPrefix = null,
            countdownSec = null,
        )
    }
}
