pluginManagement {
    repositories {
        // 国内加速源（阿里云镜像，优先）
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        // 官方源兜底
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内加速源（阿里云镜像，优先）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        // 官方源兜底
        google()
        mavenCentral()
        // PrismalAGSL（液态玻璃库）通过 JitPack 分发，镜像无此构件，直连官方源
        maven("https://jitpack.io")
    }
}

rootProject.name = "SchoolSchedule"
include(":app")
