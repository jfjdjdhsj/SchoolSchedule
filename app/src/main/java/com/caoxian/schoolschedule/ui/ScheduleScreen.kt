package com.caoxian.schoolschedule.ui

import android.view.HapticFeedbackConstants
import android.content.Context
import android.os.Build
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caoxian.schoolschedule.MainActivity
import com.caoxian.schoolschedule.ui.component.FloatingBottomBar
import com.caoxian.schoolschedule.ui.component.FloatingBottomBarItem
import com.caoxian.schoolschedule.R
import com.caoxian.schoolschedule.data.BreakPeriod
import com.caoxian.schoolschedule.data.label
import com.caoxian.schoolschedule.data.Round
import com.caoxian.schoolschedule.data.ScheduleEntry
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.data.allTimetables
import com.caoxian.schoolschedule.data.computeBreaks
import com.caoxian.schoolschedule.util.currentClockText
import com.caoxian.schoolschedule.util.currentMinutes
import com.caoxian.schoolschedule.util.effectiveRound
import com.caoxian.schoolschedule.util.fmtTime
import com.caoxian.schoolschedule.util.formatDuration
import com.caoxian.schoolschedule.util.ScheduleI18n
import com.caoxian.schoolschedule.util.visibleRounds
import com.caoxian.schoolschedule.util.UpdateController
import com.caoxian.schoolschedule.util.UpdateState
import com.caoxian.schoolschedule.widget.BaseScheduleWidgetProvider
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassBottomTab
import com.styropyr0.prismal.components.PrismalGlassBottomTabs
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.rememberPrismalAdaptiveLuminance
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.specular.PrismalSpecular
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底栏区域高度（含上下留白），内容区底部避让用 */
private val BottomBarReserve = 104.dp

/** 内容区水平留白（各卡片列表项统一使用） */
private val ContentInset = 12.dp

/**
 * 应用主界面（首页 + 设置，底部导航栏）。
 *
 * 结构：
 *  1. [Scaffold] 提供弹窗渲染宿主（MIUIX OverlayDialog 依赖其 MiuixPopupHost）；
 *  2. 内容层标记为 Prismal 玻璃采样源（`prismalGlassLayer`）；
 *  3. 首页标题区（标题 / 时钟 / 时间表与轮次标签）作为列表首项，随上下滑动一起消失与出现；
 *  4. 底部导航栏：液态玻璃胶囊（Prismal）或经典纯色底栏，开关在设置页；
 *  5. 首页右下角悬浮「定位」按钮。
 *
 * 首次启动展示「隐私政策与匿名统计」同意弹窗；同意后才允许上报设备使用信息
 * （[onConsentGiven] 回调触发一次启动上报）。
 */
@Composable
fun ScheduleScreen(
    settings: SettingsStore,
    onConsentGiven: () -> Unit = {},
    themeMode: String = com.caoxian.schoolschedule.data.SettingsStore.THEME_MODE_SYSTEM,
    onThemeModeChange: (String) -> Unit = {},
    themeColor: String = com.caoxian.schoolschedule.data.SettingsStore.THEME_COLOR_DEFAULT,
    onThemeColorChange: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 首页 / 1 设置
    var timetableId by remember { mutableStateOf(settings.timetableId) }
    var round by remember { mutableStateOf(settings.round) }
    var showAllRound by remember { mutableStateOf(settings.showAllRound) }
    // 隐私政策弹窗：仅在首次启动、尚未做任何选择时展示
    var privacyAnswered by remember { mutableStateOf(settings.privacyAnswered) }
    // 底栏玻璃引擎："prismal" / "miui" / "off"（设置页可切换）
    var glassEngine by remember { mutableStateOf(settings.glassEngine) }
    var hapticsEnabled by remember { mutableStateOf(settings.hapticFeedbackEnabled) }
    val colorScheme = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val context = LocalContext.current

    val timetable = remember(timetableId) { allTimetables.first { it.id == timetableId } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 应用内更新：进入时自动检测（可关）+ 设置页手动检测共用一个控制器
    val updateController = remember { UpdateController(context) }
    var manualCheckRequested by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (settings.autoCheckUpdate) updateController.check(silent = true)
    }
    // 更新状态侧效：手动检测时「已是最新 / 失败」用 Toast；发现新版本 / 下载完成走弹窗
    LaunchedEffect(updateController.state) {
        when (val s = updateController.state) {
            is UpdateState.Latest -> {
                if (manualCheckRequested) Toast.makeText(context, context.getString(R.string.update_already_latest), Toast.LENGTH_SHORT).show()
                updateController.state = UpdateState.Idle
            }
            is UpdateState.Failed -> {
                if (manualCheckRequested) {
                    Toast.makeText(context, context.getString(R.string.update_check_failed, s.message), Toast.LENGTH_SHORT).show()
                }
                updateController.state = UpdateState.Idle
            }
            else -> {}
        }
        if (updateController.state is UpdateState.Latest || updateController.state is UpdateState.Failed) {
            manualCheckRequested = false
        }
    }
    /** 设置页「检测更新」入口：手动模式，结果用 Toast / 弹窗提示 */
    fun checkForUpdate() {
        manualCheckRequested = true
        updateController.check(silent = false)
    }

    // 玻璃采样源：内容层（首页时间轴 / 设置页）绘制于此层，供底栏玻璃与定位按钮采样
    val backdrop = rememberPrismalGlassLayer()
    // KSU 风格底栏的内容采样源（miuix-blur layerBackdrop）
    val ksuBackdrop = rememberLayerBackdrop()
    // 应用内主题强制深浅时，同步状态栏图标颜色（浅色 / 深色 / 跟随系统）
    LaunchedEffect(themeMode) {
        (context as? MainActivity)?.applySystemBarStyle(
            when (themeMode) {
                com.caoxian.schoolschedule.data.SettingsStore.THEME_MODE_LIGHT -> false
                com.caoxian.schoolschedule.data.SettingsStore.THEME_MODE_DARK -> true
                else -> null
            },
        )
    }

    // 每 30 秒刷新一次当前时刻，驱动「进行中」高亮与倒计时
    val nowMinute by produceState(initialValue = currentMinutes()) {
        while (true) {
            value = currentMinutes()
            delay(30_000)
        }
    }

    // 「全部」轮次视图默认隐藏，设置页可开启
    val rounds = visibleRounds(showAllRound)
    val activeRound = effectiveRound(round, showAllRound)

    // 当前视图的节点列表（过滤轮次 + 排序）
    val entries = remember(timetable, activeRound) {
        timetable.entries
            .filter { activeRound == Round.ALL || it.round == activeRound || it.round == Round.ALL }
            .sortedWith(compareBy({ it.startMinute }, { it.round.ordinal }))
    }
    // 由时间表自动推导的休息段（课间休息 / 长间休）
    val breaks = remember(entries) { computeBreaks(entries) }
    val status = remember(entries, breaks, nowMinute) { computeStatus(entries, breaks, nowMinute) }

    // 轮次 / 时间表 / 「全部」视图变化时，立即同步桌面小组件
    LaunchedEffect(activeRound, timetableId, showAllRound) {
        BaseScheduleWidgetProvider.requestRefresh(context)
    }

    // 与时间轴 LazyColumn item 顺序一致的 key 列表，供「定位」滚动使用
    val flatKeys = remember(entries, breaks) { timelineKeys(entries, breaks) }

    /** 点击右下角定位按钮：滚动到当前进行中节点 / 休息段；无进行中则定位下一项；今日已结束则提示 */
    fun locate() {
        val currentEntry = status.currentEntries.firstOrNull()
        val currentBreak = status.currentBreaks.firstOrNull()
        val fallbackEntry = status.nextEntry
        val fallbackBreak = status.nextBreak
        val key: String
        val message: String
        when {
            currentEntry != null -> {
                key = entryKey(currentEntry)
                message = context.getString(R.string.locate_current, entryLabel(context, currentEntry))
            }
            currentBreak != null -> {
                key = breakKey(currentBreak)
                message = context.getString(R.string.locate_current_break, currentBreak.kind.label(context))
            }
            fallbackEntry != null &&
                (fallbackBreak == null || fallbackEntry.startMinute <= fallbackBreak.startMinute) -> {
                key = entryKey(fallbackEntry)
                message = context.getString(R.string.locate_next_entry, entryLabel(context, fallbackEntry))
            }
            fallbackBreak != null -> {
                key = breakKey(fallbackBreak)
                message = context.getString(R.string.locate_next_break, fallbackBreak.kind.label(context))
            }
            else -> {
                Toast.makeText(context, context.getString(R.string.locate_day_over), Toast.LENGTH_SHORT).show()
                return
            }
        }
        val index = flatKeys.indexOf(key)
        if (index >= 0) {
            // 滚动到目标项，并把它调整到视口垂直中间（靠近列表两端时自动贴合边界）
            scope.launch { listState.scrollItemToCenter(index) }
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Scaffold 提供 MiuixPopupHost：MIUIX OverlayDialog（隐私弹窗）依赖它渲染
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.surface,
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface),
        ) {
            // 内容层：标记为玻璃采样源（底栏从中取背景）。
            // 页面由 HorizontalPager 承载：首页左右滑动 ⇄ 设置（v1.7），
            // 与底栏点击 / 拖动切换双向同步。
            val pagerState = rememberPagerState(initialPage = 0) { 2 }

            // 滑动切页 → 同步底栏选中态（跨过中线即切换，跟手）
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (selectedTab != page) selectedTab = page
                }
            }
            // 底栏切换 → 页面滑动过去；滚动中不抢手势，滚动结束再对齐
            LaunchedEffect(selectedTab) {
                snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                    if (!scrolling && pagerState.currentPage != selectedTab) {
                        pagerState.animateScrollToPage(selectedTab)
                    }
                }
            }

            val systemDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // KSU 采样源必须挂在 Prismal 层【外】：
                    // 记录最终合成画面（Prismal 离屏层内部可能带缩放/偏移，
                    // 挂在内层会导致玻璃回放错位——文字重影 / 采样到错误区域）
                    .then(
                        if (glassEngine == "ksu") {
                            Modifier.layerBackdrop(ksuBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .prismalGlassLayer(backdrop)
                    // KSU 玻璃的记录层补不透明背景：AGSL 折射 / 模糊对「半透明文字 +
                    // 透明背景」像素运算时 alpha 会失控聚集成实心色块（分组标题等
                    // 半透明灰文字尤其明显）；补上底色后所有采样像素均为不透明
                    .then(
                        if (glassEngine == "ksu") {
                            // 与窗口背景完全一致（深色 #121212 / 浅色 白），保证观感不变
                            Modifier.background(
                                when (themeMode) {
                                    SettingsStore.THEME_MODE_LIGHT -> Color.White
                                    SettingsStore.THEME_MODE_DARK -> Color(0xFF121212)
                                    else -> if (systemDark) Color(0xFF121212) else Color.White
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    if (page == 0) {
                        ScheduleTimeline(
                            entries = entries,
                            breaks = breaks,
                            round = activeRound,
                            status = status,
                            listState = listState,
                            modifier = Modifier.fillMaxSize(),
                            header = {
                                // 首页标题区：作为列表首项，随上下滑动一起位移（不再固定悬浮）
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding(),
                                ) {
                                    // 首页标题区：标题 + 右侧当前时间 + 副标题
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 26.dp)
                                            .padding(top = 10.dp, bottom = 12.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(R.string.app_name),
                                                color = colorScheme.onSurface,
                                                fontSize = 32.sp,
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            // 当前时间（时:分:秒，每秒刷新）
                                            ClockText()
                                        }
                                        Text(
                                            text = ScheduleI18n.timetable(context, timetable.schoolName) + " · " +
                            ScheduleI18n.effective(context, timetable.effectiveText),
                                            color = colorScheme.onSurfaceVariantSummary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    TabRow(
                                        tabs = allTimetables.map { ScheduleI18n.timetable(context, it.shortName) },
                                        selectedTabIndex = allTimetables.indexOfFirst { it.id == timetable.id }
                                            .coerceAtLeast(0),
                                        onTabSelected = { index ->
                                            timetableId = allTimetables[index].id
                                            settings.timetableId = timetableId
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    TabRowWithContour(
                                        tabs = rounds.map { ScheduleI18n.roundName(context, it.displayName) },
                                        selectedTabIndex = rounds.indexOf(activeRound).coerceAtLeast(0),
                                        onTabSelected = { index ->
                                            round = rounds[index]
                                            settings.round = round
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            },
                        )
                    } else {
                        SettingsScreen(
                            settings = settings,
                            glassEngine = glassEngine,
                            onGlassEngineChange = {
                                glassEngine = it
                                settings.glassEngine = it
                            },
                            hapticsEnabled = hapticsEnabled,
                            onHapticsChange = {
                                hapticsEnabled = it
                                settings.hapticFeedbackEnabled = it
                            },
                            showAllRound = showAllRound,
                            onShowAllRoundToggle = {
                                showAllRound = it
                                settings.showAllRound = it
                            },
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            themeColor = themeColor,
                            onThemeColorChange = onThemeColorChange,
                            update = updateController,
                            onCheckUpdate = { checkForUpdate() },
                            bottomPadding = BottomBarReserve,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // 首页右下角悬浮「定位」按钮：液态玻璃圆形，按压时玻璃增强 + 轻微放大
            if (selectedTab == 0) {
                val press = remember { Animatable(0f) }
                val pressScope = rememberCoroutineScope()
                val pressSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 700f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = BottomBarReserve + 8.dp)
                        .size(60.dp)
                        .graphicsLayer {
                            val scale = 1f + 0.10f * press.value
                            scaleX = scale
                            scaleY = scale
                        }
                        .drawPrismalGlass(
                            backdrop = backdrop,
                            shape = { PrismalCapsule() },
                            effects = {
                                applyPrismalGlassEffects(
                                    density = density,
                                    adaptiveLuminance = false,
                                    luminance = 0.5f,
                                    blurRadiusPx = with(density) { (10f + 8f * press.value).dp.toPx() },
                                    refractionHeightPx = with(density) { (12f + 18f * press.value).dp.toPx() },
                                    refractionAmountPx = with(density) { (8f + 16f * press.value).dp.toPx() },
                                    chromaticAberration = 0.08f + 0.14f * press.value,
                                )
                            },
                            specular = {
                                PrismalSpecular(
                                    width = 0.6f.dp,
                                    alpha = (0.6f + 0.4f * press.value).coerceAtMost(1f),
                                )
                            },
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { locate() },
                                onPress = {
                                    pressScope.launch { press.animateTo(1f, pressSpring) }
                                    tryAwaitRelease()
                                    pressScope.launch { press.animateTo(0f, pressSpring) }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = LocateIcon,
                        contentDescription = stringResource(R.string.locate_button_desc),
                        // 纯液态玻璃底、深色箭头（跟随主题前景色，按压玻璃增强）
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // 底部导航栏：Prismal 液态玻璃或经典纯色底栏
            BottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                glassEngine = glassEngine,
                themeMode = themeMode,
                hapticsEnabled = hapticsEnabled,
                backdrop = backdrop,
                ksuBackdrop = ksuBackdrop,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // 首次启动：隐私政策与匿名统计同意弹窗（不同意则仅关闭弹窗，不影响其他功能）
            if (!privacyAnswered) {
                PrivacyConsentDialog(
                    onAgree = {
                        privacyAnswered = true
                        settings.privacyAnswered = true
                        settings.consentGiven = true
                        onConsentGiven()
                    },
                    onDisagree = {
                        privacyAnswered = true
                        settings.privacyAnswered = true
                        settings.consentGiven = false
                    },
                )
            }

            // 应用内更新弹窗（发现新版本 / 下载中 / 下载完成）
            when (val s = updateController.state) {
                is UpdateState.Available -> UpdateAvailableDialog(
                    info = s.info,
                    onUpdate = { updateController.download(s.info) },
                    onDismiss = { updateController.state = UpdateState.Idle },
                )
                is UpdateState.Downloading -> UpdateDownloadDialog(progress = s.progress)
                is UpdateState.ReadyToInstall -> UpdateInstallDialog(
                    onInstall = { updateController.startInstall(s.file) },
                    onDismiss = { updateController.state = UpdateState.Idle },
                )
                else -> {}
            }
        }
    }
}

/**
 * 底部导航栏：首页 / 设置。按玻璃引擎分派：
 *  - prismal：PrismalAGSL 液态玻璃胶囊（AGSL 折射，31–32 模糊，25–30 磨砂）；
 *  - off：经典半透明纯色底栏。
 */
@Composable
private fun BottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    glassEngine: String,
    themeMode: String,
    hapticsEnabled: Boolean,
    backdrop: PrismalGlassLayer,
    ksuBackdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    // miuix-blur 的 AGSL / RenderEffect 需要 API 31+（完整效果 33+）；低版本回退 Prismal 磨砂降级
    val ksuAvailable = glassEngine == "ksu" && Build.VERSION.SDK_INT >= 31
    if (ksuAvailable) {
        KsuGlassBottomBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            backdrop = ksuBackdrop,
            themeMode = themeMode,
            hapticsEnabled = hapticsEnabled,
            modifier = modifier,
        )
    } else if (glassEngine != "off") {
        // Prismal 液态玻璃；KSU 在低版本不可用时也回退到 Prismal 磨砂降级
        GlassBottomBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            hapticsEnabled = hapticsEnabled,
            modifier = modifier,
        )
    } else {
        ClassicBottomBarContent(selectedTab, onTabSelected, modifier)
    }
}

/**
 * KSU 风格液态玻璃底栏：完整移植自 KernelSU Manager 的 FloatingBottomBar，
 * 效果参数与 KSU 完全一致（vibrancy 饱和度 1.5 → blur 4dp×4dp → lens 折射 24dp/24dp →
 * 40% 半透明底色；选中指示器二次折射 + 色散 0.5 + 内阴影，随按压进度增强）。
 */
@Composable
private fun KsuGlassBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    themeMode: String,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val systemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val isDark = when (themeMode) {
        SettingsStore.THEME_MODE_LIGHT -> false
        SettingsStore.THEME_MODE_DARK -> true
        else -> systemDark
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // 与 Prismal 底栏一致的外边距与高度，保证两种效果尺寸相同、可直接对比
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        FloatingBottomBar(
            modifier = Modifier.fillMaxWidth(),
            selectedIndex = selectedTab,
            onSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = 2,
            isDark = isDark,
            expandWidth = true,
            hapticsEnabled = hapticsEnabled,
        ) { activateTab ->
            FloatingBottomBarItem(selected = selectedTab == 0, onClick = { activateTab(0) }) {
                KsuTabContent(selected = selectedTab == 0, icon = HomeIcon, label = stringResource(R.string.nav_home))
            }
            FloatingBottomBarItem(selected = selectedTab == 1, onClick = { activateTab(1) }) {
                KsuTabContent(selected = selectedTab == 1, icon = SettingsIcon, label = stringResource(R.string.nav_settings))
            }
        }
    }
}

/** KSU 底栏标签内容：尺寸与 Prismal 的 GlassTabContent 一致（图标 22dp / 文字 10sp） */
@Composable
private fun KsuTabContent(selected: Boolean, icon: ImageVector, label: String) {
    val colorScheme = MiuixTheme.colorScheme
    // 选中态由指示器胶囊承载主色折射内容；未选中用次级色
    val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** 经典底栏（v1.8.1 沉浸式）：半透明背景，内容在底栏与手势指示区后隐约滚动 */
@Composable
private fun ClassicBottomBarContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface.copy(alpha = 0.92f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // 顶部分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colorScheme.onSurface.copy(alpha = 0.08f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                ClassicBottomTab(
                    selected = selectedTab == 0,
                    icon = HomeIcon,
                    label = stringResource(R.string.nav_home),
                    onClick = { onTabSelected(0) },
                )
                ClassicBottomTab(
                    selected = selectedTab == 1,
                    icon = SettingsIcon,
                    label = stringResource(R.string.nav_settings),
                    onClick = { onTabSelected(1) },
                )
            }
        }
    }
}

/**
 * 液态玻璃底栏：Prismal 的 iOS 风格玻璃标签栏（胶囊玻璃 + 可左右拖动的选中胶囊）。
 *
 * 交互（v1.6 优化）：
 *  - 按下即有轻微玻璃增压反馈，抬起回落；
 *  - 短按抬起 → 点击切换（带触觉确认）；
 *  - 按住后水平滑动超过触摸斜率 → 直接进入拖动模式，无需长按；手指移到哪个
 *    菜单就实时切到哪个（仅在跨越菜单边界时触发切换 + 触觉 tick）；
 *  - 垂直方向的手势不接管，不影响其他交互；
 *  - 组件最上层手势层会吃掉 tab 点击的问题依旧通过「只观察、不消费事件」的
 *    点击热区解决。
 *
 * 玻璃效果（v1.6 优化）：
 *  - 自适应亮度：随背后内容明暗自动调节模糊与对比；
 *  - 选中胶囊内图标颜色统一为 MIUIX 主色（不再使用 iOS 默认蓝）；
 *  - 增强（按下 / 拖动）玻璃层：更强折射 + RGB 色散（API 33+）+ 边缘高光 +
 *    顶部高光 sheen，随增压进度连续淡入淡出。
 */
@Composable
private fun GlassBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: PrismalGlassLayer,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colorScheme = MiuixTheme.colorScheme
    val view = LocalView.current
    val tabsCount = 2

    // 玻璃增压进度：按下 → 0.35（轻微反馈）；拖动 → 1（全量增强）；松手 → 0
    val glassBoost = remember { Animatable(0f) }
    val boostSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 600f)
    val gestureScope = rememberCoroutineScope()

    // 自适应亮度：玻璃随背后滚动的明暗内容自动调节模糊与对比
    val luminanceState = rememberPrismalAdaptiveLuminance(
        enabled = true,
        source = backdrop,
        isLightTheme = false,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
    ) {
        // 稳定的索引 Provider：lambda 实例不变，组件内部索引不会被重组重置，
        // 切换页面时选中胶囊才能以弹簧动画滑向目标菜单（而不是瞬移 / 不动）
        val tabIndexState = rememberUpdatedState(selectedTab)
        val indexProvider = remember { { tabIndexState.value } }
        PrismalGlassBottomTabs(
            selectedTabIndex = indexProvider,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabsCount,
            adaptiveLuminance = true,
            luminance = { luminanceState.luminance },
            dropletContentTint = colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PrismalGlassBottomTab(onClick = { onTabSelected(0) }) {
                GlassTabContent(selected = selectedTab == 0, icon = HomeIcon, label = stringResource(R.string.nav_home))
            }
            PrismalGlassBottomTab(onClick = { onTabSelected(1) }) {
                GlassTabContent(selected = selectedTab == 1, icon = SettingsIcon, label = stringResource(R.string.nav_settings))
            }
        }

        // 增强（增压）玻璃层：随增压进度淡入淡出，更强折射 + 色散 + 边缘高光
        if (glassBoost.value > 0.01f) {
            val boost = glassBoost.value
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = boost }
                    .drawPrismalGlass(
                        backdrop = backdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = true,
                                luminance = luminanceState.luminance,
                                blurRadiusPx = with(density) { (6f + 5f * boost).dp.toPx() },
                                refractionHeightPx = with(density) { (18f + 16f * boost).dp.toPx() },
                                refractionAmountPx = with(density) { (14f + 18f * boost).dp.toPx() },
                                chromaticAberration = 0.12f + 0.13f * boost,
                            )
                        },
                        specular = {
                            PrismalSpecular(
                                width = 0.8f.dp,
                                alpha = (0.55f + 0.45f * boost).coerceAtMost(1f),
                            )
                        },
                        onDrawSurface = {
                            // 顶部高光 sheen：模拟光源照射，随增压进度显现
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = 0.12f * boost),
                                    0.4f to Color.Transparent,
                                ),
                            )
                        },
                    ),
            )
        }

        // 手势层（最上层）：
        //  - 按下 → 轻微玻璃增压；
        //  - 短按抬起 → 点击切换；
        //  - 水平滑动超过触摸斜率（且水平位移大于垂直）→ 进入拖动，
        //    手指所在菜单实时切换；长按不动再拖动同样成立（位移即触发）。
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        val tabWidth = size.width / tabsCount.toFloat()
                        gestureScope.launch { glassBoost.animateTo(0.35f, boostSpring) }

                        var dragging = false
                        var totalDx = 0f
                        var totalDy = 0f
                        var lastIndex = -1
                        var lastX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            lastX = change.position.x
                            val delta = change.position - change.previousPosition

                            if (!dragging) {
                                totalDx += delta.x
                                totalDy += delta.y
                                if (abs(totalDx) > slop && abs(totalDx) > abs(totalDy)) {
                                    // 水平拖动成立：全量增压 + 触觉提示
                                    dragging = true
                                    gestureScope.launch { glassBoost.animateTo(1f, boostSpring) }
                                    if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                } else if (abs(totalDy) > slop) {
                                    // 垂直手势：不接管
                                    break
                                }
                            }

                            if (dragging) {
                                val index =
                                    (change.position.x / tabWidth).toInt().coerceIn(0, tabsCount - 1)
                                if (index != lastIndex) {
                                    lastIndex = index
                                    onTabSelected(index)
                                    if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                change.consume()
                            }
                        }

                        if (!dragging) {
                            // 短按（含长按不动后抬起）：按落点所在菜单切换
                            val index = (lastX / tabWidth).toInt().coerceIn(0, tabsCount - 1)
                            onTabSelected(index)
                            if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        } else {
                            // 拖动结束：确认触觉
                            if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        gestureScope.launch { glassBoost.animateTo(0f, boostSpring) }
                    }
                },
        )
    }
}

/** 玻璃底栏标签内容：图标 + 文字（选中态主色） */
@Composable
private fun GlassTabContent(selected: Boolean, icon: ImageVector, label: String) {
    val colorScheme = MiuixTheme.colorScheme
    val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(22.dp),
    )
    Text(
        text = label,
        color = tint,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
    )
}

/** 经典底栏单个标签（纯色背景下的图标 + 文字） */
@Composable
private fun RowScope.ClassicBottomTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/**
 * 标题栏当前时间：时:分:秒，每秒刷新。
 * 浅色模式黑色字体、深色模式白色字体；字号 18sp（小于 32sp 的大标题）。
 */
@Composable
private fun ClockText(modifier: Modifier = Modifier) {
    val text by produceState(initialValue = currentClockText()) {
        while (true) {
            value = currentClockText()
            delay(1_000)
        }
    }
    Text(
        text = text,
        modifier = modifier,
        // 跟随应用内主题的深浅模式（区别于系统主题）
        color = if (LocalAppDarkTheme.current) Color.White else Color.Black,
        fontSize = 18.sp,
    )
}

/**
 * 将指定 item 直接定位到视口垂直中间（无过渡动画）。
 * 先无动画跳到该项（顶部对齐），再按「视口高度 - 行高」的一半补偿偏移；
 * 靠近列表顶部 / 底部时滚动会自动夹紧，不会越界（自适应）。
 */
private suspend fun LazyListState.scrollItemToCenter(index: Int) {
    scrollToItem(index)
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportHeight = layoutInfo.viewportSize.height
    val targetOffset = (viewportHeight - info.size) / 2
    val delta = info.offset - targetOffset
    if (delta != 0) scrollBy(delta.toFloat())
}

/** 时间轴 item 的唯一 key（与 timelineKeys 构建逻辑保持一致） */
private fun entryKey(entry: ScheduleEntry): String =
    "entry_${entry.startMinute}_${entry.round.ordinal}_${entry.activity}"

/** 休息段 item 的唯一 key */
private fun breakKey(breakPeriod: BreakPeriod): String =
    "break_${breakPeriod.startMinute}_${breakPeriod.endMinute}"

/**
 * 每个时段内：节点 key -> 其后方展示的休息段。
 * 同一空隙只挂一次，挂在「结束时刻等于该空隙开始」的最后一个节点后
 * （避免两轮活动同时结束时重复显示）。
 */
private fun breaksAfterMap(
    list: List<ScheduleEntry>,
    breaks: List<BreakPeriod>,
): Map<String, BreakPeriod> {
    val map = mutableMapOf<String, BreakPeriod>()
    list.forEachIndexed { index, entry ->
        val br = breaks.firstOrNull {
            it.startMinute == entry.endMinute && it.section == entry.section
        }
        if (br != null && list.drop(index + 1).none { it.endMinute == br.startMinute }) {
            map[entryKey(entry)] = br
        }
    }
    return map
}

/** 生成时间轴的完整 item key 序列（与 LazyColumn 顺序一致），供定位滚动使用 */
private fun timelineKeys(entries: List<ScheduleEntry>, breaks: List<BreakPeriod>): List<String> =
    buildList {
        add("header")
        add("status")
        entries.groupBy { it.section }.forEach { (section, list) ->
            add("title_$section")
            val breakAfter = breaksAfterMap(list, breaks)
            list.forEach { entry ->
                add(entryKey(entry))
                breakAfter[entryKey(entry)]?.let { add(breakKey(it)) }
            }
        }
        add("footer")
    }

/**
 * 全天作息时间轴：标题区（随滚动位移）+ 状态卡片 + 按时段分组的节点列表。
 *
 * 每个节点 / 休息段为独立 item（保证可精确滚动定位），背景色与条件圆角
 * 拼接出与整张 Card 一致的视觉；当前进行中的节点与休息段高亮。
 *
 * @param header 列表首项（首页标题区），随列表滚动一起消失 / 出现
 */
@Composable
fun ScheduleTimeline(
    entries: List<ScheduleEntry>,
    breaks: List<BreakPeriod>,
    round: Round,
    status: ScheduleStatus,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
        val sections = remember(entries) { entries.groupBy { it.section } }
    // 每个时段内：节点 key -> 其后方展示的休息段（同一空隙只挂一次，挂在最后一个结束该空隙的节点后）
    val breakAfterBySection = remember(entries, breaks) {
        sections.mapValues { (_, list) -> breaksAfterMap(list, breaks) }
    }
    val colorScheme = MiuixTheme.colorScheme

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = BottomBarReserve),
        verticalArrangement = Arrangement.Top,
    ) {
        if (header != null) {
            item(key = "header") { header() }
        }
        item(key = "status") {
            NowStatusCard(
                status = status,
                firstEntry = entries.firstOrNull(),
                modifier = Modifier.padding(horizontal = ContentInset),
            )
        }
        sections.forEach { (section, list) ->
            item(key = "title_$section") {
                SmallTitle(
                    text = ScheduleI18n.section(context, section),
                    modifier = Modifier
                        .padding(horizontal = ContentInset)
                        .padding(top = 8.dp),
                )
            }
            val breakAfter = breakAfterBySection[section].orEmpty()
            list.forEachIndexed { index, entry ->
                val br = breakAfter[entryKey(entry)]
                val isLast = index == list.lastIndex
                item(key = entryKey(entry)) {
                    // 条件圆角：首行上圆角、末行下圆角、中间直角，视觉上仍为一张卡片
                    val shape = when {
                        index == 0 && isLast && br == null -> RoundedCornerShape(16.dp)
                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        isLast && br == null -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RectangleShape
                    }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = ContentInset)
                            .fillMaxWidth()
                            .clip(shape)
                            .background(colorScheme.surfaceContainer),
                    ) {
                        EntryRow(
                            entry = entry,
                            isCurrent = status.currentEntries.contains(entry),
                            showRoundBadge = round == Round.ALL && entry.round != Round.ALL,
                        )
                        // 后方紧跟休息段时分隔线归休息段末尾，否则按是否末行判断
                        if (br == null && !isLast) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
                if (br != null) {
                    item(key = breakKey(br)) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = ContentInset)
                                .fillMaxWidth()
                                .clip(
                                    if (isLast) {
                                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    } else {
                                        RectangleShape
                                    },
                                )
                                .background(colorScheme.surfaceContainer),
                        ) {
                            BreakRow(
                                breakPeriod = br,
                                isCurrent = status.currentBreaks.contains(br),
                            )
                            if (!isLast) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "footer") {
            Text(
                text = stringResource(R.string.schedule_footer_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentInset + 8.dp, vertical = 8.dp),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * 休息段行：课间休息 / 长间休的时间段与时长，当前处于该休息段时高亮。
 */
@Composable
fun BreakRow(
    breakPeriod: BreakPeriod,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isCurrent) colorScheme.primary else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = breakPeriod.kind.label(context),
            color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${breakPeriod.timeText} · ${ScheduleI18n.duration(context, breakPeriod.durationMinutes)}",
            color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * 实时状态卡片：展示当前时刻、正在进行的项目、剩余时间与下一项。
 */
@Composable
fun NowStatusCard(
    status: ScheduleStatus,
    firstEntry: ScheduleEntry?,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.status_now, fmtTime(status.nowMinute)),
                color = colorScheme.onTertiaryContainer,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            when {
                status.currentEntries.isNotEmpty() -> {
                    val current = status.currentEntries
                    Text(
                        text = current.joinToString(" · ") { ScheduleI18n.entryLabel(context, it) },
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = current.joinToString(" · ") { it.timeText },
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    val latestEnd = current.maxOf { it.endForStatus }
                    val remain = latestEnd - status.nowMinute
                    if (current.any { it.endMinute != null } && remain > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.status_remaining_class, ScheduleI18n.duration(context, remain)),
                            color = colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    val nextEntry = status.nextEntry
                    val nextBreak = status.nextBreak
                    if (nextEntry != null || nextBreak != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (nextEntry != null) {
                                stringResource(R.string.status_next_item, entryLabel(context, nextEntry), nextEntry.timeText)
                            } else {
                                stringResource(R.string.status_next_item, nextBreak!!.kind.label(context), nextBreak.timeText)
                            },
                            color = colorScheme.onTertiaryContainer,
                            fontSize = 13.sp,
                        )
                    }
                }

                status.currentBreaks.isNotEmpty() -> {
                    val br = status.currentBreaks.first()
                    Text(
                        text = stringResource(R.string.status_in_break, br.kind.label(context)),
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.status_break_left, br.timeText, ScheduleI18n.duration(context, br.endMinute - status.nowMinute)),
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    status.nextEntry?.let { next ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.status_next_distance, entryLabel(context, next), ScheduleI18n.duration(context, next.startMinute - status.nowMinute)),
                            color = colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                status.nextEntry != null -> {
                    val next = status.nextEntry
                    Text(
                        text = stringResource(R.string.status_upcoming_class),
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${entryLabel(context, next)} ${next.timeText}",
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.status_until_start, ScheduleI18n.duration(context, next.startMinute - status.nowMinute)),
                        color = colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                status.nextBreak != null -> {
                    val br = status.nextBreak
                    Text(
                        text = stringResource(R.string.status_upcoming_break),
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${br.kind.label} ${br.timeText}",
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.status_until_break, ScheduleI18n.duration(context, br.startMinute - status.nowMinute)),
                        color = colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.status_day_over),
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = firstEntry?.let { stringResource(R.string.status_tomorrow_early, fmtTime(it.startMinute), it.activity) }
                            ?: stringResource(R.string.status_rest_early),
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

/**
 * 单个作息节点行：活动名称（含轮次徽章、备注）+ 时间段，当前项目高亮。
 */
@Composable
fun EntryRow(
    entry: ScheduleEntry,
    isCurrent: Boolean,
    showRoundBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 进行中标记圆点（非进行中时占位以保持文字对齐）
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isCurrent) colorScheme.primary else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ScheduleI18n.activity(context, entry.activity),
                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                )
                if (showRoundBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ScheduleI18n.roundShort(context, entry.round.shortName),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.secondaryContainer)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            // 下课时间 + 备注，如「9:05 下课 · 不能提前下课」
            val subText = buildList {
                if (entry.endMinute != null) add(stringResource(R.string.timeline_class_over, fmtTime(entry.endMinute)))
                if (entry.note != null) add(ScheduleI18n.note(context, entry.note!!))
            }.joinToString(" · ")
            if (subText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.timeText,
            color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            fontSize = 15.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** 节点标签：区分轮次时带上轮次名，如「第一轮 第四节」 */
private fun entryLabel(context: Context, entry: ScheduleEntry): String =
    ScheduleI18n.entryLabel(context, entry)

/**
 * 隐私政策与匿名统计同意弹窗（首次启动展示）。
 *
 * 弹窗不可通过返回键 / 点击外部关闭，必须做出选择：
 *  - 同意并继续：开启匿名统计（上报设备与使用信息）；
 *  - 不同意：关闭弹窗，仅不参与统计，其他功能正常使用。
 */
@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.privacy_intro),
                color = colorScheme.onSurface,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.privacy_item_device),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Text(
                text = stringResource(R.string.privacy_item_usage),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Text(
                text = stringResource(R.string.privacy_item_env),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.privacy_note),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(R.string.privacy_decline),
                    onClick = onDisagree,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAgree,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.privacy_agree), fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.privacy_title),
        onDismissRequest = { }, // 必须做出选择，禁止返回键 / 点击外部关闭
        content = body,
    )
}

/**
 * 发现新版本弹窗：展示版本号与更新日志，选择立即下载或暂不更新。
 */
@Composable
fun UpdateAvailableDialog(
    info: com.caoxian.schoolschedule.util.UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.update_found_msg, info.versionName),
                color = colorScheme.onSurface,
                fontSize = 14.sp,
            )
            if (info.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.update_log_label),
                    color = colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = info.changelog,
                    color = colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(R.string.update_later),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.update_download_now), fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.update_found),
        onDismissRequest = onDismiss,
        content = body,
    )
}

/** 下载进度弹窗（下载中不可关闭） */
@Composable
fun UpdateDownloadDialog(progress: Int) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colorScheme.secondaryContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colorScheme.primary),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.update_downloading, progress),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.update_downloading_title),
        onDismissRequest = { }, // 下载中不可关闭
        content = body,
    )
}

/** 下载完成弹窗：拉起安装（未授权「安装未知应用」时会先跳转系统设置） */
@Composable
fun UpdateInstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.update_done_msg),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(R.string.update_later_install),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.update_install_now), fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.update_download_complete),
        onDismissRequest = onDismiss,
        content = body,
    )
}
