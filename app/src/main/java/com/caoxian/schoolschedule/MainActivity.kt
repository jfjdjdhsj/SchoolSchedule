package com.caoxian.schoolschedule

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarStyle()
        val settingsStore = SettingsStore(applicationContext)
        reporter = DeviceReporter(applicationContext, settingsStore)
        setContent {
            SchoolScheduleTheme {
                ScheduleScreen(
                    settings = settingsStore,
                    onConsentGiven = { reporter.report("startup") },
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
        // 系统深浅模式切换时刷新状态栏 / 导航栏图标颜色
        applySystemBarStyle()
    }

    /** 透明系统栏 + 图标颜色随系统深浅模式自动切换 */
    private fun applySystemBarStyle() {
        val style = SystemBarStyle.auto(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
        ) { resources: Resources -> resources.isSystemDarkMode() }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun Resources.isSystemDarkMode(): Boolean =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
