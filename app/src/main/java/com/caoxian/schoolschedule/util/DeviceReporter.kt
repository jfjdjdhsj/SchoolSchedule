package com.caoxian.schoolschedule.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.caoxian.schoolschedule.data.SettingsStore
import com.caoxian.schoolschedule.data.allTimetables
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

/**
 * 设备使用信息上报器。
 *
 * 采集内容：设备唯一标识（ANDROID_ID）、机型 / 品牌 / 厂商 / 系统版本 / SDK、
 * 应用版本、屏幕分辨率、语言与时区、当前时间表与轮次、网络类型、电量，
 * 以及事件类型（startup 启动 / enter 进入前台 / exit 退到后台）与发生时刻。
 *
 * 策略：
 *  - 仅在用户同意隐私政策后上报（[SettingsStore.consentGiven]）；
 *  - 客户端节流：两次上报间隔不少于 60 秒，避免前后台频繁切换打爆接口；
 *  - 失败重传：上报失败的事件进入本地待传队列（最多 20 条），
 *    下次上报成功时自动补发；
 *  - 全部网络操作在单线程后台执行，不阻塞界面。
 */
class DeviceReporter(
    private val context: Context,
    private val settings: SettingsStore,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var lastReportAt = 0L

    /** 上报一次使用事件（受隐私同意与 60 秒节流约束） */
    fun report(event: String) {
        if (!settings.consentGiven) return
        val now = System.currentTimeMillis()
        if (now - lastReportAt < THROTTLE_MS) return
        lastReportAt = now
        executor.execute {
            val ok = runCatching { post(buildPayload(event)) }.getOrDefault(false)
            if (ok) {
                flushQueue()
            } else {
                enqueue(event)
            }
        }
    }

    // ---------------- 数据采集 ----------------

    private fun buildPayload(event: String): JSONObject {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return JSONObject().apply {
            put("device_id", deviceId())
            put("event", event)
            put("reported_at", sdf.format(Date()))
            put("model", Build.MODEL ?: "")
            put("brand", Build.BRAND ?: "")
            put("manufacturer", Build.MANUFACTURER ?: "")
            put("os_version", Build.VERSION.RELEASE ?: "")
            put("sdk_int", Build.VERSION.SDK_INT)
            put("app_version", appVersionName())
            put("app_version_code", appVersionCode())
            put("screen_resolution", screenResolution())
            put("locale", Locale.getDefault().toLanguageTag())
            put("timezone", TimeZone.getDefault().id)
            put("timetable", timetableName())
            put("round", settings.round.displayName)
            put("network_type", networkType())
            put("battery_level", batteryLevel())
        }
    }

    /** 设备唯一标识：优先 ANDROID_ID，取不到时生成本地 UUID 并持久化 */
    private fun deviceId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        if (!androidId.isNullOrBlank()) return androidId
        val cached = prefs.getString(KEY_DEVICE_UUID, null)
        if (cached != null) return cached
        val uuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
        return uuid
    }

    private fun appVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    private fun appVersionCode(): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(0L)

    private fun screenResolution(): String {
        val dm = context.resources.displayMetrics
        return "${dm.widthPixels}x${dm.heightPixels}"
    }

    private fun timetableName(): String = runCatching {
        allTimetables.first { it.id == settings.timetableId }.name
    }.getOrDefault(settings.timetableId)

    private fun networkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unknown"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
    }

    private fun batteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return -1
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    // ---------------- 网络与队列 ----------------

    private fun post(payload: JSONObject): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL("${BackendConfig.BASE_URL}/api/report").openConnection()
                as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("X-Api-Key", BackendConfig.API_KEY)
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "上报失败：${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    /** 上报失败：事件载荷入待传队列（保留最新 MAX_QUEUE 条，丢弃最旧的） */
    private fun enqueue(event: String) {
        val arr = readQueue()
        if (arr.length() >= MAX_QUEUE) arr.remove(0)
        arr.put(buildPayload(event).toString())
        writeQueue(arr)
    }

    /** 上报成功：按入队顺序补发待传队列；中途失败则保留剩余事件 */
    private fun flushQueue() {
        val arr = readQueue()
        if (arr.length() == 0) return
        for (i in 0 until arr.length()) {
            val ok = runCatching { post(JSONObject(arr.getString(i))) }.getOrDefault(false)
            if (!ok) {
                val remaining = JSONArray()
                for (j in i until arr.length()) remaining.put(arr.getString(j))
                writeQueue(remaining)
                return
            }
        }
        writeQueue(JSONArray())
    }

    private fun readQueue(): JSONArray = runCatching {
        JSONArray(prefs.getString(KEY_QUEUE, "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    private fun writeQueue(arr: JSONArray) {
        prefs.edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    companion object {
        private const val TAG = "DeviceReporter"
        private const val PREFS_NAME = "sjb_reporter"
        private const val KEY_QUEUE = "pending_queue"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val THROTTLE_MS = 60_000L
        private const val MAX_QUEUE = 20
    }
}
