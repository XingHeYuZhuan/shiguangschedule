package com.xingheyuzhuan.shiguangschedule.tool

import android.util.Log
import org.apache.poi.ss.usermodel.*
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.CourseTableImportModel
import java.io.InputStream

object ExcelParserTool {
    private const val TAG = "ExcelParser"

    /**
     * 解析二维课表 Excel
     * 逻辑：
     * 1. 找到"星期"行 → 确定星期一到星期日的列索引
     * 2. 找到"节次"列 → 确定各节次对应的行索引
     * 3. 遍历行列交叉点，提取每个单元格内的课程
     */
    fun parse(inputStream: InputStream): CourseTableImportModel {
        val courses = mutableListOf<ImportCourseJsonModel>()

        WorkbookFactory.create(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            Log.i(TAG, "开始解析Excel，sheet名: ${sheet.sheetName}, 行数: ${sheet.lastRowNum + 1}")

            // ---------- 第一步：定位星期行 ----------
            val weekRowIndex = findWeekRow(sheet) ?: throw IllegalArgumentException("未找到包含'星期'的表头行")
            val weekRow = sheet.getRow(weekRowIndex)
            val weekColumnMap = mapWeekColumns(weekRow)
            Log.i(TAG, "星期行: 第${weekRowIndex + 1}行, 映射列: $weekColumnMap")

            // ---------- 第二步：定位节次列（在星期行之后查找） ----------
            val periodColumnIndex = findPeriodColumn(sheet, weekRowIndex + 1) ?: throw IllegalArgumentException("未找到包含'节'的列")
            val periodRowMap = mapPeriodRows(sheet, periodColumnIndex, weekRowIndex + 1)
            Log.i(TAG, "节次列: 第${periodColumnIndex + 1}列, 映射行: $periodRowMap")

            // ---------- 第三步：遍历矩阵提取课程 ----------
            for ((dayOfWeek, colIndex) in weekColumnMap) {
                for ((periodName, rowIndex) in periodRowMap) {
                    val cell = sheet.getRow(rowIndex)?.getCell(colIndex)
                    val cellContent = getCellContent(cell)
                    if (cellContent.isBlank()) continue

                    // 一个单元格内可能有多门课（用 ----- 分隔）
                    val courseBlocks = cellContent.split("---------------------")
                    for (block in courseBlocks) {
                        val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        if (lines.isEmpty()) continue

                        val course = parseCourseBlock(lines, dayOfWeek, periodName, rowIndex + 1, colIndex + 1)
                        if (course != null) {
                            courses.add(course)
                            Log.d(TAG, "解析到课程: ${course.name}, 周${course.day} 第${course.startSection}-${course.endSection}节")
                        }
                    }
                }
            }
        }

        Log.i(TAG, "解析完成，共 ${courses.size} 门课程")
        courses.forEachIndexed { i, c ->
            Log.i(TAG, "  [${i + 1}] ${c.name} | 教师:${c.teacher} | 周${c.day} | 第${c.startSection}-${c.endSection}节 | 地点:${c.position} | 周数:${c.weeks}")
        }
        return CourseTableImportModel(courses, null, null)
    }

    // ======================= 定位函数 =======================

    /**
     * 查找包含"星期"的行（如"星期一 星期二 ..."）
     */
    private fun findWeekRow(sheet: Sheet): Int? {
        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            for (cell in row) {
                val content = getCellContent(cell)
                if (content.contains("星期一") || content.contains("周二") || content.contains("星期")) {
                    return i
                }
            }
        }
        return null
    }

    /**
     * 映射星期列：遍历星期行，找出"星期一"到"星期日"对应的列索引
     * 返回 Map<星期数字(1-7), 列索引>
     */
    private fun mapWeekColumns(weekRow: Row): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val weekKeywords = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

        for (cell in weekRow) {
            val content = getCellContent(cell)
            for ((index, keyword) in weekKeywords.withIndex()) {
                if (content.contains(keyword)) {
                    map[index + 1] = cell.columnIndex
                    break
                }
            }
        }
        return map
    }

    /**
     * 查找包含"节"的列（A列通常有"第一二节"、"第三四节"等）
     */
    private fun findPeriodColumn(sheet: Sheet, startRow: Int): Int? {
        for (colIdx in 0..10) {
            for (rowIdx in startRow..sheet.lastRowNum) {
                val row = sheet.getRow(rowIdx) ?: continue
                val cell = row.getCell(colIdx) ?: continue
                val content = getCellContent(cell)
                if (content.contains("节") && (content.contains("第") || content.contains("一二") || content.contains("三四"))) {
                    return colIdx
                }
            }
        }
        return null
    }

    /**
     * 映射节次行：在节次列中，找出"第一二节"、"第三四节"等对应的行索引
     * 返回 Map<节次名称, 行索引>
     */
    private fun mapPeriodRows(sheet: Sheet, periodColumnIndex: Int, startRow: Int): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (i in startRow..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(periodColumnIndex) ?: continue
            val content = getCellContent(cell)
            Log.d(TAG, "第${i}行节次列内容: '$content'")
            if (content.contains(Regex("第[一二三四五六七八九十]+节"))) {
                map[content.trim()] = i
            }
        }
        return map
    }

    // ======================= 课程块解析 =======================

    /**
     * 解析一个课程块（4-5行文本）
     * 格式：
     *   第0行: 课程名
     *   第1行: 教师名(职称)
     *   第2行: 周数([周])[节次]
     *   第3行: 地点
     *   第4行: 可能还有额外地点（如多个教室）
     */
    private fun parseCourseBlock(
        lines: List<String>,
        day: Int,
        periodName: String,
        rowNum: Int,
        colNum: Int
    ): ImportCourseJsonModel? {
        if (lines.isEmpty()) return null

        // 提取课程名（第一行）
        val name = lines.getOrNull(0)?.trim() ?: return null
        if (name.isEmpty()) return null

        // 提取教师（第二行）
        val teacherLine = lines.getOrNull(1)?.trim() ?: ""
        val teacher = teacherLine.replace(Regex("\\(.*\\)"), "").trim() // 去掉职称括号

        // 提取周数和节次（第三行）
        val weekLine = lines.getOrNull(2)?.trim() ?: ""
        val weeks = parseWeeksFromLine(weekLine)

        // 提取地点（第四行及以后，可能多个地点）
        val positions = mutableListOf<String>()
        for (i in 3 until lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty() && !line.contains("周") && !line.contains("节")) {
                positions.add(line)
            }
        }
        val position = positions.joinToString(";")

        // 解析节次名称获取 start/end
        val (startSection, endSection) = parsePeriodName(periodName)

        return ImportCourseJsonModel(
            name = name,
            teacher = teacher,
            position = position,
            day = day,
            startSection = startSection,
            endSection = endSection,
            weeks = weeks
        )
    }

    /**
     * 从 "1-2,4,6,8,12,14,16([周])[01-02节]" 中提取周数列表
     */
    private fun parseWeeksFromLine(line: String): List<Int> {
        // 取 ([周]) 之前的部分，或 [ 之前的部分
        val weekPart = line.substringBefore("([周])").substringBefore(" [周]").trim()
        if (weekPart.isEmpty()) return emptyList()

        val result = mutableListOf<Int>()
        weekPart.split(",").forEach { part ->
            if (part.contains("-")) {
                val range = part.split("-").mapNotNull { it.toIntOrNull() }
                if (range.size == 2) {
                    result.addAll(range[0]..range[1])
                }
            } else {
                part.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result.distinct().sorted()
    }

    /**
     * 解析节次名称："第一二节" → (1, 2), "第三四节" → (3, 4)
     */
    private fun parsePeriodName(periodName: String): Pair<Int, Int> {
        val numMap = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4,
            "五" to 5, "六" to 6, "七" to 7, "八" to 8,
            "九" to 9, "十" to 10, "十一" to 11, "十二" to 12
        )

        val cleaned = periodName.replace("第", "").replace("节", "")
        return when {
            cleaned.contains("一二") -> 1 to 2
            cleaned.contains("三四") -> 3 to 4
            cleaned.contains("五六") -> 5 to 6
            cleaned.contains("七八") -> 7 to 8
            cleaned.contains("九十") -> 9 to 10
            cleaned.contains("十一十二") -> 11 to 12
            else -> {
                val chars = cleaned.toList().mapNotNull { numMap[it.toString()] }
                if (chars.size >= 2) chars[0] to chars[1]
                else if (chars.size == 1) chars[0] to chars[0]
                else 1 to 2
            }
        }
    }

    // ======================= 辅助工具 =======================

    private fun getCellContent(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            CellType.FORMULA -> {
                try { cell.stringCellValue.trim() } catch (_: Exception) { "" }
            }
            else -> ""
        }
    }
}