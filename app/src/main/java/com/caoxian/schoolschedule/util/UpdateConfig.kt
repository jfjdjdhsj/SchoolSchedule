package com.caoxian.schoolschedule.util

/**
 * 应用内更新接入配置：版本通过 **GitHub Releases** 发布，
 * 检查更新走 GitHub Releases API（直连，失败时回退 update.json），
 * 下载 APK 走加速源（国内可达），直连兜底。
 *
 * 发布流程（发新版本时）：
 * 1. 本地改好版本号（versionCode / versionName）并构建验证；
 * 2. 提交代码：`git commit`；
 * 3. 打附注标签（标签内容 = 更新日志）：`git tag -a v1.12.0 -m "1. xxx\n2. xxx"`;
 * 4. 推送：`git push origin main --tags`；
 * 5. GitHub Actions 自动构建并发布 Release（APK 资产名：
 *    `SchoolSchedule-v{版本}-release.apk`，更新日志 = 标签内容）；
 * 6. APP 内「检测更新」即可发现新版本并下载安装。
 *
 * 仓库需配置 Secret：KEYSTORE_BASE64（签名密钥 schoolschedule.p12 的 base64），
 * 保证 CI 构建的 APK 与本地签名一致，可覆盖安装。
 */
object UpdateConfig {

    /** GitHub 仓库坐标（公开发布仓库） */
    const val REPO_OWNER = "jfjdjdhsj"
    const val REPO_NAME = "SchoolSchedule"
    const val BRANCH = "main"

    /** 镜像 ID */
    const val MIRROR_AUTO = "auto"
    const val MIRROR_JSDELIVR = "jsdelivr"
    const val MIRROR_GITHUB = "github"
    const val MIRROR_GHFAST = "ghfast"
    const val MIRROR_GHPROXY = "ghproxy"

    /** 可选镜像（测速 / 手动选择） */
    val MIRRORS = listOf(MIRROR_JSDELIVR, MIRROR_GITHUB, MIRROR_GHFAST, MIRROR_GHPROXY)

    /** GitHub Releases API（检查更新 / 更新日志兜底） */
    const val RELEASES_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    private const val GITHUB_RAW = "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/$BRANCH/"
    private const val GITHUB_DOWNLOAD =
        "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/"
    private const val JSDELIVR = "https://cdn.jsdelivr.net/gh/$REPO_OWNER/$REPO_NAME"
    private const val GHFAST = "https://ghfast.top/"
    private const val GHPROXY = "https://gh-proxy.com/"

    fun apkAssetName(tag: String) = "SchoolSchedule-$tag-release.apk"

    /**
     * 镜像的探测 / 检查地址（update.json）：同时用于测速与检查更新。
     * 代理类镜像直接代理 GitHub raw。
     */
    fun jsonProbeUrl(mirror: String): String = when (mirror) {
        MIRROR_JSDELIVR -> "$JSDELIVR@$BRANCH/update.json"
        MIRROR_GHFAST -> GHFAST + GITHUB_RAW + "update.json"
        MIRROR_GHPROXY -> GHPROXY + GITHUB_RAW + "update.json"
        else -> GITHUB_RAW + "update.json"
    }

    /**
     * update.json 候选地址：[primary] 优先，其余镜像按默认顺序兜底。
     */
    fun updateJsonUrls(primary: String): List<String> =
        (listOf(primary) + MIRRORS).distinct().map { jsonProbeUrl(it) }

    /**
     * APK 下载候选地址：[primary] 优先，其余镜像兜底（jsDelivr 按发布标签分发，
     * 代理类镜像代理 GitHub Releases 直连地址）。
     */
    fun releaseDownloadUrls(tag: String, primary: String): List<String> {
        val official = GITHUB_DOWNLOAD + "$tag/" + apkAssetName(tag)
        val byMirror = mapOf(
            MIRROR_JSDELIVR to "$JSDELIVR@$tag/" + apkAssetName(tag),
            MIRROR_GITHUB to official,
            MIRROR_GHFAST to GHFAST + official,
            MIRROR_GHPROXY to GHPROXY + official,
        )
        return (listOf(primary) + MIRRORS).distinct().mapNotNull { byMirror[it] }
    }
}
