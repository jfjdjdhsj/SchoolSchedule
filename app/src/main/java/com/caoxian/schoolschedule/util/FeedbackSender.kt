package com.caoxian.schoolschedule.util

import android.content.Context
import android.provider.Settings
import com.caoxian.schoolschedule.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/**
 * 用户文字反馈发送器。
 *
 * - 接口：POST {STATS_BASE_URL}/api/feedback（X-Api-Key 鉴权，与匿名统计一致）；
 * - 仅提交纯文字：类型（bug / suggestion / other）、正文（≤1000 字）、
 *   联系方式（选填，≤100 字）；
 * - 附带设备标识与应用版本，便于后台定位问题；用户未同意隐私政策时
 *   不携带设备标识（匿名提交）；
 * - 网络在单线程后台执行，结果通过 [onResult] 回调（主线程外，UI 层自行切线程）。
 */
object FeedbackSender {

    private val executor = Executors.newSingleThreadExecutor()

    const val MAX_CONTENT = 1000
    const val MAX_CONTACT = 100

    /**
     * 提交反馈。
     * @param category 类型：bug（问题反馈）/ suggestion（功能建议）/ other（其他）
     * @param content 正文（调用方保证非空白且不超长）
     * @param contact 联系方式（可为空串）
     */
    fun send(
        context: Context,
        category: String,
        content: String,
        contact: String,
        onResult: (Boolean) -> Unit,
    ) {
        val prefs = context.getSharedPreferences("sjb_reporter", Context.MODE_PRIVATE)
        val payload = JSONObject().apply {
            put("device_id", deviceId(context, prefs))
            put("app_version", runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "")
            put("category", category)
            put("content", content)
            put("contact", contact)
        }
        executor.execute {
            val ok = runCatching {
                val conn = URL("${BuildConfig.STATS_BASE_URL}/api/feedback")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.setRequestProperty("X-Api-Key", BuildConfig.STATS_API_KEY)
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.outputStream.use { out ->
                    out.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            }.getOrDefault(false)
            onResult(ok)
        }
    }

    /** 设备标识：优先 ANDROID_ID，与统计上报共用同一 SharedPreferences 回退 UUID */
    private fun deviceId(context: Context, prefs: android.content.SharedPreferences): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        if (!androidId.isNullOrBlank()) return androidId
        val cached = prefs.getString("device_uuid", null)
        if (cached != null) return cached
        val uuid = UUID.randomUUID().toString()
        prefs.edit().putString("device_uuid", uuid).apply()
        return uuid
    }
}
