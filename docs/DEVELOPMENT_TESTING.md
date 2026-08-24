# 拾光课程表：开发与测试流程

本文给出当前仓库可执行的开发流程、测试现状、建议的验证分层和发布路径。命令以仓库根目录为工作目录；Windows 使用 `gradlew.bat`，macOS/Linux 把它替换为 `./gradlew`。

## 1. 当前可交付范围

- 首选开发与验收目标：Android。
- Desktop/iOS：存在工程和部分平台实现，但仍是适配中目标；当前入口未看到 Koin 初始化，不能作为开箱即用的验收平台。
- 自动化测试现状：仓库暂未配置 Kotlin 测试源文件；本分支新增了 WebView 跨域请求的 Node 回归测试。
- CI 现状：Android Build 工作流只在手动触发时构建签名 Release APK；没有自动执行单元测试、设备测试或静态检查。

因此，“Gradle 构建通过”当前只能证明编译和打包，不等于业务行为已有回归覆盖。

## 2. 环境准备

### 必需工具

1. Git。
2. JDK 21。项目的 Kotlin JVM Toolchain 和 Java 编译目标均指定 21，CI 也使用 Temurin 21。
3. Android Studio 与 Android SDK：
   - compileSdk/targetSdk 37
   - minSdk 26
   - 对应 SDK Platform、Build Tools、Platform Tools
4. Node.js 20 或更高版本，用于离线索引生成和 JavaScript 适配器回归测试。
5. 可访问 Google Maven、Maven Central、Gradle Plugin Portal 和 Gradle Distribution 的网络。

仅构建 iOS 时还需要 macOS、Xcode 和可用的 iOS Simulator；Windows/Linux 无法完成 Xcode App 构建。

版本不要凭文档手工猜测，以 [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) 和 [`gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties) 为准。当前 Wrapper 固定为 Gradle 9.7.1。

### 首次同步

```powershell
git status --short
.\gradlew.bat --version
.\gradlew.bat projects
.\gradlew.bat tasks --all
```

打开 Android Studio 后，让 IDE 生成本机 `local.properties`/SDK 路径，不要提交本机路径、签名文件或密钥。

首次构建会下载 Gradle 与 Maven 依赖，并执行 Koin、Room、Wire、Compose Resources 和开源许可等代码/资源生成任务，因此耗时会明显长于增量构建。

## 3. 资源与生成代码

共享模块的构建包含几条容易忽略的生成链：

- `packSchoolsZip`：把 `shared/assets/offline_repo` 打包为 `offline_schools.zip`。
- `exportLibraryDefinitions`：生成 Compose 可读取的第三方许可数据。
- Wire：从 `shared/src/commonMain/proto` 生成学校索引、样式和 Widget 快照类型。
- Room KSP：为 Android/JVM/iOS 目标生成数据库实现，并把 schema 导出到 `shared/schemas`。
- Koin 编译器：根据 `@Single`、`@KoinViewModel`、`@KoinWorker` 等注解生成模块配置。

可单独验证内置资源打包：

```powershell
node tools/offline-repo/build-school-index.mjs
.\gradlew.bat :shared:packSchoolsZip
.\gradlew.bat :shared:exportLibraryDefinitions
```

不要直接编辑 `build/generated` 下的文件。修改 `.proto`、Room Entity 或 Koin 注解后，应重新运行 Gradle 并检查生成结果与 schema 变更。

## 4. 日常开发流程

### 4.1 开始前

```powershell
git status --short
git branch --show-current
```

保留工作区内不属于本次任务的改动。项目约定向 `main` 的 PR 必须来自本仓库 `dev` 分支；普通贡献应先进入 `dev`。

### 4.2 按影响面修改

1. UI 改动：优先在 `shared/src/commonMain/.../ui` 修改 Screen 与 ViewModel。
2. 业务改动：放在 Repository/ViewModel；Screen 只渲染状态并发送事件。
3. 数据库改动：同时修改 Entity、DAO、Database 版本/迁移和 `shared/schemas`。
4. 设置或样式改动：检查 DataStore 默认值、序列化兼容和备份恢复。
5. 导入模型改动：同步检查 JSON 模型、Web Bridge、校验、导出与旧备份兼容。
6. Android 后台改动：同时检查 Worker/Receiver、Manifest、权限、唯一任务名和系统版本分支。
7. 多平台改动：共享接口或 `expect` 的每个启用目标都必须有对应实现。

### 4.3 快速编译反馈

Android Debug APK：

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

安装到已连接设备：

```powershell
adb devices
.\gradlew.bat :androidApp:installDebug
```

共享 JVM 编译可用于较快发现 common/JVM 源集错误：

```powershell
.\gradlew.bat :shared:compileKotlinJvm
```

Desktop 存在标准 Compose 入口，通常对应以下任务；但在补齐 Desktop Koin 启动前，只建议把它用于适配工作，不作为当前项目的成功基线：

```powershell
.\gradlew.bat :desktopApp:run
```

iOS 共享 Framework 只能在 macOS 验证：

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

随后用 Xcode 打开 `iosApp/iosApp.xcodeproj` 构建宿主应用。当前还需先补齐 iOS 端依赖注入启动，才能进行完整运行验收。

## 5. 自动化测试

### 5.1 仓库现状

构建脚本已经声明：

- `commonTest` 使用 `kotlin.test`。
- Android App 本地测试使用 JUnit 4。
- Android 设备测试使用 AndroidX JUnit 与 Espresso。

`commonTest` 已配置 `kotlin.test` 依赖，但目前还没有 Kotlin 测试源文件；继续补充时使用以下目录：

```text
shared/src/commonTest/kotlin/
shared/src/jvmTest/kotlin/              # 仅在确有 JVM 专属测试时
androidApp/src/test/kotlin/
androidApp/src/androidTest/kotlin/
```

当前可直接运行的 JavaScript 回归测试：

```powershell
node tools/webview-request-interceptor-test/test-cross-origin.mjs
node tools/seu-adapter-test/test-parser.mjs
```

### 5.2 建议优先补齐的共享单元测试

优先测试纯计算与边界，而不是从 Compose 截图开始：

1. 周次计算：周一/周日起始、开学前、学期末、跨年、无效日期。
2. 课程区间合并：不重叠、相邻、部分重叠、包含关系、多列冲突、自定义时间和 24 小时模式。
3. Widget 展开：未来 7 天、跳过日期、标准节次/自定义时间、学期边界。
4. 导入校验：非法时间、节次倒置、重复周次、颜色越界、缺失配置。
5. 序列化兼容：旧 JSON/备份、Proto 默认值、未知字段。
6. ICS 生成：重复周、时区、提醒、特殊字符转义。

目前部分算法是 ViewModel 私有函数。为了低成本测试，宜把周次计算、课程布局和 Widget 展开抽成无平台依赖的纯函数，再由 ViewModel/同步器调用。

共享测试优先通过 Android Host Test 运行，因为当前 JVM Desktop 源集仍缺少若干 `actual` 实现：

```powershell
.\gradlew.bat :shared:testAndroidHostTest
```

### 5.3 Android 测试

添加 `androidApp/src/test` 测试后：

```powershell
.\gradlew.bat :androidApp:testDebugUnitTest
```

添加 `androidApp/src/androidTest` 测试并连接 API 26+ 设备/模拟器后：

```powershell
adb devices
.\gradlew.bat :androidApp:connectedDebugAndroidTest
```

设备测试重点应覆盖：Room 迁移、权限分支、Worker、Alarm/Receiver Intent、Widget RemoteViews 和关键 Compose 导航。精确闹钟、通知策略、系统日历和小组件在不同系统厂商上行为差异较大，不能只依赖 JVM 测试。

如果 Gradle 版本升级后任务名发生变化，先运行：

```powershell
.\gradlew.bat :shared:tasks --all
.\gradlew.bat :androidApp:tasks --all
```

## 6. 提交前验证阶梯

按改动风险逐级执行，前一级失败时先修复再继续：

### A. 文档或静态资源改动

```powershell
.\gradlew.bat :shared:packSchoolsZip
git diff --check
```

### B. 共享 UI/业务改动

```powershell
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
```

`:shared:jvmTest` 当前会因 JVM Desktop 源集缺少部分 `actual` 声明而失败；补齐平台实现后再把它加入验证基线。

### C. 数据库、生成代码或平台能力改动

```powershell
.\gradlew.bat clean
.\gradlew.bat :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

另外检查：

```powershell
git status --short
git diff -- shared/schemas
git diff --check
```

`clean` 会显著增加耗时，只在生成代码、缓存或跨源集问题需要时使用，不必每次开发都运行。

### D. 合并/发布候选

```powershell
.\gradlew.bat check
.\gradlew.bat :androidApp:assembleRelease
```

本地 `assembleRelease` 默认使用 debug signing，适合验证 R8/资源压缩；官方 CI 会通过注入参数改用 Release Keystore。

## 7. 手工验收清单

当前自动化覆盖仍然有限，至少完成以下 Android 冒烟测试：

### 基础与数据

- 首次启动能解压离线学校索引，能进入学校/适配器选择页。
- 新建、切换、重命名和删除课表后，课程、时间段与配置归属正确。
- 周一/周日作为每周首日时，当前周与日期标题正确。
- 学期开始前、学期结束后、跳过日期显示正确。
- 标准节次、自定义时间、冲突课程和 24 小时模式布局正确。

### 导入、导出与恢复

- 教务脚本可提交课程、配置和时间段，失败时 Promise/提示可返回错误。
- 单课表 JSON 导入导出往返后数据等价。
- 全量备份与 WebDAV 上传/下载/恢复可用。
- ICS 能被至少一个目标日历正确导入，时间和重复周正确。

### Android 平台能力

- 四种小组件在浅色/深色下都能刷新，跨天与“明日预告”正确。
- 修改课程、时间、样式和当前课表后，小组件能更新。
- 通知关闭时旧闹钟被取消；开启时提醒时间正确。
- 精确闹钟/通知/勿扰权限拒绝与授予两条路径均不崩溃。
- 自动模式能在上课/下课边界切换，并忽略跳过日期。
- 日历权限拒绝、授权和同步后的重复执行行为可接受。
- 重启设备或应用进程被回收后，后台任务仍能恢复到合理状态。

建议至少用 API 26、API 31/32、API 33+ 和当前 targetSdk 对应版本各验证一次，因为精确闹钟和通知权限边界不同。

## 8. CI 与发布流程

### Android Build

`.github/workflows/android-build.yml` 是手动工作流：

1. 清理仓库内置教务资源目录。
2. 从 `shiguang_warehouse` 的 `index-pb-release` 获取 `school_index.pb`。
3. 稀疏拉取 `main/resources`，并移除 `adapters.yaml`。
4. 生成贡献者数据。
5. 配置 JDK 21 和 Gradle 缓存。
6. 解码 Release Keystore，通过 Gradle 注入签名参数运行根任务 `assembleRelease`。
7. 上传 `androidApp/build/outputs/apk/release/*.apk`，保留 7 天。

该工作流使用 ABI Split，只输出 `armeabi-v7a`、`arm64-v8a` 和 `x86_64` APK，不生成 universal APK。

### Android Release

`.github/workflows/android-release.yml` 接收成功 Build Run ID、版本号和 prerelease 标记：

1. 检查 tag 尚不存在。
2. 验证源 Run 成功且存在 `app-release-apk` Artifact。
3. 进入 `Production-Release` Environment 等待审核。
4. 生成 changelog、下载并解压 APK。
5. 创建 Draft GitHub Release。

版本发布前还应确认 `androidApp/build.gradle.kts` 中的 `versionCode`/`versionName` 与 fastlane changelog 一致；工作流输入不会自动改写应用版本。

## 9. 当前流程的主要缺口

建议按优先级补齐：

1. 继续为周次、布局、导入校验和 Widget 展开扩充 `commonTest` 基线。
2. 新增普通 PR CI，至少运行 `:shared:testAndroidHostTest`、Android 单元测试和 `assembleDebug`。
3. 在 Release Build 中先运行测试，再进行签名打包。
4. 增加 Room migration instrumentation test，覆盖已有 schema 3→4→5。
5. 明确 Desktop/iOS 支持策略；若继续支持，为其补齐 Koin 启动和最小冒烟构建。
6. 把容易测试的日期/布局算法从 ViewModel 私有实现抽成纯函数。

## 10. 本次文档验证记录

文档编写时已完成以下静态核对：

- 检查全部模块、构建脚本、版本目录、CI、Manifest、入口、共享数据层和 Android 后台链。
- 确认本机使用 JDK 21，符合项目 Toolchain 要求。
- `node tools/webview-request-interceptor-test/test-cross-origin.mjs` 执行通过。
- `node tools/seu-adapter-test/test-parser.mjs` 执行通过。
- `:shared:testAndroidHostTest` 任务执行完成；当前为 `NO-SOURCE`，没有实际运行 Kotlin 测试用例。
- `:androidApp:testDebugUnitTest` 任务执行完成；当前同样为 `NO-SOURCE`。
- `:androidApp:assembleDebug` 执行通过，并成功生成三个 ABI 的 Debug APK。

上述验证只能覆盖编译、共享测试任务和 WebView 请求拦截回归；账号登录、学校页面变化及 Android WebView 行为仍需通过真实环境验收。

项目原理见 [项目原理与架构](PROJECT_ARCHITECTURE.md)。
