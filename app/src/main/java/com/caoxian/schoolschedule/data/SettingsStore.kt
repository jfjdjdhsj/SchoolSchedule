package com.caoxian.schoolschedule.data

import android.content.Context
import java.util.Calendar

/**
 * 简单的设置持久化（SharedPreferences）：
 * 记录用户选择的时间表（冬季 / 夏季）与轮次（第一轮 / 第二轮）。
 * 「全部」轮次视图默认隐藏，在设置页开启后显示（showAllRound 持久化）。
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("schedule_settings", Context.MODE_PRIVATE)

    /** 时间表 ID；首次使用时按当前月份自动选择 */
    var timetableId: String
        get() = prefs.getString(KEY_TIMETABLE, null)
            ?: autoTimetableId(Calendar.getInstance().get(Calendar.MONTH) + 1)
        set(value) {
            prefs.edit().putString(KEY_TIMETABLE, value).apply()
        }

    /** 轮次；默认第一轮 */
    var round: Round
        get() = runCatching {
            Round.valueOf(prefs.getString(KEY_ROUND, null) ?: Round.FIRST.name)
        }.getOrDefault(Round.FIRST)
        set(value) {
            prefs.edit().putString(KEY_ROUND, value.name).apply()
        }

    /** 是否显示「全部」轮次视图（设置页开启，默认隐藏） */
    var showAllRound: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ALL_ROUND, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_ALL_ROUND, value).apply()
        }

    /**
     * 是否已同意「匿名使用统计」隐私政策（默认 false）。
     * 仅在用户于首次启动弹窗中点击「同意并继续」后置 true，
     * 之后才允许上报设备使用信息（机型、系统、使用时刻等，不含任何个人身份内容）。
     */
    var consentGiven: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONSENT, value).apply()
        }

    /**
     * 首次启动的隐私政策弹窗是否已由用户做出选择（同意 / 不同意均置 true）。
     * 用于保证弹窗只出现一次，避免用户选择「不同意」后每次启动重复询问。
     */
    var privacyAnswered: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ANSWERED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PRIVACY_ANSWERED, value).apply()
        }

    /**
     * 底栏液态玻璃效果开关（默认开启）。
     * 开启时底栏为 iOS 风格液态玻璃（AGSL 折射 / RenderEffect 模糊，
     * 低版本自动降级为磨砂）；关闭后为经典纯色底栏。在「设置」中切换。
     */
    var bottomBarGlassEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOTTOM_BAR_GLASS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BOTTOM_BAR_GLASS, value).apply()
        }

    /**
     * 进入应用时自动检测更新（默认开启）。
     * 开启时启动应用静默检查 GitHub 上的新版本，发现新版本弹窗提示。
     */
    var autoCheckUpdate: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK_UPDATE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATE, value).apply()
        }

    /**
     * 主题模式：System（跟随系统）/ Light（浅色）/ Dark（深色）。
     */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, null) ?: "System"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
        }

    /**
     * 主题色：default（MIUIX 默认配色）或 Monet 种子色十六进制（如 #2C77F0）。
     */
    var themeColor: String
        get() = prefs.getString(KEY_THEME_COLOR, null) ?: "default"
        set(value) {
            prefs.edit().putString(KEY_THEME_COLOR, value).apply()
        }

    /**
     * 底栏液态玻璃（PrismalAGSL 引擎）："prismal"（开启，默认）/"off"（关闭）。
     * 兼容旧设置：未设置过时读取旧「底栏液态玻璃」开关；历史 "miui" 值视为开启。
     */
    var glassEngine: String
        get() {
            prefs.getString(KEY_GLASS_ENGINE, null)?.let { return it }
            return if (prefs.getBoolean(KEY_BOTTOM_BAR_GLASS, true)) GLASS_ENGINE_PRISMAL else GLASS_ENGINE_OFF
        }
        set(value) {
            val v = if (value == GLASS_ENGINE_MIUI) GLASS_ENGINE_PRISMAL else value
            prefs.edit().putString(KEY_GLASS_ENGINE, v).apply()
        }

    companion object {
        const val KEY_TIMETABLE = "timetable_id"
        const val KEY_ROUND = "round"
        const val KEY_SHOW_ALL_ROUND = "show_all_round"
        const val KEY_CONSENT = "stats_consent_given"
        const val KEY_PRIVACY_ANSWERED = "privacy_answered"
        const val KEY_BOTTOM_BAR_GLASS = "bottom_bar_glass_enabled"
        const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_GLASS_ENGINE = "glass_engine"

        const val THEME_MODE_SYSTEM = "System"
        const val THEME_MODE_LIGHT = "Light"
        const val THEME_MODE_DARK = "Dark"
        const val THEME_COLOR_DEFAULT = "default"
        const val GLASS_ENGINE_PRISMAL = "prismal"
        const val GLASS_ENGINE_MIUI = "miui"
        const val GLASS_ENGINE_OFF = "off"
    }
}
