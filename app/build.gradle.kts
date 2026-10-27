import java.util.Properties

plugins {
    // AGP 9.0 起内置 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android 插件
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.caoxian.schoolschedule"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.caoxian.schoolschedule"
        minSdk = 26
        targetSdk = 37
        versionCode = 48
        versionName = "1.17.2"

        // 匿名统计后端配置：地址与密钥放在本地 local.properties（不进仓库）
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField(
            "String",
            "STATS_BASE_URL",
            "\"${localProps.getProperty("stats.baseUrl") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "STATS_API_KEY",
            "\"${localProps.getProperty("stats.apiKey") ?: ""}\"",
        )
    }

    // 发布签名配置：密码等敏感信息放在本地 local.properties（已被 .gitignore 排除，
    // 不进仓库）。缺少配置时构建 Debug 可用，Release 需先在 local.properties 写入：
    //   keystore.password=你的密码
    //   keystore.alias=schoolschedule
    signingConfigs {
        create("release") {
            val localProps = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            storeFile = rootProject.file("keystore/schoolschedule.p12")
            storeType = "pkcs12"
            storePassword = localProps.getProperty("keystore.password") ?: ""
            keyAlias = localProps.getProperty("keystore.alias") ?: "schoolschedule"
            keyPassword = localProps.getProperty("keystore.password") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // MIUIX 界面组件库（Compose Multiplatform）
    implementation(libs.miuix.ui)
    // MIUIX 扩展图标库（设置页图标）
    implementation(libs.miuix.icons)

    // 液态玻璃底栏（AGSL 折射 / RenderEffect 模糊 / 低版本磨砂自动降级）
    implementation(libs.prismal)
    // KSU 风格液态玻璃底栏（miuix-blur：backdrop 采样 + RenderEffect 模糊 + AGSL 折射）
    implementation(libs.miuix.blur)

    // 单元测试（当前时段高亮判定）
    testImplementation(libs.junit)
}
