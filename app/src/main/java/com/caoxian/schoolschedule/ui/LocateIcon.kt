package com.caoxian.schoolschedule.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘「定位」图标（MIUIX 内置图标集不含定位类图标）：
 * 外圈 + 十字准线 + 中心点，线性风格，可整体 tint。
 */
val LocateIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "locate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // 外圈（描边）
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(20f, 12f)
            arcToRelative(8f, 8f, 0f, true, true, -16f, 0f)
            arcToRelative(8f, 8f, 0f, true, true, 16f, 0f)
            close()
        }
        // 十字准线（描边）
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(12f, 3.5f)
            lineTo(12f, 8f)
            moveTo(12f, 16f)
            lineTo(12f, 20.5f)
            moveTo(3.5f, 12f)
            lineTo(8f, 12f)
            moveTo(16f, 12f)
            lineTo(20.5f, 12f)
        }
        // 中心点（实心）
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 10f)
            arcToRelative(2f, 2f, 0f, true, true, 0f, 4f)
            arcToRelative(2f, 2f, 0f, true, true, 0f, -4f)
            close()
        }
    }.build()
}
