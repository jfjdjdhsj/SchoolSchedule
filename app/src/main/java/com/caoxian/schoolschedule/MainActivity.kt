package com.caoxian.schoolschedule

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.ui.ScheduleScreen
import com.caoxian.schoolschedule.ui.SchoolScheduleTheme
import com.caoxian.schoolschedule.util.DeviceReporter

/**
 * 应用入口：初始化设置存储与使用统计上报器。
 *
 * 上报时机：onCreate（startup 启动）/ onStart（enter 进入前台）/ onStop（exit 退到后台）。
 * 上报受用户隐私同意约束（首次启动弹窗），且客户端有 60 秒节流与失败重传队列，
 * 详见 [DeviceReporter]。
 *
 * 状态栏 / 导航栏：透明 + 图标颜色跟随系统浅深模式（浅色模式深色图标、
 * 深色模式浅色图标），与主题 XML 的 windowLightStatusBar 双保险；
 * 运行中切换系统深浅模式时在 [onConfigurationChanged] 里重新应用。
 */
class MainActivity : ComponentActivity() {

    private lateinit var reporter: DeviceReporter

    /** 应用内主题强制深浅（null = 跟随系统）；onConfigurationChanged 时保持 */
    private var forcedDark: Boolean? = null

    /**
     * 应用内语言覆盖（仅 Android 13 以下生效）：
     * 读取设置中的语言偏好，用 createConfigurationContext 包裹基础 Context，
     * 使 Compose 的 stringResource 走目标语言资源。
     * Android 13+ 由系统「应用语言」（LocaleManager）接管，无需覆盖。
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(wrapLanguageContext(newBase))
    }

    private fun wrapLanguageContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val lang = SettingsStore(base).appLanguage
        if (lang == SettingsStore.LANG_SYSTEM) return base
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarStyle()
        val settingsStore = SettingsStore(applicationContext)
        reporter = DeviceReporter(applicationContext, settingsStore)
        setContent {
            // 主题设置状态：设置页修改后经回调写回并同步状态栏图标颜色
            var themeMode by remember { mutableStateOf(settingsStore.themeMode) }
            var themeColor by remember { mutableStateOf(settingsStore.themeColor) }
            SchoolScheduleTheme(themeMode, themeColor) {
                ScheduleScreen(
                    settings = settingsStore,
                    onConsentGiven = { reporter.report("startup") },
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        settingsStore.themeMode = mode
                        applySystemBarStyle(forceDark = when (mode) {
                            com.caoxian.schoolschedule.data.SettingsStore.THEME_MODE_LIGHT -> false
                            com.caoxian.schoolschedule.data.SettingsStore.THEME_MODE_DARK -> true
                            else -> null
                        })
                    },
                    themeColor = themeColor,
                    onThemeColorChange = { color ->
                        themeColor = color
                        settingsStore.themeColor = color
                    },
                )
            }
        }
        reporter.report("startup")
    }

    override fun onStart() {
        super.onStart()
        reporter.report("enter")
    }

    override fun onStop() {
        super.onStop()
        reporter.report("exit")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 系统深浅模式切换时刷新状态栏 / 导航栏图标颜色（保持应用内主题的强制深浅）
        applySystemBarStyle(forcedDark)
    }

    /**
     * 透明系统栏 + 图标颜色随深浅模式切换。
     * @param forceDark 应用内主题强制深浅时指定（null = 跟随系统）
     */
    fun applySystemBarStyle(forceDark: Boolean? = null) {
        forcedDark = forceDark
        val style = SystemBarStyle.auto(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
        ) { resources: Resources ->
            forceDark ?: resources.isSystemDarkMode()
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun Resources.isSystemDarkMode(): Boolean =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
