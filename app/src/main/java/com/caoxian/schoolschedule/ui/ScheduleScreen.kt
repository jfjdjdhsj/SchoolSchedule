package com.caoxian.schoolschedule.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caoxian.schoolschedule.MainActivity
import com.caoxian.schoolschedule.data.BreakPeriod
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
import com.caoxian.schoolschedule.util.visibleRounds
import com.caoxian.schoolschedule.util.UpdateController
import com.caoxian.schoolschedule.util.UpdateState
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.components.PrismalGlassBottomTab
import com.styropyr0.prismal.components.PrismalGlassBottomTabs
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.rememberPrismalAdaptiveLuminance
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.specular.PrismalSpecular
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
    val colorScheme = MiuixTheme.colorScheme
    val density = LocalDensity.current

    val timetable = remember(timetableId) { allTimetables.first { it.id == timetableId } }
    val context = LocalContext.current
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
                if (manualCheckRequested) Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                updateController.state = UpdateState.Idle
            }
            is UpdateState.Failed -> {
                if (manualCheckRequested) {
                    Toast.makeText(context, "检查更新失败：${s.message}", Toast.LENGTH_SHORT).show()
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
                message = "已定位到当前项目：${entryLabel(currentEntry)}"
            }
            currentBreak != null -> {
                key = breakKey(currentBreak)
                message = "已定位到当前休息：${currentBreak.kind.label}"
            }
            fallbackEntry != null &&
                (fallbackBreak == null || fallbackEntry.startMinute <= fallbackBreak.startMinute) -> {
                key = entryKey(fallbackEntry)
                message = "当前无进行中项目，已定位到下一项：${entryLabel(fallbackEntry)}"
            }
            fallbackBreak != null -> {
                key = breakKey(fallbackBreak)
                message = "当前无进行中项目，已定位到下一段休息：${fallbackBreak.kind.label}"
            }
            else -> {
                Toast.makeText(context, "今日作息已结束", Toast.LENGTH_SHORT).show()
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .prismalGlassLayer(backdrop),
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
                                                text = "校园作息表",
                                                color = colorScheme.onSurface,
                                                fontSize = 32.sp,
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            // 当前时间（时:分:秒，每秒刷新）
                                            ClockText()
                                        }
                                        Text(
                                            text = "${timetable.schoolName} · ${timetable.effectiveText}",
                                            color = colorScheme.onSurfaceVariantSummary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    TabRow(
                                        tabs = allTimetables.map { it.shortName },
                                        selectedTabIndex = allTimetables.indexOfFirst { it.id == timetable.id }
                                            .coerceAtLeast(0),
                                        onTabSelected = { index ->
                                            timetableId = allTimetables[index].id
                                            settings.timetableId = timetableId
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    TabRowWithContour(
                                        tabs = rounds.map { it.displayName },
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
                            glassEnabled = glassEngine != "off",
                            onGlassToggle = {
                                glassEngine = if (it) "prismal" else "off"
                                settings.glassEngine = glassEngine
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
                        contentDescription = "定位到当前时间段",
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
                glassEnabled = glassEngine != "off",
                backdrop = backdrop,
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
    glassEnabled: Boolean,
    backdrop: PrismalGlassLayer,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    if (glassEnabled) {
        GlassBottomBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            modifier = modifier,
        )
    } else {
        ClassicBottomBarContent(selectedTab, onTabSelected, modifier)
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
                    label = "首页",
                    onClick = { onTabSelected(0) },
                )
                ClassicBottomTab(
                    selected = selectedTab == 1,
                    icon = SettingsIcon,
                    label = "设置",
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
                GlassTabContent(selected = selectedTab == 0, icon = HomeIcon, label = "首页")
            }
            PrismalGlassBottomTab(onClick = { onTabSelected(1) }) {
                GlassTabContent(selected = selectedTab == 1, icon = SettingsIcon, label = "设置")
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
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                change.consume()
                            }
                        }

                        if (!dragging) {
                            // 短按（含长按不动后抬起）：按落点所在菜单切换
                            val index = (lastX / tabWidth).toInt().coerceIn(0, tabsCount - 1)
                            onTabSelected(index)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        } else {
                            // 拖动结束：确认触觉
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                    text = section,
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
                text = "以上时间整理自学校官方作息时间表，如有调整以学校最新通知为准。",
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
            text = breakPeriod.kind.label,
            color = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${breakPeriod.timeText} · ${formatDuration(breakPeriod.durationMinutes)}",
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "现在 ${fmtTime(status.nowMinute)}",
                color = colorScheme.onTertiaryContainer,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            when {
                status.currentEntries.isNotEmpty() -> {
                    val current = status.currentEntries
                    Text(
                        text = current.joinToString(" · ") { entryLabel(it) },
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
                            text = "还剩 ${formatDuration(remain)}下课",
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
                                "下一项：${entryLabel(nextEntry)} ${nextEntry.timeText}"
                            } else {
                                "下一项：${nextBreak!!.kind.label} ${nextBreak.timeText}"
                            },
                            color = colorScheme.onTertiaryContainer,
                            fontSize = 13.sp,
                        )
                    }
                }

                status.currentBreaks.isNotEmpty() -> {
                    val br = status.currentBreaks.first()
                    Text(
                        text = "${br.kind.label}中",
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${br.timeText} · 还剩 ${formatDuration(br.endMinute - status.nowMinute)}",
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    status.nextEntry?.let { next ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "距离 ${entryLabel(next)} 还有 ${formatDuration(next.startMinute - status.nowMinute)}",
                            color = colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                status.nextEntry != null -> {
                    val next = status.nextEntry
                    Text(
                        text = "即将开始",
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${entryLabel(next)} ${next.timeText}",
                        color = colorScheme.onTertiaryContainer,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "距离开始 ${formatDuration(next.startMinute - status.nowMinute)}",
                        color = colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                status.nextBreak != null -> {
                    val br = status.nextBreak
                    Text(
                        text = "即将休息",
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
                        text = "距离休息 ${formatDuration(br.startMinute - status.nowMinute)}",
                        color = colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                else -> {
                    Text(
                        text = "今日作息已结束",
                        color = colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = firstEntry?.let { "明早 ${fmtTime(it.startMinute)} ${it.activity}，早点休息" }
                            ?: "早点休息",
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
                    text = entry.activity,
                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                )
                if (showRoundBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.round.shortName,
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
                if (entry.endMinute != null) add("${fmtTime(entry.endMinute)} 下课")
                if (entry.note != null) add(entry.note)
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
private fun entryLabel(entry: ScheduleEntry): String =
    if (entry.round == Round.ALL) entry.activity else "${entry.round.displayName} ${entry.activity}"

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
                text = "为了解校园作息表的实际使用情况，我们希望在应用使用过程中匿名上报以下信息：",
                color = colorScheme.onSurface,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "· 设备信息：机型、品牌、系统版本、屏幕分辨率",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Text(
                text = "· 使用情况：启动与前后台切换时刻、当前查看的时间表与轮次",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Text(
                text = "· 环境信息：网络类型、电量、应用版本",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "以上信息仅用于统计本软件的使用情况，不包含姓名、手机号、账号等" +
                    "任何个人身份内容，不会向任何第三方提供。",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "不同意",
                    onClick = onDisagree,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAgree,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "同意并继续", fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = "隐私政策与匿名统计",
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
                text = "发现新版本 v${info.versionName}，是否立即下载更新？",
                color = colorScheme.onSurface,
                fontSize = 14.sp,
            )
            if (info.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "更新日志：",
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
                    text = "暂不更新",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "立即下载", fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = "发现新版本",
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
                text = "正在下载 $progress%",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
    OverlayDialog(
        show = true,
        title = "正在下载更新",
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
                text = "更新包已下载完成。点击「立即安装」升级到新版本；" +
                    "如系统弹出「允许安装未知应用」提示，请允许后重试。",
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "稍后",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "立即安装", fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    OverlayDialog(
        show = true,
        title = "更新下载完成",
        onDismissRequest = onDismiss,
        content = body,
    )
}
