# -------------------------------------------------------------------------
# R8/ProGuard 混淆配置文件
# -------------------------------------------------------------------------

# 基础全局设置 ---
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,AnnotationDefault

# 依赖注入 (Koin) ---
# 保留 Koin 核心类及 DSL 相关
-keep class org.koin.** { *; }

# 保留 Koin Annotations 及其生成的模块 (KSP 路径)
-keep class org.koin.ksp.generated.** { *; }
-keep @org.koin.core.annotation.Module class * { *; }

# 确保 Koin 能够调用被注解类的构造函数进行依赖注入
-keepclassmembers class * {
    @org.koin.core.annotation.Single <init>(...);
    @org.koin.core.annotation.Factory <init>(...);
    @org.koin.core.annotation.KoinViewModel <init>(...);
    @org.koin.core.annotation.Named <init>(...);
}

# 原生组件与 WorkManager
-keep public class * extends android.appwidget.AppWidgetProvider {
    public void *(android.content.Context, android.content.Intent);
    <init>();
}
-keep class com.xingheyuzhuan.shiguangschedule.widget.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 网络库 (Ktor/OkHttp)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-dontwarn io.ktor.**


# 保护 GitUpdater 核心 API 及协议/凭证逻辑
-keepclassmembers class org.eclipse.jgit.api.Git {
    public static *** cloneRepository();
    public static *** lsRemoteRepository();
    public *** fetch();
    public *** reset();
}
-keep class org.eclipse.jgit.transport.TransportHttp { *; }
-keep class org.eclipse.jgit.transport.HttpTransport { *; }
-keep class org.eclipse.jgit.transport.CredentialsProvider { *; }
-keep class org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider { *; }
-keep class org.eclipse.jgit.transport.CredentialItem** { *; }

# 日志与极致优化
-keep class org.slf4j.impl.** { *; }
# 移除 JGit 内部海量字符串计算
-assumenosideeffects class org.eclipse.jgit.internal.JGitText {
    public static *** get();
}
# 移除 Android 系统调试日志 (v/d/i/w)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 数据解析 (Wire Protobuf/Serialization)
-keep class * implements com.squareup.wire.Message {
    <fields>;
    <methods>;
}
-keep class * implements com.squareup.wire.WireEnum { *; }
-keepclassmembers class * implements com.squareup.wire.Message {
    public static *** ADAPTER;
}
-keep class * extends com.squareup.wire.ProtoAdapter { *; }


-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keep @kotlinx.serialization.Serializable class * { ** Companion; }
-keepclassmembers class * { *** write$Self(...); <init>(int, ...); }
-keep class **$$serializer { *; }

#  WebView & JS 交互
-keep class com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidBridge { *; }
-keepclassmembers class com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# 数据模型与数据库 (Room)
-keep class com.xingheyuzhuan.shiguangschedule.data.db.** { *; }
-keep class com.xingheyuzhuan.shiguangschedule.data.model.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }