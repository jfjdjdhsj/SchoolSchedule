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
            // 全新安装（无任何历史设置）：默认 KSU 液态玻璃；
            // 老用户升级：按旧「底栏液态玻璃」开关回退为 Prismal / 关闭
            if (prefs.contains(KEY_BOTTOM_BAR_GLASS)) {
                return if (prefs.getBoolean(KEY_BOTTOM_BAR_GLASS, true)) GLASS_ENGINE_PRISMAL else GLASS_ENGINE_OFF
            }
            return GLASS_ENGINE_KSU
        }
        set(value) {
            val v = if (value == GLASS_ENGINE_MIUI) GLASS_ENGINE_PRISMAL else value
            prefs.edit().putString(KEY_GLASS_ENGINE, v).apply()
        }

    /**
     * 常驻状态通知：上课期间自动静默，课间与其他时间提醒。
     */
    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATION, value).apply()
        }

    /**
     * 加速源：开启使用 gh-proxy.com 加速更新检查与下载（默认），关闭使用 GitHub 官方直连。
     */
    var cdnAccelerated: Boolean
        get() = prefs.getBoolean(KEY_CDN, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CDN, value).apply()
        }

    /**
     * 触觉反馈：底栏拖动 / 切换菜单时的震动（可关）。
     */
    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        }

    /**
     * 应用语言（仅 Android 13 以下生效；13+ 由系统「应用语言」设置接管）。
     * "system"（跟随系统，默认）/ "zh-CN" / "en" / "zh-TW"。
     */
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, null) ?: LANG_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()
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
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_HAPTICS = "haptic_feedback_enabled"
        const val KEY_CDN = "accelerated_source"
        const val KEY_NOTIFICATION = "status_notification"

        /** 应用语言取值：跟随系统 / 简体中文 / English / 繁體中文 */
        const val LANG_SYSTEM = "system"
        const val LANG_ZH = "zh-CN"
        const val LANG_EN = "en"
        const val LANG_ZH_TW = "zh-TW"
        const val LANG_JA = "ja"
        const val LANG_RU = "ru"

        const val THEME_MODE_SYSTEM = "System"
        const val THEME_MODE_LIGHT = "Light"
        const val THEME_MODE_DARK = "Dark"
        const val THEME_COLOR_DEFAULT = "default"
        const val GLASS_ENGINE_PRISMAL = "prismal"
        const val GLASS_ENGINE_MIUI = "miui"
        const val GLASS_ENGINE_OFF = "off"
        const val GLASS_ENGINE_KSU = "ksu"
    }
}
