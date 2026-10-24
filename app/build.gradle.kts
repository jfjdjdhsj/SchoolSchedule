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
        versionCode = 21
        versionName = "1.11.0"
    }

    // 发布签名配置（密钥文件位于项目根目录 keystore/schoolschedule.p12）
    // 正式对外发布前请自行重新生成密钥并修改此处密码，切勿将密钥提交到公开仓库
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/schoolschedule.p12")
            storeType = "pkcs12"
            storePassword = "123456"
            keyAlias = "schoolschedule"
            keyPassword = "123456"
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

    // 单元测试（当前时段高亮判定）
    testImplementation(libs.junit)
}
