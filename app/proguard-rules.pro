# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ProGuard / R8 混淆规则文件
# 该文件解决了所有数据模型（Gson/Protobuf/枚举/Kotlinx.Serialization）和组件（Glance/Worker/WebView）的混淆问题。

# 保留调试信息：源代码文件和行号，用于查看真实的堆栈信息。
-keepattributes SourceFile,LineNumberTable

# 1. 核心 WebView JavaScript 接口保留 (AndroidBridge)

# 保留 AndroidBridge 类本身（防止整个类被移除）
-keep class com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidBridge { *; }

# 保留 AndroidBridge 类中所有带有 @JavascriptInterface 注解的 public 方法。
-keepclassmembers class com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidBridge {
    @android.webkit.JavascriptInterface public *;
}

# 2. 忽略 JGit 依赖中的警告和保留 JGit 类

-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-keep class org.eclipse.jgit.** { *; }
-keep interface org.eclipse.jgit.** { *; }

# 3. 保留 Glance 小组件相关的核心类和生命周期方法

-keep public class androidx.glance.appwidget.GlanceAppWidgetReceiver {
    <init>(...);
}
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidget {
    @androidx.glance.appwidget.GlanceComposable public void Content();
}
# 保留所有 App Widget 的构造函数
-keep public class com.xingheyuzhuan.shiguangschedule.widget.** {
    <init>(...);
}
# 保留所有 App Widget Provider 的生命周期方法
-keepclassmembers class com.xingheyuzhuan.shiguangschedule.widget.**GlanceAppWidget {
    public void onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[]);
    public void onEnabled(android.content.Context);
    public void onDisabled(android.content.Context);
    public void onAppWidgetOptionsChanged(android.content.Context, android.appwidget.AppWidgetManager, int, android.os.Bundle);
}

# 4. 保留 WorkManager Worker 类和应用/同步辅助类

# Worker 类需要保留构造函数
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetUiUpdateWorker {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.FullDataSyncWorker {
    <init>(...);
}
# 其他工具类和应用入口
-keep public class com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.MyApplication { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetUpdateHelperKt { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetDataKt { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper { *; }

# 5. 核心数据模型保留 (通用 Gson/Room/枚举规则)

# 保留 data.model 包下的所有类（School, ContributionList 及其嵌套类）
-keep class com.xingheyuzhuan.shiguangschedule.data.model.** { *; }

# 保留 data.model 包下的所有枚举类及其常量（如 SchoolCategory）
-keep enum com.xingheyuzhuan.shiguangschedule.data.model.** {
    *;
}

# 确保所有带有 @SerializedName 注解的字段名称不被混淆。
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# 保留数据仓库类和 Room 数据库模型
-keep class com.xingheyuzhuan.shiguangschedule.data.repository.SchoolRepository { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.data.db.widget.** { *; }

# 6. Protobuf Lite 数据模型保留 (解决 NoSuchFieldException 错误)
# 确保所有 Protobuf Lite 生成的类和字段不被混淆或移除。

# 1. 保留所有继承自 GeneratedMessageLite 的类及其成员
-keep class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# 2. 保留所有继承自 GeneratedMessageLite$Builder 的类及其成员
-keep class * extends com.google.protobuf.GeneratedMessageLite$Builder {
    <fields>;
    <methods>;
}

# 3. 保留实现 Internal$EnumLite 的枚举类
-keep enum * implements com.google.protobuf.Internal$EnumLite {
    <fields>;
    <methods>;
}

# 4. 保护 Protobuf 运行时可能使用反射访问的内部类 (如 k9.**)
-keep class k9.** { *; }

# 7. Kotlin Serialization 数据模型保留
# 保护使用 @Serializable 注解的数据类 (如 AndroidBridge.kt 中的 TimeSlotJsonModel)


# 保护 Kotlin Metadata，确保反射功能正常
-keep class kotlin.Metadata { *; }

# 保护 Kotlin Serialization 运行时类
-keep class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }

# 保护所有自动生成的序列化器类（以 $$serializer 结尾）
-keep class **$$serializer { *; }