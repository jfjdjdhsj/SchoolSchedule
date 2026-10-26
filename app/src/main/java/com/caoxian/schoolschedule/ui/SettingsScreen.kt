package com.caoxian.schoolschedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.widget.Toast
import android.content.Context
import android.os.LocaleList
import com.caoxian.schoolschedule.R
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.util.FeedbackSender
import com.caoxian.schoolschedule.util.ReleaseLogEntry
import com.caoxian.schoolschedule.util.UpdateController
import com.caoxian.schoolschedule.util.UpdateState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：标题区（随滚动位移）+ 外观（底栏液态玻璃）、显示（全部轮次）、
 * 数据与隐私、更新（自动检测 / 手动检测）、关于信息。
 *
 * @param update 应用内更新控制器（与首页共用，弹窗在首页层级渲染）
 * @param onCheckUpdate 手动触发检测更新（Toast / 弹窗提示结果）
 * @param glassEngine 底栏玻璃引擎："prismal" / "ksu" / "off"
 * @param onGlassEngineChange 引擎切换回调
 * @param showAllRound 是否显示「全部」轮次视图
 * @param onShowAllRoundToggle 开关回调
 * @param bottomPadding 底部避让（底栏高度）
 */
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    glassEngine: String,
    onGlassEngineChange: (String) -> Unit,
    showAllRound: Boolean,
    onShowAllRoundToggle: (Boolean) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit,
    update: UpdateController,
    onCheckUpdate: () -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }
    // 匿名统计状态：可随时在设置中开启 / 关闭（上报器每次上报前都会读取，即时生效）
    var statsEnabled by remember { mutableStateOf(settings.consentGiven) }
    // 进入时自动检测更新开关
    var autoCheckUpdate by remember { mutableStateOf(settings.autoCheckUpdate) }
    // 弹窗
    var showModeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showOssDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGlassDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 6.dp)
            .padding(bottom = bottomPadding),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SmallTitle(
                text = stringResource(R.string.settings_group_appearance),
                insideMargin = PaddingValues(start = 16.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
            )
            // 底栏玻璃效果：Prismal / KSU / 关闭（可切换对比两种液态玻璃）
            Card(modifier = Modifier.fillMaxWidth()) {
                SelectRow(
                    icon = MiuixIcons.Theme,
                    title = stringResource(R.string.settings_glass_title),
                    summary = stringResource(R.string.settings_glass_summary),
                    value = when (glassEngine) {
                        SettingsStore.GLASS_ENGINE_KSU -> stringResource(R.string.glass_style_ksu)
                        SettingsStore.GLASS_ENGINE_OFF -> stringResource(R.string.glass_style_off)
                        else -> stringResource(R.string.glass_style_prismal)
                    },
                    onClick = { showGlassDialog = true },
                )
            }
            // 主题模式
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SelectRow(
                    icon = MiuixIcons.Theme,
                    title = stringResource(R.string.settings_theme_mode_title),
                    summary = stringResource(R.string.settings_theme_mode_summary),
                    value = when (themeMode) {
                        SettingsStore.THEME_MODE_LIGHT -> stringResource(R.string.theme_light)
                        SettingsStore.THEME_MODE_DARK -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = { showModeDialog = true },
                )
            }
            // 主题色（Monet 动态取色）
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SelectRow(
                    icon = MiuixIcons.Album,
                    title = stringResource(R.string.settings_theme_color_title),
                    summary = stringResource(R.string.settings_theme_color_summary),
                    value = ThemeColorPresets.firstOrNull { (name, hex) ->
                        (hex ?: "default") == themeColor || (hex != null && name == themeColor)
                    }?.first ?: if (themeColor == "default") stringResource(R.string.theme_color_default) else themeColor,
                    onClick = { showColorDialog = true },
                )
            }
            // 语言（应用内切换；Android 13+ 走系统「应用语言」）
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SelectRow(
                    icon = MiuixIcons.Community,
                    title = stringResource(R.string.language_title),
                    summary = stringResource(R.string.settings_language_summary),
                    value = when (currentAppLanguage(context)) {
                        SettingsStore.LANG_ZH -> "简体中文"
                        SettingsStore.LANG_EN -> "English"
                        SettingsStore.LANG_ZH_TW -> "繁體中文"
                        SettingsStore.LANG_JA -> "日本語"
                        SettingsStore.LANG_RU -> "Русский"
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = { showLanguageDialog = true },
                )
            }

            SmallTitle(
                text = stringResource(R.string.settings_group_display),
                insideMargin = PaddingValues(start = 16.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.padding(top = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    icon = MiuixIcons.Show,
                    title = stringResource(R.string.settings_show_all_round),
                    summary = stringResource(R.string.settings_show_all_round_summary),
                    checked = showAllRound,
                    onToggle = onShowAllRoundToggle,
                )
            }

            SmallTitle(
                text = stringResource(R.string.settings_group_privacy),
                insideMargin = PaddingValues(start = 16.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.padding(top = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    icon = MiuixIcons.Lock,
                    title = stringResource(R.string.settings_stats_title),
                    summary = if (statsEnabled) {
                        stringResource(R.string.settings_stats_summary_on)
                    } else {
                        stringResource(R.string.settings_stats_summary_off)
                    },
                    checked = statsEnabled,
                    onToggle = {
                        statsEnabled = it
                        settings.consentGiven = it
                    },
                )
            }

            SmallTitle(
                text = stringResource(R.string.settings_group_update),
                insideMargin = PaddingValues(start = 16.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.padding(top = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    icon = MiuixIcons.Update,
                    title = stringResource(R.string.settings_auto_update_title),
                    summary = stringResource(R.string.settings_auto_update_summary),
                    checked = autoCheckUpdate,
                    onToggle = {
                        autoCheckUpdate = it
                        settings.autoCheckUpdate = it
                    },
                )
            }
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                // 手动检测更新：点击立即检查，结果用 Toast / 弹窗提示
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = update.state !is UpdateState.Checking) { onCheckUpdate() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = stringResource(R.string.settings_check_update_title),
                        tint = colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-1).dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_check_update_title),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = stringResource(R.string.settings_check_update_summary),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                            .align(Alignment.CenterVertically),
                    )
                    Text(
                        text = if (update.state is UpdateState.Checking) stringResource(R.string.settings_checking) else "›",
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = if (update.state is UpdateState.Checking) 13.sp else 18.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            // 更新日志：聚合 GitHub Releases 历史版本说明
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Notes,
                        contentDescription = stringResource(R.string.settings_update_log_title),
                        tint = colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-1).dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dialog_update_log),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = stringResource(R.string.settings_update_log_summary),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                            .align(Alignment.CenterVertically),
                    )
                    Text(
                        text = "›",
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            SmallTitle(
                text = stringResource(R.string.settings_group_about),
                insideMargin = PaddingValues(start = 16.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.padding(top = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                // 问题反馈入口：纯文字反馈（类型 + 正文 + 可选联系方式）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFeedbackDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        contentDescription = stringResource(R.string.settings_feedback_title),
                        tint = colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-1).dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_feedback_title),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = stringResource(R.string.settings_feedback_summary),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .width(12.dp)
                            .align(Alignment.CenterVertically),
                    )
                    Text(
                        text = "›",
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                // 开源库入口：点击弹窗展示 APP 使用的开源组件（图标与标题首行对齐）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOssDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Community,
                        contentDescription = stringResource(R.string.settings_oss_title),
                        tint = colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-1).dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_oss_title),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = stringResource(R.string.settings_oss_summary),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "›",
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_name) + " " + (versionName ?: ""),
                        color = colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_source),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }
    }

    // 开源库弹窗：展示 APP 使用的开源组件与许可
    if (showOssDialog) {
        OssLibrariesDialog(onDismiss = { showOssDialog = false })
    }

    // 问题反馈弹窗（纯文字：类型 + 正文 + 可选联系方式）
    if (showFeedbackDialog) {
        FeedbackDialog(onDismiss = { showFeedbackDialog = false })
    }

    // 更新日志弹窗（聚合 GitHub Releases，离线时回退本地缓存）
    if (showLogDialog) {
        UpdateLogDialog(update = update, onDismiss = { showLogDialog = false })
    }

    // 底栏玻璃效果弹窗：两种液态玻璃实现 + 关闭
    if (showGlassDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_glass_title),
            selected = glassEngine,
            options = listOf(
                Option(SettingsStore.GLASS_ENGINE_PRISMAL, stringResource(R.string.glass_style_prismal), null),
                Option(SettingsStore.GLASS_ENGINE_KSU, stringResource(R.string.glass_style_ksu), null),
                Option(SettingsStore.GLASS_ENGINE_OFF, stringResource(R.string.glass_style_off), null),
            ),
            onSelect = {
                onGlassEngineChange(it)
                showGlassDialog = false
            },
            onDismiss = { showGlassDialog = false },
        )
    }

    // 主题模式弹窗
    if (showModeDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_theme_mode_title),
            selected = themeMode,
            options = listOf(
                Option("System", stringResource(R.string.theme_system), null),
                Option("Light", stringResource(R.string.theme_light), null),
                Option("Dark", stringResource(R.string.theme_dark), null),
            ),
            onSelect = {
                onThemeModeChange(it)
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false },
        )
    }

    // 语言弹窗：Android 13+ 写系统「应用语言」（小组件同步）；13 以下写偏好并重建
    if (showLanguageDialog) {
        OptionDialog(
            title = stringResource(R.string.language_title),
            selected = currentAppLanguage(context),
            options = listOf(
                Option(SettingsStore.LANG_SYSTEM, stringResource(R.string.theme_system), null),
                Option(SettingsStore.LANG_ZH, "简体中文", null),
                Option(SettingsStore.LANG_EN, "English", null),
                Option(SettingsStore.LANG_ZH_TW, "繁體中文", null),
                Option(SettingsStore.LANG_JA, "日本語", null),
                Option(SettingsStore.LANG_RU, "Русский", null),
            ),
            onSelect = { value ->
                showLanguageDialog = false
                settings.appLanguage = value
                if (Build.VERSION.SDK_INT >= 33) {
                    val lm = context.getSystemService(LocaleManager::class.java)
                    lm.applicationLocales =
                        if (value == SettingsStore.LANG_SYSTEM) {
                            LocaleList.getEmptyLocaleList()
                        } else {
                            LocaleList.forLanguageTags(value)
                        }
                } else {
                    // Android 13 以下无系统级应用语言 API，只能重建 Activity 应用新语言；
                    // 去掉转场动画以减轻黑屏闪烁观感（13+ 走 LocaleManager，无重建）
                    (context as? Activity)?.apply {
                        window.setWindowAnimations(0)
                        recreate()
                    }
                }
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    // 主题色弹窗（Monet 动态取色预设）
    if (showColorDialog) {
        OptionDialog(
            title = stringResource(R.string.settings_theme_color_title),
            selected = themeColor,
            options = ThemeColorPresets.map { (name, hex) ->
                Option(hex ?: "default", name, hex?.let { parseThemeColor(it) })
            },
            onSelect = {
                onThemeColorChange(it)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false },
        )
    }
}

/** APP 使用的开源组件（名称 / 项目地址或坐标 / 许可） */
private data class OssLibrary(val name: String, val detail: String, val license: String)

private val ossLibraries = listOf(
    OssLibrary(
        "MIUIX",
        "top.yukonga.miuix.kmp:miuix-ui-android · github.com/miuix-kotlin-multiplatform/miuix",
        "Apache License 2.0",
    ),
    OssLibrary(
        "PrismalAGSL",
        "com.github.styropyr0:PrismalAGSL · github.com/styropyr0/PrismalAGSL",
        "MIT License",
    ),
    OssLibrary(
        "Jetpack Compose / AndroidX",
        "developer.android.com/jetpack",
        "Apache License 2.0",
    ),
    OssLibrary(
        "Kotlin",
        "kotlinlang.org",
        "Apache License 2.0",
    ),
)

/**
 * 开源库弹窗：列出 APP 使用的开源组件、项目地址与许可。
 */
@Composable
private fun OssLibrariesDialog(onDismiss: () -> Unit) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            ossLibraries.forEachIndexed { index, lib ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        text = lib.name,
                        color = colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = lib.detail,
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.padding(top = 3.dp))
                    Text(
                        text = lib.license,
                        color = colorScheme.primary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(
                                color = colorScheme.primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (index < ossLibraries.lastIndex) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(modifier = Modifier.padding(top = 14.dp))
            TextButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.padding(top = 4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.settings_oss_title),
        onDismissRequest = onDismiss,
        content = body,
    )
}

/** 选项弹窗条目：[value] 存储值、[label] 显示文本、[dot] 色点（主题色用）、[sub] 二级说明 */
private data class Option(
    val value: String,
    val label: String,
    val dot: Color? = null,
    val sub: String? = null,
)

/** 当前生效的应用语言（LanguageTag）；"system" = 跟随系统 */
private fun currentAppLanguage(context: Context): String {
    if (Build.VERSION.SDK_INT >= 33) {
        val first = context.getSystemService(LocaleManager::class.java)
            .applicationLocales.get(0)
        if (first != null) return first.toLanguageTag()
        return SettingsStore.LANG_SYSTEM
    }
    return SettingsStore(context).appLanguage
}

/**
 * 更新日志弹窗：聚合 GitHub Releases 历史版本说明（最新在前，最多 30 条）。
 * 网络不可达时展示本地缓存（标注缓存时间）。
 */
@Composable
private fun UpdateLogDialog(update: UpdateController, onDismiss: () -> Unit) {
    val colorScheme = MiuixTheme.colorScheme
    var loading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<ReleaseLogEntry>?>(null) }
    var cacheAt by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val (list, cache) = update.fetchReleaseLog()
        entries = list
        cacheAt = cache
        loading = false
    }

    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            when {
                loading -> {
                    Text(
                        text = stringResource(R.string.log_loading),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                entries == null -> {
                    Text(
                        text = stringResource(R.string.log_load_failed),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    entries!!.forEachIndexed { index, e ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .height(0.5.dp)
                                    .background(colorScheme.onSurface.copy(alpha = 0.08f)),
                            )
                        }
                        Text(
                            text = "v${e.version}",
                            color = colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (e.date.isNotBlank()) {
                            Text(
                                text = e.date,
                                color = colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            text = e.changelog.ifBlank { stringResource(R.string.log_no_notes) },
                            color = colorScheme.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                    cacheAt?.let { at ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.log_offline_cache, at),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.dialog_update_log),
        onDismissRequest = onDismiss,
        content = body,
    )
}

/**
 * 问题反馈弹窗（专业版，仅文字）：
 * 类型三选一 + 正文（≤1000 字，实时计数）+ 联系方式（选填）。
 * 提交走 [FeedbackSender]（X-Api-Key 鉴权），成功后 Toast 并关闭。
 */
@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme
    val categories = listOf(
                "bug" to stringResource(R.string.feedback_type_bug),
                "suggestion" to stringResource(R.string.feedback_type_suggestion),
                "other" to stringResource(R.string.feedback_type_other),
            )
    var category by remember { mutableStateOf("bug") }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val canSubmit = !submitting && content.trim().length >= 5

    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            // 类型选择（三选一胶囊）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { (value, label) ->
                    val selected = value == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) colorScheme.primary
                                else colorScheme.secondaryContainer
                            )
                            .clickable { category = value }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else colorScheme.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = content,
                onValueChange = {
                    if (it.length <= FeedbackSender.MAX_CONTENT) content = it
                },
                label = stringResource(R.string.feedback_content_hint),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${content.length}/${FeedbackSender.MAX_CONTENT}",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = contact,
                onValueChange = {
                    if (it.length <= FeedbackSender.MAX_CONTACT) contact = it
                },
                label = stringResource(R.string.feedback_contact_hint),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    submitting = true
                    val trimmed = content.trim()
                    val trimmedContact = contact.trim()
                    FeedbackSender.send(context, category, trimmed, trimmedContact) { ok ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            submitting = false
                            Toast.makeText(
                                context,
                                if (ok) context.getString(R.string.feedback_submitted)
                                else context.getString(R.string.feedback_submit_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (ok) onDismiss()
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (submitting) stringResource(R.string.feedback_submitting) else stringResource(R.string.feedback_submit), fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.dialog_feedback),
        onDismissRequest = onDismiss,
        content = body,
    )
}

/**
 * 单选弹窗：列表选择，当前项带 ✓ 与主色高亮。
 */
@Composable
private fun OptionDialog(
    title: String,
    selected: String,
    options: List<Option>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            options.forEach { option ->
                val isselected = option.value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(option.value)
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    option.dot?.let { c ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(c)
                                .padding(2.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.label,
                            color = if (isselected) colorScheme.primary else colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = if (isselected) FontWeight.Medium else FontWeight.Normal,
                        )
                        if (option.sub != null) {
                            Spacer(modifier = Modifier.padding(top = 2.dp))
                            Text(
                                text = option.sub,
                                color = colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    if (isselected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓",
                            color = colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(top = 10.dp))
            TextButton(
                text = stringResource(R.string.dialog_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.padding(top = 4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        content = body,
    )
}

/**
 * 选择行：图标 + 标题 + 说明 + 当前值 + ›（点击弹出选择弹窗）。
 */
@Composable
private fun SelectRow(
    icon: ImageVector,
    title: String,
    summary: String,
    value: String,
    onClick: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .offset(y = (-1).dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontSize = 15.sp,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
        }
        Spacer(
            modifier = Modifier
                .width(12.dp)
                .align(Alignment.CenterVertically),
        )
        Text(
            text = value,
            color = colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "›",
            color = colorScheme.onSurfaceVariantSummary,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

/**
 * 设置行：图标 + 标题 + 说明 + 右侧开关。
 * 图标与标题首行对齐（不随多行说明垂直居中），右侧开关保持整行居中。
 */
@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    summary: String,
    checked: Boolean,
    onToggle: ((Boolean) -> Unit)?,
) {
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    // 24dp 图标在 15sp 标题首行（行高约 21dp）内垂直居中
                    .offset(y = (-1).dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontSize = 15.sp,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
        }
        Spacer(
            modifier = Modifier
                .width(12.dp)
                .align(Alignment.CenterVertically),
        )
        if (onToggle != null) {
            Checkbox(
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = { onToggle(!checked) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        } else {
            Text(
                text = if (checked) stringResource(R.string.stats_state_on) else stringResource(R.string.stats_state_off),
                color = if (checked) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}
