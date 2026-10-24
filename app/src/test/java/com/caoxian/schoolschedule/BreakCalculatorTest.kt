package com.caoxian.schoolschedule

import com.caoxian.schoolschedule.data.BreakKind
import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.data.ScheduleEntry
import com.caoxian.schoolschedule.data.computeBreaks
import com.caoxian.schoolschedule.data.summerTimetable
import com.caoxian.schoolschedule.data.winterTimetable
import com.caoxian.schoolschedule.ui.computeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 休息时间段（课间休息 / 长间休）推导测试。
 */
class BreakCalculatorTest {

    private fun m(hour: Int, minute: Int) = hour * 60 + minute

    private fun entriesFor(round: Round, winter: Boolean = true) =
        (if (winter) winterTimetable else summerTimetable).entries
            .filter { round == Round.ALL || it.round == round || it.round == Round.ALL }
            .sortedWith(compareBy({ it.startMinute }, { it.round.ordinal }))

    private fun breaksOf(round: Round, winter: Boolean = true) =
        computeBreaks(entriesFor(round, winter))

    @Test
    fun `冬季全部视图推导出全部课间与长间休`() {
        val breaks = breaksOf(Round.ALL)
        val times = breaks.map { it.timeText }
        // 两轮并行活动合并后，各段之间的空隙
        assertTrue("缺少 9:05—9:15 课间", times.contains("9:05—9:15"))
        assertTrue("缺少 10:00—10:20 大课间", times.contains("10:00—10:20"))
        assertTrue("缺少 12:35—14:10 午间长休", times.contains("12:35—14:10"))
        assertTrue("缺少 15:25—15:35 课间", times.contains("15:25—15:35"))
        assertTrue("缺少 18:05—18:30 晚饭后长休", times.contains("18:05—18:30"))
        assertTrue("缺少 19:10—19:20 课间", times.contains("19:10—19:20"))
        // 6:50—6:55 与 7:20—7:25 的 5 分钟换场空隙
        assertTrue("缺少 6:50—6:55 换场", times.contains("6:50—6:55"))
        assertTrue("缺少 7:20—7:25 换场", times.contains("7:20—7:25"))
    }

    @Test
    fun `冬季休息段类型与时长`() {
        val breaks = breaksOf(Round.ALL)
        val breakAt = breaks.first { it.timeText == "9:05—9:15" }
        assertEquals(BreakKind.CLASS_BREAK, breakAt.kind)
        assertEquals(10, breakAt.durationMinutes)
        assertEquals("上午", breakAt.section)

        val long = breaks.first { it.timeText == "10:00—10:20" }
        assertEquals(BreakKind.LONG_BREAK, long.kind)
        assertEquals(20, long.durationMinutes)

        val noon = breaks.first { it.timeText == "12:35—14:10" }
        assertEquals(BreakKind.LONG_BREAK, noon.kind)
        assertEquals(95, noon.durationMinutes)
    }

    @Test
    fun `同一空隙只产生一个休息段`() {
        val breaks = breaksOf(Round.ALL)
        // 6:50 同时结束一轮早操与二轮晨读，只应有一个 6:50—6:55 休息段
        assertEquals(1, breaks.count { it.timeText == "6:50—6:55" })
    }

    @Test
    fun `第一轮视图按本轮安排推导`() {
        val breaks = breaksOf(Round.FIRST)
        val times = breaks.map { it.timeText }
        // 一轮早饭 7:15 结束、晨读 7:40 开始 → 25 分钟长间休
        assertTrue("缺少 7:15—7:40 长间休", times.contains("7:15—7:40"))
        assertTrue("缺少 9:05—9:15 课间", times.contains("9:05—9:15"))
    }

    @Test
    fun `夏季表推导含午休后长间休`() {
        val breaks = breaksOf(Round.ALL, winter = false)
        val times = breaks.map { it.timeText }
        // 午休 13:00—14:20 后接素质提升 14:30 → 10 分钟
        assertTrue("缺少 14:20—14:30 课间", times.contains("14:20—14:30"))
        // 晚自习第二节 20:30 后是单点活动，不产生休息段
        assertTrue(breaks.none { it.startMinute >= m(20, 30) })
    }

    @Test
    fun `小于最小空隙不产生休息段`() {
        val entries = listOf(
            ScheduleEntry("上午", "第一节", m(8, 0), m(8, 45)),
            ScheduleEntry("上午", "第二节", m(8, 49), m(9, 30)), // 4 分钟空隙
        )
        assertTrue(computeBreaks(entries).isEmpty())
        val entries2 = entries.mapIndexed { i, e ->
            if (i == 1) e.copy(startMinute = m(8, 50)) else e // 5 分钟空隙
        }
        val breaks = computeBreaks(entries2)
        assertEquals(1, breaks.size)
        assertEquals("8:45—8:50", breaks.first().timeText)
    }

    @Test
    fun `无区间节点时无休息段`() {
        val entries = listOf(
            ScheduleEntry("就寝", "起床", m(6, 0)),
            ScheduleEntry("就寝", "熄灯", m(21, 10)),
        )
        assertTrue(computeBreaks(entries).isEmpty())
        assertTrue(computeBreaks(emptyList()).isEmpty())
    }

    @Test
    fun `重叠区间合并后连续占用不产生休息段`() {
        val entries = listOf(
            ScheduleEntry("上午", "第一节", m(8, 0), m(8, 45), Round.FIRST),
            ScheduleEntry("上午", "第一节", m(8, 0), m(8, 50), Round.SECOND),
            ScheduleEntry("上午", "第二节", m(8, 50), m(9, 30)),
        )
        // 两轮第一节重叠 + 第二节紧接 → 无空隙
        assertTrue(computeBreaks(entries).isEmpty())
    }

    @Test
    fun `休息中状态计算`() {
        val e = entriesFor(Round.ALL)
        // 9:10 处于 9:05—9:15 课间，无节点进行中
        val status = computeStatus(e, m(9, 10))
        assertTrue(status.currentEntries.isEmpty())
        assertEquals(1, status.currentBreaks.size)
        assertEquals("9:05—9:15", status.currentBreaks.first().timeText)
        assertEquals("第二节", status.nextEntry?.activity)
        // 休息段结束瞬间（9:15）不再算休息中，第二节已开始
        val after = computeStatus(e, m(9, 15))
        assertTrue(after.currentBreaks.isEmpty())
        assertEquals(listOf("第二节"), after.currentEntries.map { it.activity })
    }

    @Test
    fun `午间长休中状态计算`() {
        val e = entriesFor(Round.ALL)
        val status = computeStatus(e, m(14, 0))
        assertEquals(1, status.currentBreaks.size)
        assertEquals("长间休", status.currentBreaks.first().kind.label)
        assertEquals("素质提升", status.nextEntry?.activity)
    }
}
