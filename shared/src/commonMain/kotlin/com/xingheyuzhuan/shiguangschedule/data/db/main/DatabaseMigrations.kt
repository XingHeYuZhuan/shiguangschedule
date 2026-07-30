package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库版本 1 迁移到 版本 2 的迁移代码。
 * 核心任务：
 * 1. 创建 course_table_config 表。
 * 2. 将老版本 app_settings 中的全局设置数据迁移到新表中。
 * 3. 删除 app_settings 表中已迁移的字段。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // --- 步骤 1: 创建新的 course_table_config 表 ---
        connection.execSQL(
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
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_course_table_config_courseTableId` ON `course_table_config` (`courseTableId`)")

        // --- 步骤 2 & 3: 读取老数据并插入新表 ---

        var currentCourseTableId: String? = null
        var showWeekends = 0
        var semesterStartDate: String? = null
        var semesterTotalWeeks = 20
        var defaultClassDuration = 45
        var defaultBreakDuration = 10

        // 1. 读取旧数据
        connection.prepare(
            "SELECT currentCourseTableId, showWeekends, semesterStartDate, semesterTotalWeeks, defaultClassDuration, defaultBreakDuration FROM app_settings WHERE id = 1 LIMIT 1"
        ).use { stmt ->
            if (stmt.step()) {
                currentCourseTableId = if (!stmt.isNull(0)) stmt.getText(0) else null
                showWeekends = stmt.getLong(1).toInt()
                semesterStartDate = if (!stmt.isNull(2)) stmt.getText(2) else null
                semesterTotalWeeks = stmt.getLong(3).toInt()
                defaultClassDuration = stmt.getLong(4).toInt()
                defaultBreakDuration = stmt.getLong(5).toInt()
            }
        }

        // 2. 将数据插入新表
        if (currentCourseTableId != null) {
            connection.prepare(
                """
                INSERT INTO `course_table_config` (courseTableId, showWeekends, semesterStartDate, semesterTotalWeeks, defaultClassDuration, defaultBreakDuration, firstDayOfWeek)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """
            ).use { stmt ->
                stmt.bindText(1, currentCourseTableId)
                stmt.bindLong(2, showWeekends.toLong())
                if (semesterStartDate != null) {
                    stmt.bindText(3, semesterStartDate)
                } else {
                    stmt.bindNull(3)
                }
                stmt.bindLong(4, semesterTotalWeeks.toLong())
                stmt.bindLong(5, defaultClassDuration.toLong())
                stmt.bindLong(6, defaultBreakDuration.toLong())
                stmt.bindLong(7, 1L)
                stmt.step()
            }
        }

        // --- 步骤 4: 更新 app_settings 表结构 (删除字段) ---
        // 1. 重命名旧表
        connection.execSQL("ALTER TABLE app_settings RENAME TO app_settings_old")

        // 2. 创建新的 app_settings 表
        connection.execSQL(
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
        connection.execSQL(
            """
            INSERT INTO app_settings (id, currentCourseTableId, reminderEnabled, remindBeforeMinutes, skippedDates, autoModeEnabled, autoControlMode)
            SELECT id, currentCourseTableId, reminderEnabled, remindBeforeMinutes, skippedDates, 0, 'DND' FROM app_settings_old
            """
        )

        // 4. 删除旧表
        connection.execSQL("DROP TABLE app_settings_old")
    }
}

/**
 * 数据库版本 2 迁移到 版本 3 的迁移代码。
 * 修改 courses 表，添加自定义时间字段。
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {

        // 创建具有新结构和约束的临时表 `courses_new`
        connection.execSQL(
            """
            CREATE TABLE `courses_new` (
                `id` TEXT NOT NULL,
                `courseTableId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `teacher` TEXT NOT NULL,
                `position` TEXT NOT NULL,
                `day` INTEGER NOT NULL,
                `startSection` INTEGER,  -- 变为可空 (Int?)
                `endSection` INTEGER,    -- 变为可空 (Int?)
                `isCustomTime` INTEGER NOT NULL DEFAULT 0, -- 新增字段，默认 FALSE
                `customStartTime` TEXT,  -- 新增字段
                `customEndTime` TEXT,    -- 新增字段
                `colorInt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`courseTableId`) REFERENCES `course_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
        )

        // 将原表数据复制到新表
        connection.execSQL(
            """
            INSERT INTO courses_new (id, courseTableId, name, teacher, position, day, startSection, endSection, colorInt, isCustomTime, customStartTime, customEndTime)
            SELECT id, courseTableId, name, teacher, position, day, startSection, endSection, colorInt, 0, NULL, NULL
            FROM courses
            """
        )

        // 移除原表并重命名新表
        connection.execSQL("DROP TABLE courses")
        connection.execSQL("ALTER TABLE courses_new RENAME TO courses")

        // 重新创建必要的索引和外键索引
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_courseTableId` ON `courses` (`courseTableId`)")
    }
}

// 【集中管理所有迁移对象】
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
)