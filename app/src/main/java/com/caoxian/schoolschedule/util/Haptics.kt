package com.caoxian.schoolschedule.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 触觉反馈：直接走 Vibrator 预定义效果（EFFECT_CLICK / EFFECT_TICK），
 * 强度比 View.performHapticFeedback(CLOCK_TICK) 明显得多，
 * 且不受系统「触摸时振动」设置静默拦截。
 */
object Haptics {

    private fun vibrator(context: Context): Vibrator? {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        return v.takeIf { it?.hasVibrator() == true }
    }

    /** 轻触感（拖动越过菜单边界） */
    fun tick(context: Context) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= 29) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** 确认感（松手确认切换） */
    fun confirm(context: Context) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= 29) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
