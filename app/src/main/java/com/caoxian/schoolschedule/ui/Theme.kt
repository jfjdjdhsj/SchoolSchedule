package com.caoxian.schoolschedule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.caoxian.schoolschedule.data.SettingsStore
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 应用当前生效的深色模式（跟随应用内主题设置，而非系统），供状态栏图标 / 时钟等使用 */
val LocalAppDarkTheme = staticCompositionLocalOf { false }

/** 主题色预设（name, hex）；null 表示 MIUIX 默认配色（HyperOS 蓝） */
val ThemeColorPresets: List<Pair<String, String?>> = listOf(
    "默认" to null,
    "海蓝" to "#2C77F0",
    "靛蓝" to "#5C6BC0",
    "青色" to "#00ACC1",
    "翠绿" to "#2E9E5B",
    "琥珀" to "#F59E0B",
    "橘橙" to "#FF7043",
    "绯红" to "#E5484D",
    "葡萄紫" to "#8E5BD9",
    "樱粉" to "#EC5F9E",
)

/** 根据 themeColor 存储值（"default" 或 "#RRGGBB"）解析种子色；默认配色返回 null */
fun parseThemeColor(themeColor: String): Color? =
    if (themeColor == SettingsStore.THEME_COLOR_DEFAULT) null
    else runCatching {
        Color(android.graphics.Color.parseColor(themeColor))
    }.getOrNull()

/**
 * 应用主题：MIUIX 主题 + 应用内可选的深浅模式与主题色。
 *
 * - [themeMode]：System（跟随系统）/ Light（浅色）/ Dark（深色）；
 * - [themeColor]：default（MIUIX 默认配色）或 "#RRGGBB"——
 *   非默认时使用 Monet 动态取色（以该颜色为种子生成全套配色，
 *   深浅模式跟随应用内主题模式设置）。
 *
 * 适配说明：
 *  - [LocalAppDarkTheme] 提供应用内生效的深浅模式（区别于系统），
 *    时钟文字 / 状态栏图标颜色等都应读取它；
 *  - 切换主题通过重建 [ThemeController] 完成（MIUIX 设计如此）。
 */
@Composable
fun SchoolScheduleTheme(
    themeMode: String,
    themeColor: String,
    content: @Composable () -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val keyColor = parseThemeColor(themeColor)
    val monet = keyColor != null

    val mode = when (themeMode) {
        SettingsStore.THEME_MODE_LIGHT -> if (monet) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        SettingsStore.THEME_MODE_DARK -> if (monet) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        else -> if (monet) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
    }
    // 应用内生效的深色模式（Monet 各模式与 System 一样在 currentColors 内部跟随）
    val dark = when (themeMode) {
        SettingsStore.THEME_MODE_LIGHT -> false
        SettingsStore.THEME_MODE_DARK -> true
        else -> systemDark
    }

    // 切换主题 = 重建 controller（MIUIX 的 ThemeController 属性只读）
    val controller = remember(mode, keyColor) {
        ThemeController(colorSchemeMode = mode, keyColor = keyColor)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppDarkTheme provides dark) {
        MiuixTheme(controller = controller, content = content)
    }
}
