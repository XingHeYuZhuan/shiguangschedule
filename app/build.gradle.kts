plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gradle.license)
}

// 定义清理函数，仅用于清理值，防止 Secret 包含不可见字符
fun String.cleanAndTrim(): String {
    // 移除所有控制字符（\p{Cntrl}），然后进行标准 trim()
    return this.replace("\\p{Cntrl}".toRegex(), "").trim()
}

val propertiesFile = project.file("keystore.properties")
val signingPropertiesMap = mutableMapOf<String, String>()

if (propertiesFile.exists()) {
    try {
        propertiesFile.readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val match = "(.+?)=(.*)".toRegex().matchEntire(line.trim())
                if (match != null) {
                    // ⭐ 终极修正：对键和值都只使用标准的 .trim()
                    val key = match.groupValues[1].trim()
                    // 注意：这里不再调用 cleanAndTrim()，仅使用标准 trim()
                    val value = match.groupValues[2].trim()

                    signingPropertiesMap[key] = value

                    // ⭐ 打印实际值（不再隐藏）
                    println("DEBUG: 实际存储的键名和值: [$key] = [$value]")
                }
            }
    } catch (e: Exception) {
        println("ERROR: 手动读取 keystore.properties 失败: ${e.message}")
        throw e
    }
}

val keystoreFile = "release.jks"

// ⭐ 最终的终极查找函数：使用迭代器而非索引，确保取值成功
fun getSigningValue(keyName: String, map: Map<String, String>): String? {
    // 强制使用 Map 迭代器查找与键名匹配的键，确保绕过索引 bug
    val foundKey = map.keys.firstOrNull { it == keyName }

    // 如果找到键，则返回 Map 中的值
    return foundKey?.let { map[it] }
}

val keystorePassword: String? = getSigningValue("storePassword", signingPropertiesMap)
val keyAlias: String? = getSigningValue("keyAlias", signingPropertiesMap)
val keyPassword: String? = getSigningValue("keyPassword", signingPropertiesMap)

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.05"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 调试输出
            println("--- DEBUG SIGNING CONFIGS (最终验证 - 强制查找) ---")
            println("keystoreFile: [${keystoreFile}]")
            println("keystorePassword (storePassword): [${keystorePassword}]")
            println("keyAlias: [${keyAlias}]")
            println("keyPassword: [${keyPassword}]")
            println("----------------------------------------------")

            storeFile = file(keystoreFile) // 直接使用固定的文件名

            storePassword = keystorePassword
                ?: throw IllegalStateException("Gradle 属性 'storePassword' 缺失。请检查 keystore.properties。")

            keyAlias = keyAlias
                ?: throw IllegalStateException("Gradle 属性 'keyAlias' 缺失。请检查 keystore.properties。")

            keyPassword = keyPassword
                ?: throw IllegalStateException("Gradle 属性 'keyPassword' 缺失。请检查 keystore.properties。")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val isCiBuild = System.getenv("GITHUB_ACTIONS") == "true"
            if (isCiBuild && keystorePassword != null && keyAlias != null && keyPassword != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }
}

afterEvaluate {
    tasks.named("assembleRelease") {
        dependsOn("licenseReleaseReport")
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