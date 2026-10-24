package com.caoxian.schoolschedule.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 底栏「首页」图标：房子轮廓（线性描边，可整体 tint）。
 */
val HomeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // 屋顶 + 墙体
            moveTo(3.5f, 10.2f)
            lineTo(12f, 3.8f)
            lineTo(20.5f, 10.2f)
            lineTo(20.5f, 20f)
            lineTo(3.5f, 20f)
            close()
            // 门
            moveTo(9.6f, 20f)
            lineTo(9.6f, 14.2f)
            lineTo(14.4f, 14.2f)
            lineTo(14.4f, 20f)
        }
    }.build()
}

/**
 * 底栏「设置」图标：三条滑杆 + 圆点（线性描边 + 实心圆点，可整体 tint）。
 */
val SettingsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // 三条滑杆
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 17f)
            lineTo(20f, 17f)
        }
        // 三个旋钮圆点
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 7f)
            arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
            arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
            close()
            moveTo(13f, 12f)
            arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
            arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
            close()
            moveTo(6f, 17f)
            arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
            arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
            close()
        }
    }.build()
}
