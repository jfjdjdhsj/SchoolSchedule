package com.caoxian.schoolschedule.data

/**
 * 从作息时间表推导休息时间段。
 *
 * 算法：取全部区间型节点（有结束时间的活动，单点时刻如「起床 6:00」不参与），
 * 按开始时间排序后合并重叠区间（第一轮 / 第二轮活动并行时视为连续占用），
 * 相邻合并区间之间 >= [minGapMinutes] 的空隙即休息段：
 *  - 15 分钟以内 → 课间休息
 *  - 15 分钟以上 → 长间休（大课间、午间休息等）
 *
 * @param entries 已按开始时间排序的节点列表（可为任意轮次过滤后的视图）
 * @param minGapMinutes 认定为休息的最小空隙分钟数，默认 5
 */
fun computeBreaks(entries: List<ScheduleEntry>, minGapMinutes: Int = 5): List<BreakPeriod> {
    val ranges = entries
        .filter { it.endMinute != null }
        .map { it.startMinute to it.endMinute!! }
        .sortedBy { it.first }
    if (ranges.isEmpty()) return emptyList()

    // 合并重叠 / 相接区间
    val merged = mutableListOf<Pair<Int, Int>>()
    for ((start, end) in ranges) {
        val last = merged.lastOrNull()
        if (last != null && start <= last.second) {
            merged[merged.lastIndex] = last.first to maxOf(last.second, end)
        } else {
            merged.add(start to end)
        }
    }

    // 相邻区间的空隙 → 休息段
    return buildList {
        for (i in 0 until merged.size - 1) {
            val gapStart = merged[i].second
            val gapEnd = merged[i + 1].first
            val gap = gapEnd - gapStart
            if (gap >= minGapMinutes) {
                add(
                    BreakPeriod(
                        startMinute = gapStart,
                        endMinute = gapEnd,
                        kind = if (gap <= 15) BreakKind.CLASS_BREAK else BreakKind.LONG_BREAK,
                        section = sectionOf(entries, gapStart),
                    ),
                )
            }
        }
    }
}

/** 空隙开始时刻所属时段：取结束时刻等于该时刻的节点，取不到则取此前最近的节点 */
private fun sectionOf(entries: List<ScheduleEntry>, gapStart: Int): String =
    entries.firstOrNull { it.endMinute == gapStart }?.section
        ?: entries.filter { it.startMinute <= gapStart }
            .maxByOrNull { it.startMinute }?.section
        ?: "其他"
