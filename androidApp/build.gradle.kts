import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.wire)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 33
        versionName = "1.2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    flavorDimensions += "version"

    productFlavors {
        create("dev") {
            dimension = "version"
            // 开发者版本的包名后缀，使其可以和正式版共存
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            // 环境标识变量
            buildConfigField("String", "CURRENT_FLAVOR_ID", "\"dev\"")

            // 注入开关：开发者版本不隐藏，显示自定义/私有仓库
            buildConfigField("Boolean", "HIDE_CUSTOM_REPOS", "false")
            // 注入开关：开发者版本关闭基准灯塔标签验证
            buildConfigField("Boolean", "ENABLE_LIGHTHOUSE_VERIFICATION", "false")

            // 开发者版本：允许在 UI 中显示 DevTools 选项
            buildConfigField("Boolean", "ENABLE_DEV_TOOLS_OPTION_IN_UI", "true")

            // 允许在 UI 中显示地址栏切换按钮
            buildConfigField("Boolean", "ENABLE_ADDRESS_BAR_TOGGLE_BUTTON", "true")


        }

        create("prod") {
            dimension = "version"

            // 环境标识变量
            buildConfigField("String", "CURRENT_FLAVOR_ID", "\"prod\"")
            // 注入开关：正式版本隐藏自定义/私有仓库
            buildConfigField("Boolean", "HIDE_CUSTOM_REPOS", "true")
            // 注入开关：正式版本开启基准灯塔标签验证
            buildConfigField("Boolean", "ENABLE_LIGHTHOUSE_VERIFICATION", "true")
            // 正式版本：禁止在 UI 中显示 DevTools 选项
            buildConfigField("Boolean", "ENABLE_DEV_TOOLS_OPTION_IN_UI", "false")

            // 禁止在 UI 中显示地址栏切换按钮
            buildConfigField("Boolean", "ENABLE_ADDRESS_BAR_TOGGLE_BUTTON", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    splits {
        abi {
            isEnable = true
            exclude("mips", "mips64", "armeabi", "riscv64", "x86")
            isUniversalApk = false
            include("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("zh", "zh-rCN", "zh-rTW", "en")
    }
}

// Koin 编译器配置
koinCompiler {
    userLogs = false
    unsafeDslChecks = true
}

aboutLibraries {
    collect {
        includePlatform = true
    }
}

dependencies {
    // 引入共享模块
    implementation(project(":shared"))

    // Kotlin & 基础序列化
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)

    // AndroidX 核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose 跨平台构件替换（对应 libs.versions.toml 中的 libs.compose.*）
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)

    // Navigation3 & DataStore & Room
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager & 网络库
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging.interceptor)

    // Koin 依赖注入
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.koin.annotations)

    // 工具库与 SDK
    implementation(libs.slf4j.api)
    implementation(libs.coil.compose)
    implementation(libs.javax.inject)
    implementation(libs.aboutlibraries.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.wire.runtime)
    implementation(libs.kgit)
    implementation(libs.okio)

    // 测试与 Debug 工具
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.compose.ui.tooling)
}

wire {
    sourcePath {
        srcDir("src/main/proto")
    }

    kotlin {
        escapeKotlinKeywords = true
        enumMode = "enum_class"
        rpcRole = "none"
    }
}

androidComponents {
    onVariants { variant ->
        val flavor = variant.flavorName ?: ""
        val buildType = variant.buildType ?: ""
        val versionName = android.defaultConfig.versionName ?: ""

        variant.outputs.forEach { output ->
            val abiFilter = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"

            // 修改输出文件名
            output.outputFileName.set("shiguangschedule-v${versionName}-${flavor}-${abiFilter}-${buildType}.apk")
        }
    }
}