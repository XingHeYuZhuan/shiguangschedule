plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gradle.license)
}

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    flavorDimensions += "version"

    productFlavors {
        create("dev") {
            dimension = "version"
            // 开发者版本的包名后缀，使其可以和正式版共存
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-DEV"

            // 注入开关：开发者版本不隐藏，显示自定义/私有仓库
            buildConfigField("Boolean", "HIDE_CUSTOM_REPOS", "false")
            // 注入开关：开发者版本关闭基准灯塔标签验证
            buildConfigField("Boolean", "ENABLE_LIGHTHOUSE_VERIFICATION", "false")

            // 开发者版本：允许在 UI 中显示 v
            buildConfigField("Boolean", "ENABLE_DEV_TOOLS_OPTION_IN_UI", "true")
        }

        create("prod") {
            dimension = "version"

            // 注入开关：正式版本隐藏自定义/私有仓库
            buildConfigField("Boolean", "HIDE_CUSTOM_REPOS", "true")
            // 注入开关：正式版本开启基准灯塔标签验证
            buildConfigField("Boolean", "ENABLE_LIGHTHOUSE_VERIFICATION", "true")
            // 正式版本：禁止在 UI 中显示 DevTools 选项
            buildConfigField("Boolean", "ENABLE_DEV_TOOLS_OPTION_IN_UI", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        jvmToolchain(21)
    }
    splits {
        // 启用对 ABI (CPU 架构) 的分包
        abi {
            isEnable = true
            exclude("mips", "mips64", "armeabi", "riscv64")
            isUniversalApk = false
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    sourceSets.getByName("main") {
        java.srcDirs("src/main/java")
        kotlin.srcDirs("src/main/java")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

afterEvaluate {
    tasks.named("assembleProdRelease") {
        dependsOn("licenseProdReleaseReport")
    }
    tasks.named("assembleDevRelease") {
        dependsOn("licenseDevReleaseReport")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jgit)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.androidx.compose.animation)
    implementation(libs.coil.compose)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}