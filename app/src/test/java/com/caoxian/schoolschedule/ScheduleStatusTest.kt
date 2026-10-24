package com.caoxian.schoolschedule

import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.data.ScheduleEntry
import com.caoxian.schoolschedule.data.Timetable
import com.caoxian.schoolschedule.data.allTimetables
import com.caoxian.schoolschedule.data.autoTimetableId
import com.caoxian.schoolschedule.data.summerTimetable
import com.caoxian.schoolschedule.data.winterTimetable
import com.caoxian.schoolschedule.ui.computeStatus
import com.caoxian.schoolschedule.util.fmtTime
import com.caoxian.schoolschedule.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「当前时刻匹配的时间段高亮」判定测试。
 *
 * 覆盖：单点活动窗口、区间左闭右开边界、轮次重叠（同刻两轮同时进行）、
 * 空闲间隙的下一项预告、夜间已结束状态，以及时间格式化工具。
 */
class ScheduleStatusTest {

    /** 与界面 ScheduleTimeline 一致的过滤 + 排序逻辑 */
    private fun entriesFor(timetable: Timetable, round: Round): List<ScheduleEntry> =
        timetable.entries
            .filter { round == Round.ALL || it.round == round || it.round == Round.ALL }
            .sortedWith(compareBy({ it.startMinute }, { it.round.ordinal }))

    /** 当前高亮的节点（轮次/活动名，便于区分两轮同名活动） */
    private fun currentLabels(entries: List<ScheduleEntry>, now: Int): List<String> =
        computeStatus(entries, now).currentEntries.map { "${it.round.shortName}/${it.activity}" }

    private fun nextLabel(entries: List<ScheduleEntry>, now: Int): String? =
        computeStatus(entries, now).nextEntry?.let { "${it.round.shortName}/${it.activity}" }

    private fun m(hour: Int, minute: Int) = hour * 60 + minute

    // ---------- 冬季表 · 全部轮次 ----------

    @Test
    fun `冬季 起床前显示下一项为起床`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(emptyList<String>(), currentLabels(e, m(5, 59)))
        assertEquals("全部/起床", nextLabel(e, m(5, 59)))
    }

    @Test
    fun `冬季 单点活动起床有十分钟高亮窗口`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(listOf("全部/起床"), currentLabels(e, m(6, 0)))
        assertEquals(listOf("全部/起床"), currentLabels(e, m(6, 9)))
        assertEquals(emptyList<String>(), currentLabels(e, m(6, 10)))
        assertEquals("一轮/早操、晨会", nextLabel(e, m(6, 10)))
    }

    @Test
    fun `冬季 早操与二轮晨读同时高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 6:20 起一轮早操（6:20—6:50）与二轮晨读（6:20—6:50）同时进行，同时高亮
        assertEquals(listOf("一轮/早操、晨会", "二轮/晨读"), currentLabels(e, m(6, 20)))
        assertEquals(listOf("一轮/早操、晨会", "二轮/晨读"), currentLabels(e, m(6, 25)))
        assertEquals("一轮/早饭", nextLabel(e, m(6, 25)))
    }

    @Test
    fun `冬季 区间结束瞬间不再高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 早操 6:20—6:50：6:50 整已结束
        assertEquals(emptyList<String>(), currentLabels(e, m(6, 50)))
        assertEquals("一轮/早饭", nextLabel(e, m(6, 50)))
    }

    @Test
    fun `冬季 空闲间隙无高亮但预告下一项`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 9:05 第一节结束、9:15 第二节开始的间隙
        assertEquals(emptyList<String>(), currentLabels(e, m(9, 10)))
        assertEquals("全部/第二节", nextLabel(e, m(9, 10)))
    }

    @Test
    fun `冬季 第一轮早饭后高亮第二轮早操`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 7:16：一轮早饭 7:15 结束，二轮早操 6:55—7:20 进行中
        assertEquals(listOf("二轮/早操、晨会"), currentLabels(e, m(7, 16)))
        // 下一项为 7:25 的二轮早饭（早于 7:40 的一轮晨读）
        assertEquals("二轮/早饭", nextLabel(e, m(7, 16)))
    }

    @Test
    fun `冬季 两轮第四节同刻同时高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 11:20：一轮第四节 11:15—11:35、二轮第四节 11:15—12:00 同时进行
        assertEquals(listOf("一轮/第四节", "二轮/第四节"), currentLabels(e, m(11, 20)))
    }

    @Test
    fun `冬季 一轮第四节结束后仅剩二轮第四节高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(listOf("二轮/第四节"), currentLabels(e, m(11, 36)))
        assertEquals("一轮/午饭", nextLabel(e, m(11, 36)))
    }

    @Test
    fun `冬季 午饭时分按轮次错峰高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        // 11:50：一轮午饭 11:40 开餐，与二轮第四节（至 12:00）重叠，同时高亮
        assertEquals(listOf("二轮/第四节", "一轮/午饭"), currentLabels(e, m(11, 50)))
        assertEquals(emptyList<String>(), currentLabels(e, m(12, 7)))
        assertEquals(listOf("二轮/午饭"), currentLabels(e, m(12, 30)))
    }

    @Test
    fun `冬季 素质提升与晚自习节点高亮`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(listOf("全部/素质提升"), currentLabels(e, m(14, 15)))
        assertEquals(listOf("全部/第一节"), currentLabels(e, m(18, 45)))
    }

    @Test
    fun `冬季 宿舍检查为单点窗口`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(listOf("全部/宿舍检查"), currentLabels(e, m(20, 40)))
        assertEquals(listOf("全部/宿舍检查"), currentLabels(e, m(20, 49)))
        assertEquals(emptyList<String>(), currentLabels(e, m(20, 50)))
        assertEquals("全部/熄灯", nextLabel(e, m(20, 50)))
    }

    @Test
    fun `冬季 熄灯后今日已结束`() {
        val e = entriesFor(winterTimetable, Round.ALL)
        assertEquals(listOf("全部/熄灯"), currentLabels(e, m(21, 10)))
        assertEquals(emptyList<String>(), currentLabels(e, m(21, 20)))
        assertNull(nextLabel(e, m(21, 20)))
    }

    // ---------- 轮次过滤视图 ----------

    @Test
    fun `第一轮视图只高亮第一轮节点`() {
        val e = entriesFor(winterTimetable, Round.FIRST)
        assertEquals(listOf("一轮/早操、晨会"), currentLabels(e, m(6, 25)))
        // 7:20 一轮早饭 7:15 已结束、晨读 7:40 未开始
        assertEquals(emptyList<String>(), currentLabels(e, m(7, 20)))
        assertEquals("一轮/晨读", nextLabel(e, m(7, 20)))
    }

    @Test
    fun `第二轮视图只高亮第二轮节点`() {
        val e = entriesFor(winterTimetable, Round.SECOND)
        assertEquals(listOf("二轮/早操、晨会"), currentLabels(e, m(7, 0)))
        assertEquals(listOf("二轮/早饭"), currentLabels(e, m(7, 35)))
    }

    // ---------- 夏季表抽查 ----------

    @Test
    fun `夏季 午休时段高亮`() {
        val e = entriesFor(summerTimetable, Round.ALL)
        assertEquals(listOf("全部/午休"), currentLabels(e, m(13, 10)))
        assertEquals(emptyList<String>(), currentLabels(e, m(14, 25)))
    }

    @Test
    fun `夏季 预备窗口与第五节无缝衔接`() {
        val e = entriesFor(summerTimetable, Round.ALL)
        // 预备 15:00 单点活动有 10 分钟高亮窗口（15:00—15:10）
        assertEquals(listOf("全部/预备"), currentLabels(e, m(15, 5)))
        // 15:12 预备窗口结束，第五节（15:10—15:55）无缝开始，下一项为第六节
        assertEquals(listOf("全部/第五节"), currentLabels(e, m(15, 12)))
        assertEquals("全部/第六节", nextLabel(e, m(15, 12)))
    }

    @Test
    fun `夏季 熄灯时间晚于冬季`() {
        val e = entriesFor(summerTimetable, Round.ALL)
        assertEquals(listOf("全部/熄灯"), currentLabels(e, m(21, 30)))
    }

    // ---------- 数据与工具函数 ----------

    @Test
    fun `自动选表按月份切换`() {
        assertEquals("summer", autoTimetableId(5))
        assertEquals("summer", autoTimetableId(9))
        assertEquals("winter", autoTimetableId(10))
        assertEquals("winter", autoTimetableId(4))
    }

    @Test
    fun `时间格式化`() {
        assertEquals("6:20", fmtTime(m(6, 20)))
        assertEquals("0:00", fmtTime(0))
        assertEquals("23:59", fmtTime(m(23, 59)))
        assertEquals("45分钟", formatDuration(45))
        assertEquals("1小时", formatDuration(60))
        assertEquals("1小时30分钟", formatDuration(90))
    }

    @Test
    fun `节点时间段文本`() {
        val single = winterTimetable.entries.first { it.activity == "起床" }
        assertEquals("6:00", single.timeText)
        val range = winterTimetable.entries.first { it.activity == "第一节" && it.round == Round.ALL }
        assertEquals("8:20—9:05", range.timeText)
    }

    @Test
    fun `两套时间表均包含关键节点`() {
        for (timetable in allTimetables) {
            val names = timetable.entries.map { it.activity }
            for (required in listOf("起床", "早饭", "第一节", "第七节", "晚饭", "熄灯")) {
                assertTrue("${timetable.name} 缺少节点「$required」", names.contains(required))
            }
        }
    }
}
