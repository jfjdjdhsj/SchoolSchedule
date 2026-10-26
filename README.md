# 校园作息表（SchoolSchedule）

基于 [MIUIX](https://compose-miuix-ui.github.io/miuix/zh_CN/)（Compose Multiplatform 界面组件库）开发的安卓作息时间表应用，内置**冬季 / 夏季两套作息时间表，支持一键切换**。当前版本 **v1.17.0**（versionCode 46）。

## 双玻璃引擎

底栏液态玻璃支持两种实现（设置 → 外观 → 底栏玻璃效果 切换对比）：
- **Prismal**（默认）：`PrismalAGSL` 引擎，自适应亮度 + 按压增压（折射/色散随进度增强）+ 顶部高光；
- **KSU**：完整移植自 [KernelSU Manager](https://github.com/tiann/KernelSU) 的 `FloatingBottomBar`（miuix-blur），参数与 KSU 完全一致——vibrancy 饱和度 1.5、blur 4dp×4dp、lens 折射 24dp/24dp、40% 半透明底色、选中指示器二次折射 + 色散 0.5 + 内阴影，重力感应高光。
Android 12 以下自动回退 Prismal 磨砂降级（AGSL 需 API 31+）。

## 多语言（i18n）

支持 **简体中文（默认）/ English / 繁體中文 / 日本語 / Русский** 共 5 种语言。设置 → 外观 → 语言 可手动切换：Android 13+ 走系统「应用语言」（`LocaleManager`，桌面小组件同步生效），旧版本通过语言偏好 + Activity 重建即时生效。**作息表内容完全翻译**（时段、活动名、轮次名、时间表名、备注均随语言切换，见 `util/ScheduleI18n.kt`）。

## 版本发布流程（本地构建 + gh 发布）

> 项目采用双目录工作流：本目录（SchoolSchedule）为**本地完整版**（含 local.properties、
> keystore、server/config.json 等私有配置，不连接远程）；`/workspace/SchoolSchedule-git`
> 为**GitHub 脱敏版**（由 sync-to-git.sh 自动导出，含敏感值扫描）。推送一律走后者。

1. 本地改好版本号（`app/build.gradle.kts` 的 versionCode / versionName，同步 `update.json`）并构建验证：
   `gradle :app:assembleRelease`
2. 提交并推送：
   ```bash
   bash sync-to-git.sh "release: vX.Y.Z 说明"
   ```
3. 打附注标签（**标签内容 = 更新日志**）并推送（在 SchoolSchedule-git 仓库操作）：
   ```bash
   git -C /workspace/SchoolSchedule-git tag -a vX.Y.Z -m "1. xxx" -m "2. xxx"
   git -C /workspace/SchoolSchedule-git push origin vX.Y.Z
   ```
4. 用 GitHub CLI 发布 Release（更新日志 = 标签内容）：
   ```bash
   gh release create vX.Y.Z -R jfjdjdhsj/SchoolSchedule \
     "app/build/outputs/apk/release/app-release.apk#SchoolSchedule-vX.Y.Z-release.apk" \
     --title "校园作息表 vX.Y.Z" \
     --notes "$(git -C /workspace/SchoolSchedule-git tag -l --format='%(contents)' vX.Y.Z)"
   ```
5. APP 内「设置 → 检测更新」即可发现新版本：检查走 GitHub Releases API（回退 jsDelivr 的 update.json），下载走加速源（ghfast / gh-proxy，直连兜底）。

数据来源为学校官方 Excel 作息表：

| 时间表 | 学校 | 执行时间 |
| --- | --- | --- |
| 冬季作息 | 曹县高级技工学校 | 2026年10月1日起执行 |
| 夏季作息 | 曹县高级技工学校 | 2026年5月1日起执行 |

## 版本号规则

版本号采用三段式 `主.次.补丁`，`versionCode` 每次发版严格 +1：

| 变更类型 | 版本号变化 | 示例 |
| --- | --- | --- |
| Bug 修复（不改功能） | 补丁号 +1 | `1.3.0 → 1.3.1` |
| 新增 / 调整功能 | 次版本号 +1，补丁号归零 | `1.3.1 → 1.4.0` |

主版本号仅在不兼容的数据结构或整体重写时变更。

## 功能特性

- **双时间表切换**：顶部标签栏在「冬季作息 / 夏季作息」之间即时切换，副标题同步显示对应学校与执行日期
- **轮次查看**：默认显示「第一轮 / 第二轮」两个视图，选择后只显示与本轮相关的节点；「全部」视图默认隐藏，**在设置页开启「显示「全部」轮次」**后出现，按时间线混合展示两轮安排并带轮次徽章（开关状态会被记住）
- **实时状态卡片**：自动高亮当前正在进行的项目（每 30 秒刷新），显示剩余时间、下课倒计时与下一项安排；课间休息中 / 即将开始 / 今日已结束均有对应提示
- **课间休息与下课时间**：从时间表自动推导课间休息 / 长间休时段并穿插在时间轴中（同样支持高亮），每个节点行标注下课时刻
- **一键定位**：右下角悬浮按钮平滑滚动到当前进行中（或休息中 / 下一项）节点；v1.8 起为**纯液态玻璃**圆形按钮（去掉蓝色底色），中间箭头改为主题前景色（浅色模式深色 / 深色模式浅色），按压时玻璃折射与高光增强
- **匿名使用统计**（v1.2 新增，v1.8 起可在设置中开关）：首次启动弹出隐私政策，同意后匿名上报设备与使用信息至学校服务器；**设置 → 数据与隐私 → 匿名使用统计**可随时开启 / 关闭，即时生效
- **标题区随滚动**：首页标题、时钟与时间表/轮次标签是列表首项，向上滑动随之隐藏、下滑回到顶部即出现
- **左右滑动切页**（v1.7 新增）：首页与设置页由 HorizontalPager 承载，在页面上**向左滑动进入设置、向右滑回首页**，与底栏点击 / 拖动切换双向同步；玻璃底栏实时采样滑动中的页面内容
- **底部导航栏**（v1.3 新增，v1.6 优化交互与玻璃效果，v1.8.1 经典底栏沉浸式）：首页 / 设置两个页面；底栏可选 **iOS 风格液态玻璃**（AGSL 折射 + 动态模糊，Android 13+ 效果最佳，低版本自动降级），设置页可随时开关。v1.6：按下即有玻璃增压反馈，**水平滑动直接进入拖动**（无需长按），切页 / 拖动带触觉反馈；玻璃随内容明暗自适应调节，增强态叠加 RGB 色散与边缘高光，选中胶囊颜色统一为主题主色。v1.8.1：关闭玻璃后的经典底栏也支持沉浸式——半透明背景，内容与手势指示条区域透出
- **设置页**（v1.3 新增，v1.10 添加图标）：底栏液态玻璃开关、「全部」轮次视图开关、匿名统计状态、版本与数据来源；**开源库**（v1.9 新增）：点击弹窗展示 APP 使用的开源组件（MIUIX、PrismalAGSL、Jetpack Compose、Kotlin）及其许可；设置项均带 MIUIX 官方图标（`miuix-icons` 扩展图标库：Theme / Show / Lock / Community）
- **自动选表**：首次打开按月份自动选择（5—9 月夏季表，其余冬季表），用户选择后记忆
- **MIUIX 风格界面**：Scaffold + TabRow + Card + OverlayDialog 等 MIUIX 组件，浅色 / 深色主题跟随系统

## 技术栈

| 项 | 版本 |
| --- | --- |
| Kotlin | 2.4.20 |
| AGP（Android Gradle Plugin） | 9.1.0 |
| Gradle | 9.3.1（Wrapper） |
| MIUIX | 0.9.4（`top.yukonga.miuix.kmp:miuix-ui-android`） |
| MIUIX 图标 | 0.9.4（`top.yukonga.miuix.kmp:miuix-icons-android`，设置页图标） |
| Compose Multiplatform | 1.12.0（MIUIX 传递依赖） |
| PrismalAGSL | 1.0.4（`com.github.styropyr0:PrismalAGSL`，JitPack；液态玻璃底栏） |
| compileSdk / minSdk / targetSdk | 37 / 26 / 37 |

## 目录结构

```
SchoolSchedule/
├── settings.gradle.kts            # 仓库与模块配置
├── build.gradle.kts               # 顶层构建文件
├── gradle/libs.versions.toml      # 版本目录
├── gradlew                        # Gradle Wrapper
├── update.json                    # 应用内更新元数据（versionCode / 更新日志）
├── keystore/schoolschedule.p12    # 发布签名密钥（本地私有，勿上传公开仓库）
├── server/                        # 统计后台服务端（纯 Python 标准库，部署见 server/README.md）
│   ├── server.py                  # 后端服务（API + 管理网页；密钥全部外置 config.json）
│   ├── config.example.json        # 配置模板（api_key / page_token / admin_password）
│   └── sjb.service                # systemd 服务单元
└── app/
    ├── build.gradle.kts           # 应用模块构建配置（签名密码经 local.properties 注入）
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/caoxian/schoolschedule/
        │   ├── MainActivity.kt                # 入口 Activity（含使用统计上报钩子）
        │   ├── data/
        │   │   ├── ScheduleModels.kt          # 数据模型（Timetable / ScheduleEntry / Round）
        │   │   ├── ScheduleData.kt            # 冬/夏两套作息表数据
        │   │   ├── BreakModels.kt             # 休息段模型（课间休息 / 长间休）
        │   │   ├── BreakCalculator.kt         # 由时间表推导休息段
        │   │   └── SettingsStore.kt           # SharedPreferences 持久化
        │   ├── ui/
        │   │   ├── Theme.kt                   # MIUIX 主题（深浅模式 + Monet 主题色）
        │   │   ├── ScheduleStatus.kt          # 实时状态计算
        │   │   ├── ScheduleScreen.kt          # 主界面（首页/设置切换、底栏、时间轴）
        │   │   ├── SettingsScreen.kt          # 设置页（外观/显示/隐私/更新/反馈/关于）
        │   │   ├── NavIcons.kt                # 底栏自绘图标（首页 / 设置）
        │   │   └── LocateIcon.kt              # 自绘定位图标
        │   └── util/
        │       ├── TimeUtils.kt               # 时间格式化工具
        │       ├── RoundVisibility.kt         # 轮次显隐规则
        │       ├── UpdateConfig.kt            # 应用内更新接入配置（GitHub Releases）
        │       ├── UpdateManager.kt           # 检查更新 / 下载 / 安装（加速源）
        │       ├── DeviceReporter.kt          # 设备使用信息上报（节流 + 失败重传）
        │       └── FeedbackSender.kt          # 用户文字反馈提交
        └──res/
            ├── values/、values-night/         # 字符串、主题（含 API 31+ 启动画面）
            └── mipmap-*/                     # 自适应启动图标（五档密度）
```

## 构建方法

### 环境要求

- Android Studio（最新稳定版，自带 JDK 17 与 Android SDK）
- 或命令行：JDK 17+、Android SDK（android-37 平台，可通过 SDK Manager 安装）

> 注：首次在命令行构建前，请在项目根目录创建 `local.properties` 并写入 `sdk.dir=/你的/Android/Sdk路径`（Android Studio 会自动生成，该文件已被 .gitignore 忽略）。

### 命令行构建

```bash
#  Debug APK
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

#  Release APK（开启 R8 混淆与资源压缩并自动签名，约 1.8 MB）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk（已签名，可直接安装分发）
```

### Android Studio

1. `File → Open` 选择本目录
2. 等待 Gradle Sync 完成
3. 点击 Run ▶ 安装到设备 / 模拟器（minSdk 26，即 Android 8.0 及以上）

## 签名信息

项目已内置发布签名配置（`app/build.gradle.kts` 的 `signingConfigs`），`assembleRelease` 产出的 APK 可直接安装分发。

| 项 | 值 |
| --- | --- |
| 密钥文件 | `keystore/schoolschedule.p12`（PKCS12，RSA 2048） |
| 密钥别名 | `schoolschedule` |
| 密钥库 / 密钥密码 | 存放在本地 `local.properties`（`keystore.password` / `keystore.alias`），**不进仓库** |
| 证书 SHA-256 | `aaea6cc05decc964c170e98f8866ad7daf304a4bf0a6f7b242111c1705f81f81` |

> **注意**：密钥文件 `keystore/*.p12` 与密码配置均已被 `.gitignore` 排除，请勿提交到仓库或分享给他人。丢失密钥将无法为应用提供升级。若需重新生成密钥：
> ```bash
> keytool -genkeypair -v -keystore keystore/schoolschedule.p12 -storetype pkcs12 \
>   -keyalg RSA -keysize 2048 -validity 10950 -alias schoolschedule \
>   -dname "CN=你的名称, OU=单位, O=组织, L=城市, ST=省份, C=CN"
> ```
> 同时在 `local.properties` 更新 `keystore.password`。

## 匿名使用统计与隐私（v1.2）

应用首次启动时会弹出「隐私政策与匿名统计」对话框，**必须做出选择**（不可跳过）：

| 选择 | 结果 |
| --- | --- |
| 同意并继续 | 开启匿名统计，上报设备与使用信息 |
| 不同意 | 关闭弹窗，仅不参与统计，其他功能正常使用 |

上报时机：应用启动（startup）、进入前台（enter）、退到后台（exit）；客户端有 60 秒节流，上报失败的事件进入本地待传队列（最多 20 条），下次上报成功时自动补发。**不上报任何个人身份信息**，采集内容如下：

| 类别 | 内容 |
| --- | --- |
| 设备信息 | 设备唯一标识（ANDROID_ID）、机型、品牌、厂商、系统版本、SDK、屏幕分辨率 |
| 使用情况 | 事件类型与发生时刻、当前查看的时间表与轮次 |
| 环境信息 | 应用版本、网络类型、电量、IP（由服务器记录） |

上报服务端部署在学校自有服务器上（纯 Python 标准库实现，systemd 守护，数据存 SQLite），仅接收匿名统计并存储；服务端地址与鉴权密钥仅在应用内配置（构建时注入），管理页面需管理密码进入，相关凭据不对外公开。

> 服务端源码与部署文档见 [`server/`](server/README.md)（本仓库已脱敏：所有密钥 / 密码均通过服务端 `config.json` 外置，不进仓库）。

## 底部导航与液态玻璃（v1.3）

底栏为「首页 / 设置」两页导航，玻璃效果基于 [PrismalAGSL](https://github.com/styropyr0/PrismalAGSL)（MIT）：

- **采样机制**：首页时间轴与设置页绘制在 `Modifier.prismalGlassLayer(backdrop)` 标记的内容层中，底栏玻璃实时采样其背后内容（滚动时折射 / 模糊随之变化）
- **三档管线自动降级**：API 33+ 完整 AGSL 折射与色散；API 31–32 `RenderEffect` 动态模糊；API 25–30 磨砂覆盖。代码无需分支判断
- **交互**：点击「首页 / 设置」即时切换，选中胶囊以弹簧动画滑向目标菜单；**长按底栏进入拖动**（长按瞬间玻璃折射与高光增强显现），手指移到哪个菜单就切到哪个（普通滑动不会误触）；玻璃层实时采样背后内容，滚动时模糊与折射随之变化
- **实现说明**：底栏使用库的 `PrismalGlassBottomTabs`（胶囊玻璃 + 可拖拽选中胶囊 + 按压动效），并在最上层叠加一个「只观察、不消费事件」的点击热区，解决该组件手势层挡住 tab 点击的问题
- **开关**：设置 → 外观 → 「底栏液态玻璃」，默认开启；关闭后使用经典纯色底栏（顶部发丝分隔线 + 主色选中态），状态持久化（`bottom_bar_glass_enabled`）
- **依赖**：`com.github.styropyr0:PrismalAGSL:1.0.4`（JitPack，仓库地址已加入 `settings.gradle.kts`）

## 国内加速源

为在国内网络环境下快速构建，仓库源已全部切换为阿里云镜像 + 官方源兜底（`settings.gradle.kts`）：

| 用途 | 镜像 |
| --- | --- |
| Gradle 插件 / AGP / Kotlin 插件 | `https://maven.aliyun.com/repository/gradle-plugin` |
| Google / AndroidX 构件 | `https://maven.aliyun.com/repository/google` |
| Central / 三方库（MIUIX 等） | `https://maven.aliyun.com/repository/public` |
| Gradle Wrapper 分发 | `https://mirrors.cloud.tencent.com/gradle/` |
| PrismalAGSL（JitPack） | `https://jitpack.io`（镜像无此构件，官方源直连） |

镜像不可达时自动回退 `google()` / `mavenCentral()` / `gradlePluginPortal()` 官方源。阿里云 jitpack 镜像不可用（返回 401），本项目无 jitpack 依赖，不受影响。

## 使用说明

1. **首次启动**：阅读「隐私政策与匿名统计」并选择同意 / 不同意（不影响其他功能使用）
2. **切换页面**：点击底部导航栏「首页 / 设置」
3. **切换时间表**：首页顶部「冬季作息 / 夏季作息」标签
4. **切换轮次**：首页第二行「第一轮 / 第二轮」标签（适用于分两轮组织活动与就餐的安排）；需要「全部」轮次视图时，到 **设置 → 显示「全部」轮次** 开启
5. **查看当前状态**：顶部蓝色卡片实时显示正在进行的项目、剩余时间、课间休息与下一项；标题栏右侧显示当前时间（时:分:秒，浅色模式黑字 / 深色模式白字）
6. **一键定位**：点击右下角液态玻璃圆形按钮（按压时玻璃增强并轻微放大），直接跳到当前进行中（或休息中）的节点，并使其显示在屏幕垂直中间
7. **底栏液态玻璃**：设置 → 外观 → 「底栏液态玻璃」开关（默认开启；关闭后为经典纯色底栏）

## 数据说明

两套时间表的全部节点（起床、早操/晨会、早饭、晨读、预备、第一至七节、午饭、午休、素质提升、晚饭、晚自习、宿舍检查、熄灯）均按官方表格 1:1 录入，包含「不能提前下课」「开始进入餐厅」「开始从集合点出发」等备注；单点项目（如「起床 6:00」「熄灯 21:10」）以时刻形式展示。

## 许可

本项目代码可自由使用；界面组件库 MIUIX 遵循 Apache-2.0 许可。
