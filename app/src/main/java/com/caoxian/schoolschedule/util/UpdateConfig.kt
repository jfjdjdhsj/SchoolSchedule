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

    /** GitHub Releases API（检查更新首选） */
    const val RELEASES_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    /** 加速源前缀：依次尝试，空串 = GitHub 直连兜底 */
    val MIRRORS = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "",
    )

    /** jsDelivr CDN（国内可达，API 不可达时回退读 update.json） */
    private const val JSDELIVR = "https://cdn.jsdelivr.net/gh/$REPO_OWNER/$REPO_NAME@$BRANCH/"

    /** GitHub 直连 */
    private const val GITHUB_RAW = "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/$BRANCH/"

    /** 更新信息 update.json 的候选地址（API 失败时的回退，按顺序尝试） */
    fun updateJsonUrls(): List<String> = listOf(
        JSDELIVR + "update.json",
        GITHUB_RAW + "update.json",
    )

    /** Release 资产下载直连地址（下载时再按 [MIRRORS] 套加速源） */
    fun releaseDownloadUrl(tag: String): String =
        "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/$tag/" +
            "SchoolSchedule-$tag-release.apk"
}
