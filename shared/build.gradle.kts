import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.wire)
}

kotlin {
    listOf(
        iosArm64(),
        // iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    android {
        namespace = "com.xingheyuzhuan.shiguangschedule.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            // Lifecycle & ViewModel
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Serialization & Tools
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.cbor)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kgit)
            implementation(libs.okio)

            // Ktor 核心及功能插件
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)

            // Koin 依赖注入
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Room 3.0 & DataStore
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.datastore.core)

            // Wire 运行时
            implementation(libs.wire.runtime)
        }

        androidMain.dependencies {
            implementation(libs.androidx.sqlite.framework)
            implementation(libs.ktor.client.cio)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.cio)
        }

        iosMain.dependencies {
            implementation(libs.androidx.sqlite.framework)
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    // add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}

// Room 3.0 插件配置 schema 导出路径
room3 {
    schemaDirectory("$projectDir/schemas")
}

// Koin 编译器行为配置
koinCompiler {
    userLogs = false
    unsafeDslChecks = true
}

// Wire 编译配置
wire {
    sourcePath {
        srcDir("src/commonMain/proto")
    }

    kotlin {
        escapeKotlinKeywords = true
        enumMode = "enum_class"
        rpcRole = "none"
    }
}