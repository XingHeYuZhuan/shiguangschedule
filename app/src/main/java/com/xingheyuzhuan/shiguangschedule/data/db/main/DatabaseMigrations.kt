package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库版本 1 迁移到 版本 2 的迁移代码。
 * 核心任务：
 * 1. 创建 course_table_config 表。
 * 2. 将老版本 app_settings 中的全局设置数据迁移到新表中。
 * 3. 删除 app_settings 表中已迁移的字段。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // --- 步骤 1: 创建新的 course_table_config 表 ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `course_table_config` (
                `courseTableId` TEXT NOT NULL,
                `showWeekends` INTEGER NOT NULL DEFAULT 0,
                `semesterStartDate` TEXT,
                `semesterTotalWeeks` INTEGER NOT NULL DEFAULT 20,
                `defaultClassDuration` INTEGER NOT NULL DEFAULT 45,
                `defaultBreakDuration` INTEGER NOT NULL DEFAULT 10,
                `firstDayOfWeek` INTEGER NOT NULL DEFAULT 1, 
                PRIMARY KEY(`courseTableId`),
                FOREIGN KEY(`courseTableId`) REFERENCES `course_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
        )
        // 添加索引
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_course_table_config_courseTableId` ON `course_table_config` (`courseTableId`)")


        // --- 步骤 2 & 3: 读取老数据并插入新表 ---

        // 1. 从老版本 app_settings 表中读取配置和当前课表 ID
        // 这里使用简单的 SQLITE 查询，而不是 Room 的对象，因为表结构在迁移过程中是动态变化的。
        val cursor = db.query("SELECT currentCourseTableId, showWeekends, semesterStartDate, semesterTotalWeeks, defaultClassDuration, defaultBreakDuration FROM app_settings WHERE id = 1 LIMIT 1")

        var currentCourseTableId: String? = null
        var showWeekends = 0
        var semesterStartDate: String? = null
        var semesterTotalWeeks = 20
        var defaultClassDuration = 45
        var defaultBreakDuration = 10

        if (cursor.moveToFirst()) {
            currentCourseTableId = cursor.getString(0)
            showWeekends = cursor.getInt(1)
            if (!cursor.isNull(2)) semesterStartDate = cursor.getString(2) else semesterStartDate = null
            semesterTotalWeeks = cursor.getInt(3)
            defaultClassDuration = cursor.getInt(4)
            defaultBreakDuration = cursor.getInt(5)
        }
        cursor.close()

        // 2. 将读取到的老数据插入到新的 course_table_config 表中
        if (currentCourseTableId != null) {
            db.execSQL(
                """
                INSERT INTO `course_table_config` (courseTableId, showWeekends, semesterStartDate, semesterTotalWeeks, defaultClassDuration, defaultBreakDuration, firstDayOfWeek)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    currentCourseTableId,
                    showWeekends,
                    semesterStartDate,
                    semesterTotalWeeks,
                    defaultClassDuration,
                    defaultBreakDuration,
                    1
                )
            )
        }

        // --- 步骤 4: 更新 app_settings 表结构 (删除字段) ---
        // 1. 重命名旧表
        db.execSQL("ALTER TABLE app_settings RENAME TO app_settings_old")

        // 2. 创建新的 app_settings 表 (精简后的结构)
        db.execSQL(
            """
            CREATE TABLE `app_settings` (
                `id` INTEGER NOT NULL,
                `currentCourseTableId` TEXT,
                `reminderEnabled` INTEGER NOT NULL DEFAULT 0,
                `remindBeforeMinutes` INTEGER NOT NULL DEFAULT 15,
                `skippedDates` TEXT,
                `autoModeEnabled` INTEGER NOT NULL DEFAULT 0,
                `autoControlMode` TEXT NOT NULL DEFAULT 'DND',
                PRIMARY KEY(`id`)
            )
            """
        )

        // 3. 将旧表中保留的字段数据复制到新表
        db.execSQL(
            """
            INSERT INTO app_settings (id, currentCourseTableId, reminderEnabled, remindBeforeMinutes, skippedDates, autoModeEnabled, autoControlMode)
            SELECT id, currentCourseTableId, reminderEnabled, remindBeforeMinutes, skippedDates, 0, 'DND' FROM app_settings_old
            """
        )

        // 4. 删除旧表
        db.execSQL("DROP TABLE app_settings_old")
    }
}

// 【集中管理所有迁移对象】
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    // 未来在这里添加 MIGRATION_2_3, MIGRATION_3_4 等...
)