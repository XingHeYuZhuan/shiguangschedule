plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.wire)
}

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = 26
        targetSdk = 37
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
        // 启用对 ABI (CPU 架构) 的分包
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
aboutLibraries {
    collect {
        includePlatform = true
    }
}
dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jgit) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "com.googlecode.javaewah", module = "JavaEWAH")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.android)
    implementation(libs.androidx.compose.animation)
    implementation(libs.coil.compose)
    implementation(libs.javax.inject)
    implementation(libs.androidx.appcompat)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.aboutlibraries.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.wire.runtime)
    implementation(libs.poi.ooxml)

    debugImplementation(libs.okhttp.logging.interceptor)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
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
