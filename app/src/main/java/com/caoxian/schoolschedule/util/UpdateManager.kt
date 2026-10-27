package com.caoxian.schoolschedule.util

import android.content.Context
import com.caoxian.schoolschedule.data.SettingsStore
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 仓库中最新版本的信息（来自 GitHub Releases API 或 update.json 回退） */
data class UpdateInfo(
    val versionName: String,
    val changelog: String,
    /** update.json 回退源提供的 versionCode；Releases API 来源为 null（用版本号比较） */
    val versionCode: Int? = null,
    /** Release 资产下载地址；为 null 时按 tag 构造 */
    val assetUrl: String? = null,
) {
    /**
     * APK 下载候选地址：首选镜像优先，其余镜像兜底；Release 资产地址最后兜底。
     */
    fun apkUrls(primaryMirror: String): List<String> =
        UpdateConfig.releaseDownloadUrls("v$versionName", primaryMirror) +
            listOfNotNull(assetUrl)
}

/** 更新日志条目（GitHub Release：版本 / 日期 / 说明） */
data class ReleaseLogEntry(val version: String, val date: String, val changelog: String)

/** 更新流程状态机 */
sealed interface UpdateState {
    /** 空闲 */
    data object Idle : UpdateState

    /** 正在检查更新 */
    data object Checking : UpdateState

    /** 发现新版本 */
    data class Available(val info: UpdateInfo) : UpdateState

    /** 已是最新版本 */
    data object Latest : UpdateState

    /** 正在下载（progress：0—100） */
    data class Downloading(val progress: Int) : UpdateState

    /** 下载完成，等待安装 */
    data class ReadyToInstall(val file: File) : UpdateState

    /** 检查或下载失败 */
    data class Failed(val message: String) : UpdateState
}

/**
 * 应用内更新控制器：检查更新、下载 APK、拉起安装。
 *
 * - 检查更新：加速源开启时 jsDelivr 优先（可关，见 [UpdateConfig.updateJsonUrls]）；
 * - 下载 APK：加速源开启时 jsDelivr（按发布标签分发）优先，官方源兜底；
 * - 安装前检查「未知来源应用安装」权限，未授权时跳转系统设置页。
 */
class UpdateController(private val context: Context) {

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 当前应用是否已是最新（优先 versionCode 比较，否则按版本号三段式比较） */
    suspend fun isNewest(info: UpdateInfo): Boolean = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val currentCode = runCatching {
            pm.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        }.getOrDefault(0)
        val currentName = runCatching {
            pm.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
        val remoteCode = info.versionCode
        if (remoteCode != null) {
            remoteCode <= currentCode
        } else {
            compareVersionName(info.versionName, currentName) <= 0
        }
    }

    /** 三段式版本号比较（主.次.补丁），缺段按 0 处理；a > b 返回 1 */
    private fun compareVersionName(a: String, b: String): Int {
        fun parse(v: String) = v.removePrefix("v").split('.').map { it.trim().toIntOrNull() ?: 0 }
        val pa = parse(a)
        val pb = parse(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    /**
     * 检查更新。
     * @param silent 静默模式（进入应用自动检测）：无新版本 / 失败时不改变状态、不提示
     */
    /** 实际生效的镜像：auto = 上次测速选出的最快源；否则为手动指定的镜像 */
    private fun effectiveMirror(): String {
        val s = SettingsStore(context)
        return if (s.mirrorMode == UpdateConfig.MIRROR_AUTO) s.mirrorSaved else s.mirrorMode
    }

    /**
     * 全镜像测速：并发请求各镜像 update.json，保存延迟并选出最快源。
     *
     * @return (最快镜像 ID, 各镜像延迟 ms)
     */
    suspend fun runSpeedTest(): Pair<String, Map<String, Long>> = withContext(Dispatchers.IO) {
        val results = UpdateConfig.MIRRORS.map { id ->
            async { id to UpdateChecker.testMirror(id) }
        }.awaitAll().toMap()
        val settings = SettingsStore(context)
        results.forEach { (id, ms) -> settings.setMirrorLatency(id, ms.toInt()) }
        val best = results.filterValues { it > 0 }.minByOrNull { it.value }?.key
            ?: UpdateConfig.MIRROR_JSDELIVR
        settings.mirrorSaved = best
        best to results
    }

    fun check(silent: Boolean) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        scope.launch {
            try {
                val info = UpdateChecker.check(effectiveMirror())
                if (isNewest(info)) {
                    if (!silent) state = UpdateState.Latest
                    else state = UpdateState.Idle
                } else {
                    state = UpdateState.Available(info)
                }
            } catch (e: Exception) {
                if (!silent) state = UpdateState.Failed(e.message ?: "检查更新失败")
                else state = UpdateState.Idle
            }
        }
    }

    /** 下载新版本 APK（带进度回调） */
    fun download(info: UpdateInfo) {
        if (state is UpdateState.Downloading) return
        state = UpdateState.Downloading(0)
        scope.launch {
            try {
                val dir = File(context.getExternalFilesDir("update") ?: File(context.filesDir, "update"), "")
                dir.mkdirs()
                val target = File(dir, "SchoolSchedule-${info.versionName}.apk")
                UpdateChecker.downloadApk(info, target, effectiveMirror()) { progress ->
                    state = UpdateState.Downloading(progress)
                }
                state = UpdateState.ReadyToInstall(target)
            } catch (e: Exception) {
                state = UpdateState.Failed(e.message ?: "下载失败")
            }
        }
    }

    /**
     * 拉起安装。未授予「安装未知应用」权限时先跳转系统设置，
     * 用户授权后再次点击「立即安装」即可。
     */
    fun startInstall(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            // 跳转到本应用的「允许安装未知应用」系统设置页
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        runCatching { context.startActivity(intent) }
    }

    /** 拉取更新日志（委托 [UpdateChecker.fetchReleaseLog]，实现与缓存说明见其文档） */
    suspend fun fetchReleaseLog(): Pair<List<ReleaseLogEntry>?, String?> =
        UpdateChecker.fetchReleaseLog(context)
}

/** 更新检查与下载实现（纯网络逻辑，无状态） */
object UpdateChecker {

    /** 更新日志本地缓存 prefs 键名 */
    private const val KEY_LOG_CACHE = "log_cache"
    private const val KEY_LOG_CACHE_AT = "log_cache_at"

    /**
     * 检查更新：优先 GitHub Releases API（含更新日志与 APK 资产地址）；
     * API 不可达时回退 update.json（jsDelivr → GitHub 直连）。全部失败抛异常。
     */
    suspend fun check(primaryMirror: String): UpdateInfo = withContext(Dispatchers.IO) {
        // 镜像源优先（国内可达性最好），GitHub Releases API 兜底
        try {
            return@withContext checkViaUpdateJson(primaryMirror)
        } catch (_: Exception) {
        }
        try {
            return@withContext checkViaReleasesApi()
        } catch (_: Exception) {
        }
        return@withContext checkViaUpdateJson(UpdateConfig.MIRROR_GITHUB)
    }

    /**
     * 镜像测速：TCP 连接测速（等效 ping，一个网络往返），2 秒超时。
     * 失败 / 超时返回 -1。
     */
    suspend fun testMirror(mirror: String): Long = withContext(Dispatchers.IO) {
        val host = when (mirror) {
            UpdateConfig.MIRROR_JSDELIVR -> "cdn.jsdelivr.net"
            UpdateConfig.MIRROR_GHFAST -> "ghfast.top"
            UpdateConfig.MIRROR_GHPROXY -> "gh-proxy.com"
            else -> "raw.githubusercontent.com"
        }
        val start = System.currentTimeMillis()
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, 443), 2000)
                System.currentTimeMillis() - start
            }
        } catch (_: Exception) {
            -1
        }
    }

    /** update.json 检查：按候选地址顺序尝试（加速源开 = jsDelivr → raw；关 = raw） */
    private suspend fun checkViaUpdateJson(primaryMirror: String): UpdateInfo = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in UpdateConfig.updateJsonUrls(primaryMirror)) {
            try {
                val body = httpGet(url) ?: continue
                val json = JSONObject(body)
                return@withContext UpdateInfo(
                    versionName = json.optString("versionName"),
                    changelog = json.optString("changelog"),
                    versionCode = json.optInt("versionCode", 0),
                )
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw (lastError ?: IllegalStateException("检查更新失败"))
    }

    /** GitHub Releases API：releases/latest → tag_name / body / APK 资产地址 */
    private fun checkViaReleasesApi(): UpdateInfo {
        val conn = URL(UpdateConfig.RELEASES_API).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "SchoolSchedule-App")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode !in 200..299) {
                error("GitHub API HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name", "").removePrefix("v")
            if (tag.isBlank()) error("仓库还没有发布任何版本")
            val changelog = json.optString("body", "")
            var assetUrl: String? = null
            json.optJSONArray("assets")?.let { assets ->
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name", "").endsWith(".apk")) {
                        assetUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            return UpdateInfo(
                versionName = tag,
                changelog = changelog,
                versionCode = null,
                assetUrl = assetUrl?.takeIf { it.isNotBlank() },
            )
        } finally {
            conn.disconnect()
        }
    }

    /** 下载 APK：对直连地址依次套用加速源前缀，首个成功者生效 */
    suspend fun downloadApk(
        info: UpdateInfo,
        target: File,
        primaryMirror: String,
        onProgress: (Int) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in info.apkUrls(primaryMirror)) {
            try {
                httpDownload(url, target, onProgress)
                return@withContext
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw (lastError ?: IllegalStateException("下载失败"))
    }

    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "SchoolSchedule-App")
        try {
            if (conn.responseCode !in 200..299) return null
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 拉取更新日志（GitHub Releases 列表，最新在前，最多 30 条）。
     * API 直连不可达时按加速源前缀重试；全部失败时回退本地缓存。
     *
     * @return 第一项：日志列表（null = 拉取失败且无缓存）；第二项：缓存时间（离线数据时非 null）
     */
    suspend fun fetchReleaseLog(
        context: Context,
    ): Pair<List<ReleaseLogEntry>?, String?> = withContext(Dispatchers.IO) {
        val logPrefs = context.getSharedPreferences("sjb_update_log", Context.MODE_PRIVATE)
        val api = "https://api.github.com/repos/${UpdateConfig.REPO_OWNER}/${UpdateConfig.REPO_NAME}/releases?per_page=30"
        val urls = listOf(api)
        var result: List<ReleaseLogEntry>? = null
        for (url in urls) {
            try {
                val body = httpGet(url) ?: continue
                val arr = org.json.JSONArray(body)
                val out = ArrayList<ReleaseLogEntry>()
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    val version = r.optString("tag_name").removePrefix("v")
                    if (version.isBlank()) continue
                    out.add(
                        ReleaseLogEntry(
                            version = version,
                            date = r.optString("published_at").take(10),
                            changelog = r.optString("body"),
                        ),
                    )
                }
                if (out.isNotEmpty()) {
                    result = out
                    break
                }
            } catch (_: Exception) {
                // 尝试下一个地址
            }
        }
        if (result != null) {
            // 成功：写本地缓存供离线查看
            runCatching {
                val arr = org.json.JSONArray()
                result!!.forEach {
                    arr.put(
                        JSONObject().put("v", it.version).put("d", it.date).put("c", it.changelog),
                    )
                }
                logPrefs.edit()
                    .putString(KEY_LOG_CACHE, arr.toString())
                    .putString(
                        KEY_LOG_CACHE_AT,
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                            .format(java.util.Date()),
                    )
                    .apply()
            }
            result to null
        } else {
            // 失败：回退本地缓存
            val cached = logPrefs.getString(KEY_LOG_CACHE, null) ?: return@withContext null to null
            runCatching {
                val arr = org.json.JSONArray(cached)
                val out = ArrayList<ReleaseLogEntry>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(ReleaseLogEntry(o.optString("v"), o.optString("d"), o.optString("c")))
                }
                out
            }.getOrNull() to logPrefs.getString(KEY_LOG_CACHE_AT, null)
        }
    }

    private fun httpDownload(url: String, target: File, onProgress: (Int) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "SchoolSchedule-App")
        conn.instanceFollowRedirects = true
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastNotified = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastNotified) {
                                lastNotified = percent
                                onProgress(percent)
                            }
                        }
                    }
                    if (total <= 0) onProgress(100)
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
