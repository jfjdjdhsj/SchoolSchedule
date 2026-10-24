package com.caoxian.schoolschedule.data

/**
 * 两套作息时间表数据，来源：
 *  - 冬季：《2026.10.1作息时间表》（曹县高级技工学校，2026年10月1日起执行）
 *  - 夏季：《cc8cf38b...xls》（曹县高级技工学校，2026年5月1日起执行）
 */

private fun t(hour: Int, minute: Int = 0) = hour * 60 + minute

private fun e(
    section: String,
    activity: String,
    startHour: Int,
    startMinute: Int,
    endHour: Int? = null,
    endMinute: Int = 0,
    round: Round = Round.ALL,
    note: String? = null,
) = ScheduleEntry(
    section = section,
    activity = activity,
    startMinute = t(startHour, startMinute),
    endMinute = if (endHour != null) t(endHour, endMinute) else null,
    round = round,
    note = note,
)

/** 冬季作息时间表（2026年10月1日起执行） */
val winterTimetable = Timetable(
    id = "winter",
    name = "冬季作息时间表",
    shortName = "冬季作息",
    schoolName = "曹县高级技工学校",
    effectiveText = "2026年10月1日起执行",
    entries = buildList {
        // 早晨
        add(e("早晨", "起床", 6, 0))
        add(e("早晨", "早操、晨会", 6, 20, 6, 50, Round.FIRST))
        add(e("早晨", "早饭", 6, 55, 7, 15, Round.FIRST))
        add(e("早晨", "晨读", 7, 40, 8, 10, Round.FIRST))
        add(e("早晨", "晨读", 6, 20, 6, 50, Round.SECOND))
        add(e("早晨", "早操、晨会", 6, 55, 7, 20, Round.SECOND))
        add(e("早晨", "早饭", 7, 25, 7, 45, Round.SECOND))
        // 上午
        add(e("上午", "预备", 8, 10))
        add(e("上午", "第一节", 8, 20, 9, 5))
        add(e("上午", "第二节", 9, 15, 10, 0))
        add(e("上午", "第三节", 10, 20, 11, 5))
        add(e("上午", "第四节", 11, 15, 11, 35, Round.FIRST))
        add(e("上午", "第四节", 11, 15, 12, 0, Round.SECOND, note = "不能提前下课"))
        add(e("上午", "午饭", 11, 40, 12, 5, Round.FIRST))
        add(e("上午", "午饭", 12, 10, 12, 35, Round.SECOND, note = "开始进入餐厅"))
        // 素质提升
        add(e("素质提升", "素质提升", 14, 10, 14, 30))
        // 下午
        add(e("下午", "预备", 14, 30))
        add(e("下午", "第五节", 14, 40, 15, 25))
        add(e("下午", "第六节", 15, 35, 16, 20))
        add(e("下午", "第七节", 16, 30, 17, 15))
        add(e("下午", "晚饭", 17, 20, 17, 40, Round.FIRST))
        add(e("下午", "晚饭", 17, 45, 18, 5, Round.SECOND))
        // 晚自习
        add(e("晚自习", "预备", 18, 20))
        add(e("晚自习", "第一节", 18, 30, 19, 10))
        add(e("晚自习", "第二节", 19, 20, 20, 0))
        // 就寝
        add(e("就寝", "宿舍检查", 20, 40))
        add(e("就寝", "熄灯", 21, 10))
    },
)

/** 夏季作息时间表（2026年5月1日起执行） */
val summerTimetable = Timetable(
    id = "summer",
    name = "夏季作息时间表",
    shortName = "夏季作息",
    schoolName = "曹县高级技工学校",
    effectiveText = "2026年5月1日起执行",
    entries = buildList {
        // 早晨
        add(e("早晨", "起床", 6, 0))
        add(e("早晨", "早操、晨会", 6, 20, 6, 50, Round.FIRST))
        add(e("早晨", "早饭", 6, 55, 7, 20, Round.FIRST))
        add(e("早晨", "晨读", 7, 50, 8, 10, Round.FIRST))
        add(e("早晨", "晨读", 6, 20, 6, 55, Round.SECOND))
        add(e("早晨", "早操、晨会", 7, 0, 7, 30, Round.SECOND))
        add(e("早晨", "早饭", 7, 35, 7, 55, Round.SECOND))
        // 上午
        add(e("上午", "预备", 8, 10))
        add(e("上午", "第一节", 8, 20, 9, 5))
        add(e("上午", "第二节", 9, 15, 10, 0))
        add(e("上午", "第三节", 10, 20, 11, 5))
        add(e("上午", "第四节", 11, 15, 11, 35, Round.FIRST))
        add(e("上午", "第四节", 11, 15, 12, 0, Round.SECOND, note = "不能提前下课"))
        add(e("上午", "午饭", 11, 40, 12, 5, Round.FIRST))
        add(e("上午", "午饭", 12, 10, 12, 35, Round.SECOND, note = "开始从集合点出发"))
        // 午休
        add(e("午休", "午休", 13, 0, 14, 20))
        // 素质提升
        add(e("素质提升", "素质提升", 14, 30, 15, 0))
        // 下午
        add(e("下午", "预备", 15, 0))
        add(e("下午", "第五节", 15, 10, 15, 55))
        add(e("下午", "第六节", 16, 5, 16, 50))
        add(e("下午", "第七节", 17, 0, 17, 45))
        add(e("下午", "晚饭", 17, 55, 18, 15, Round.FIRST))
        add(e("下午", "晚饭", 18, 25, 18, 45, Round.SECOND, note = "开始从集合点出发"))
        // 晚自习
        add(e("晚自习", "预备", 18, 50))
        add(e("晚自习", "第一节", 19, 0, 19, 40))
        add(e("晚自习", "第二节", 19, 50, 20, 30))
        // 就寝
        add(e("就寝", "宿舍检查", 21, 0))
        add(e("就寝", "熄灯", 21, 30))
    },
)

/** 全部时间表（顺序即界面切换顺序：冬季、夏季） */
val allTimetables: List<Timetable> = listOf(winterTimetable, summerTimetable)

/**
 * 根据月份自动选择默认时间表：
 * 5—9 月执行夏季表，其余月份执行冬季表（冬季表自 10 月 1 日起执行）。
 */
fun autoTimetableId(month: Int): String =
    if (month in 5..9) summerTimetable.id else winterTimetable.id
