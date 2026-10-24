package com.caoxian.schoolschedule.util

import com.caoxian.schoolschedule.data.Round

/**
 * 轮次视图可见性规则：
 * 默认只显示「第一轮 / 第二轮」；在设置页开启后才显示「全部」。
 */

/** 当前应展示的轮次 Tab 列表 */
fun visibleRounds(showAll: Boolean): List<Round> =
    if (showAll) Round.entries.toList() else listOf(Round.FIRST, Round.SECOND)

/**
 * 实际生效的轮次：未解锁「全部」时，即使用户此前选过「全部」也回退为第一轮，
 * 解锁后恢复显示全部。
 */
fun effectiveRound(round: Round, showAll: Boolean): Round =
    if (showAll || round != Round.ALL) round else Round.FIRST
