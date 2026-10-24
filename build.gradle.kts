// 顶层构建文件
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// AGP 9 内置 Kotlin 支持默认使用 KGP 2.2.10；
// MIUIX 0.9.4 等依赖基于 Kotlin 2.4.20 编译，需将 KGP 提升到 2.4.20
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}
