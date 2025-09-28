# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-keep class org.eclipse.jgit.** { *; }
-keep interface org.eclipse.jgit.** { *; }


# -----------------
# 1. 保留核心小组件类
# -----------------
-keep public class androidx.glance.appwidget.GlanceAppWidgetReceiver {
    <init>(...);
}
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidget {
    @androidx.glance.appwidget.GlanceComposable public void Content();
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysScheduleWidget {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.compact.CompactScheduleWidget {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.single_day.SingleDayAppWidget {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.week.WeekAppWidget {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.moderate.ModerateScheduleWidget {
    <init>(...);
}


# -----------------
# 2. 保留数据同步相关的 Worker 类
# -----------------
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetUiUpdateWorker {
    <init>(...);
}
-keep public class com.xingheyuzhuan.shiguangschedule.widget.FullDataSyncWorker {
    <init>(...);
}

# -----------------
# 3. 保留数据仓库和数据模型
# -----------------
-keep public class com.xingheyuzhuan.shiguangschedule.data.db.widget.** { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.MyApplication { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetUpdateHelperKt { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WidgetDataKt { *; }
-keep public class com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper { *; }

# -----------------
# 4. 保留所有 App Widget Provider 的生命周期方法
# -----------------
-keepclassmembers class com.xingheyuzhuan.shiguangschedule.widget.**GlanceAppWidget {
    public void onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[]);
    public void onEnabled(android.content.Context);
    public void onDisabled(android.content.Context);
    public void onAppWidgetOptionsChanged(android.content.Context, android.appwidget.AppWidgetManager, int, android.os.Bundle);
}
