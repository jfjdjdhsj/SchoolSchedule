package com.caoxian.schoolschedule

import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.util.effectiveRound
import com.caoxian.schoolschedule.util.visibleRounds
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 轮次视图显隐规则测试：「全部」默认隐藏，长按「关于」解锁后显示。
 */
class RoundVisibilityTest {

    @Test
    fun `默认只显示第一轮与第二轮`() {
        assertEquals(listOf(Round.FIRST, Round.SECOND), visibleRounds(showAll = false))
    }

    @Test
    fun `解锁后显示全部三个视图`() {
        assertEquals(listOf(Round.ALL, Round.FIRST, Round.SECOND), visibleRounds(showAll = true))
    }

    @Test
    fun `未解锁时全部回退为第一轮`() {
        assertEquals(Round.FIRST, effectiveRound(Round.ALL, showAll = false))
    }

    @Test
    fun `解锁后保持全部`() {
        assertEquals(Round.ALL, effectiveRound(Round.ALL, showAll = true))
    }

    @Test
    fun `具体轮次不受显隐影响`() {
        assertEquals(Round.FIRST, effectiveRound(Round.FIRST, showAll = false))
        assertEquals(Round.SECOND, effectiveRound(Round.SECOND, showAll = false))
        assertEquals(Round.SECOND, effectiveRound(Round.SECOND, showAll = true))
    }
}
