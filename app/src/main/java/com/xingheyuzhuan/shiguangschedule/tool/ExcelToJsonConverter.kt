package com.xingheyuzhuan.shiguangschedule.tool

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.CourseConfigJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseImportExport.TimeSlotJsonModel
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileInputStream

class ExcelToJsonConverter {

    companion object {
        private const val TAG = "ExcelToJsonConverter"
    }

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val dayMapping = mapOf(
        "星期一" to 1,
        "星期二" to 2,
        "星期三" to 3,
        "星期四" to 4,
        "星期五" to 5,
        "星期六" to 6,
        "星期日" to 7
    )

    fun convert(excelFilePath: String): CourseImportExport.CourseTableImportModel {
        val file = File(excelFilePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Excel file not found: $excelFilePath")
        }

        FileInputStream(file).use { inputStream ->
            return convert(inputStream)
        }
    }

    fun convert(inputStream: java.io.InputStream): CourseImportExport.CourseTableImportModel {
        val workbook = WorkbookFactory.create(inputStream)
        val courseTable = parseWorkbook(workbook)
        workbook.close()
        return courseTable
    }

    fun convertToJson(excelFilePath: String): String {
        val courseTable = convert(excelFilePath)
        return objectMapper.writeValueAsString(courseTable)
    }

    fun convertToJson(inputStream: java.io.InputStream): String {
        val courseTable = convert(inputStream)
        return objectMapper.writeValueAsString(courseTable)
    }

    private fun parseWorkbook(workbook: Workbook): CourseImportExport.CourseTableImportModel {
        val courses = mutableListOf<ImportCourseJsonModel>()
        val timeSlots = generateDefaultTimeSlots()

        Log.d(TAG, "工作簿共有 ${workbook.numberOfSheets} 个Sheet")
        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val sheetName = sheet.sheetName
            val parsed = parseMatrixScheduleSheet(sheet)
            Log.d(TAG, "Sheet[$i]($sheetName): 解析出 ${parsed.size} 门课程")
            courses.addAll(parsed)
        }
        Log.d(TAG, "总共解析出 ${courses.size} 门课程")

        return CourseImportExport.CourseTableImportModel(courses, timeSlots, null)
    }

    private fun parseMatrixScheduleSheet(sheet: Sheet): List<ImportCourseJsonModel> {
        val courses = mutableListOf<ImportCourseJsonModel>()
        val sheetName = sheet.sheetName

        if (sheet.lastRowNum < 3) {
            Log.w(TAG, "Sheet[$sheetName] 行数不足 (lastRowNum=${sheet.lastRowNum})，跳过")
            return courses
        }

        val dayHeaderRow = sheet.getRow(2) ?: return courses
        Log.d(TAG, "Sheet[$sheetName] dayHeaderRow lastCellNum=${dayHeaderRow.lastCellNum}")

        val dayColumnMap = mutableMapOf<Int, Int>()
        for (colIndex in 1 until dayHeaderRow.lastCellNum) {
            val cellValue = getCellStringValue(dayHeaderRow.getCell(colIndex))
            Log.d(TAG, "Sheet[$sheetName] 第3行 列$colIndex 值='$cellValue'")
            if (dayMapping.containsKey(cellValue)) {
                dayColumnMap[colIndex] = dayMapping[cellValue]!!
            }
        }
        Log.d(TAG, "Sheet[$sheetName] 识别到的星期列映射: $dayColumnMap")

        if (dayColumnMap.isEmpty()) {
            Log.w(TAG, "Sheet[$sheetName] 未识别到任何星期列标题（需要匹配：星期一~星期日）")
        }

        for (rowIndex in 3..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val firstCellValue = getCellStringValue(row.getCell(0))
            if (firstCellValue.isBlank()) {
                Log.d(TAG, "Sheet[$sheetName] 第${rowIndex+1}行 第一列为空，跳过")
                continue
            }

            val sectionRange = parseSectionRange(firstCellValue)
            if (sectionRange == null) {
                Log.d(TAG, "Sheet[$sheetName] 第${rowIndex+1}行 节次格式无法解析: '$firstCellValue'")
                continue
            }
            Log.d(TAG, "Sheet[$sheetName] 第${rowIndex+1}行 节次=$firstCellValue -> 范围=${sectionRange}")

            for ((colIndex, day) in dayColumnMap) {
                val cellValue = getCellStringValue(row.getCell(colIndex))
                if (cellValue.isBlank()) {
                    continue
                }

                val cellCourses = parseCellCourses(cellValue, day, sectionRange)
                Log.d(TAG, "Sheet[$sheetName] 第${rowIndex+1}行 第${colIndex+1}列(星期$day): 解析出 ${cellCourses.size} 门课程")
                courses.addAll(cellCourses)
            }
        }

        Log.d(TAG, "Sheet[$sheetName] 共解析出 ${courses.size} 门课程")
        return courses
    }

    private fun parseSectionRange(firstCellValue: String): Pair<Int, Int>? {
        val regex = Regex("\\((\\d+),(\\d+)\\)")
        val match = regex.find(firstCellValue)
        if (match != null) {
            val start = match.groupValues[1].toIntOrNull()
            val end = match.groupValues[2].toIntOrNull()
            if (start != null && end != null) {
                return Pair(start, end)
            }
        }

        val chineseSectionRegex = Regex("第([一二三四五六七八九十]+)节")
        val chineseMatch = chineseSectionRegex.find(firstCellValue)
        if (chineseMatch != null) {
            val chineseNum = chineseMatch.groupValues[1]
            val num = chineseNumToInt(chineseNum)
            if (num > 0) {
                return Pair(num, num)
            }
        }

        return null
    }

    private fun chineseNumToInt(chinese: String): Int {
        val chineseToNum = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
            "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10
        )

        if (chinese.length == 1) {
            return chineseToNum[chinese] ?: 0
        }

        if (chinese.length == 2) {
            if (chinese.startsWith("十")) {
                return 10 + (chineseToNum[chinese[1].toString()] ?: 0)
            } else {
                return (chineseToNum[chinese[0].toString()] ?: 0) * 10 + (chineseToNum[chinese[1].toString()] ?: 0)
            }
        }

        return 0
    }

    private fun parseCellCourses(cellValue: String, day: Int, sectionRange: Pair<Int, Int>): List<ImportCourseJsonModel> {
        val courses = mutableListOf<ImportCourseJsonModel>()
        val courseBlocks = cellValue.split("---------------------")

        for (block in courseBlocks) {
            val course = parseCourseBlock(block.trim(), day, sectionRange)
            if (course != null) {
                courses.add(course)
            }
        }

        return courses
    }

    private val titleKeywords = listOf("讲师", "教授", "副教授", "实验师", "助教", "研究员", "工程师", "教师", "博士", "硕士", "无")

    private fun parseCourseBlock(block: String, day: Int, sectionRange: Pair<Int, Int>): ImportCourseJsonModel? {
        if (block.isBlank()) {
            return null
        }

        val lines = block.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 3) {
            Log.d(TAG, "parseCourseBlock: 行数不足3行 (${lines.size})，内容='${block.take(80)}'")
            return null
        }

        var name = ""
        var teacherLine = ""
        var weekLine = ""
        var position = ""
        var lineIndex = 0

        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            if (teacherLine.isEmpty() && isTeacherLine(line)) {
                teacherLine = line
            } else if (teacherLine.isNotEmpty() && weekLine.isEmpty() && (line.contains("[周]") || line.contains("周"))) {
                weekLine = line
            } else if (weekLine.isNotEmpty() && position.isEmpty()) {
                position = line
            } else if (teacherLine.isEmpty()) {
                if (name.isNotEmpty()) {
                    name += "\n"
                }
                name += line
            }
            lineIndex++
        }

        val teacher = parseTeacher(teacherLine)
        val weeks = parseWeeksFromWeekLine(weekLine)

        if (name.isBlank()) {
            Log.d(TAG, "parseCourseBlock: 课程名称为空")
            return null
        }
        if (weeks.isEmpty()) {
            Log.d(TAG, "parseCourseBlock: 周次解析为空, weekLine='$weekLine'")
            return null
        }

        Log.d(TAG, "parseCourseBlock 成功: name='$name', teacher='$teacher', weeks=$weeks")
        return ImportCourseJsonModel(
            name = name,
            teacher = teacher,
            position = position,
            day = day,
            startSection = sectionRange.first,
            endSection = sectionRange.second,
            weeks = weeks
        )
    }

    private fun isTeacherLine(line: String): Boolean {
        if (!line.contains("(")) {
            return false
        }
        for (keyword in titleKeywords) {
            if (line.contains(keyword)) {
                return true
            }
        }
        return false
    }

    private fun parseTeacher(teacherLine: String): String {
        val regex = Regex("(.+?)\\(")
        val match = regex.find(teacherLine)
        return match?.groupValues?.get(1)?.trim() ?: teacherLine
    }

    private fun parseWeeksFromWeekLine(weekLine: String): List<Int> {
        // 找到 ([周]) 的位置，取它前面的内容作为周次数字部分
        val idx = weekLine.indexOf("([周])")
        if (idx >= 0) {
            val weeksStr = weekLine.substring(0, idx).trim()
            return parseWeeks(weeksStr)
        }
        // 如果没有 ([周])，尝试去掉结尾的 [XX节] 后整体解析
        val cleaned = weekLine.replace(Regex("\\[\\d+(-\\d+)?节\\]"), "").trim()
        return parseWeeks(cleaned)
    }

    private fun parseWeeks(weeksString: String): List<Int> {
        if (weeksString.isBlank() || weeksString == "[]") {
            return emptyList()
        }

        val weeks = mutableListOf<Int>()
        val parts = weeksString.split(Regex("[,，、;；]"))

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val range = trimmed.split("-")
                if (range.size == 2) {
                    val start = range[0].trim().toIntOrNull()
                    val end = range[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        weeks.addAll(start..end)
                    }
                }
            } else {
                val week = trimmed.toIntOrNull()
                if (week != null && week > 0) {
                    weeks.add(week)
                }
            }
        }

        return weeks.distinct().sorted()
    }

    private fun generateDefaultTimeSlots(): List<TimeSlotJsonModel> {
        return listOf(
            TimeSlotJsonModel(1, "08:00", "08:45", "第一节"),
            TimeSlotJsonModel(2, "08:55", "09:40", "第二节"),
            TimeSlotJsonModel(3, "10:00", "10:45", "第三节"),
            TimeSlotJsonModel(4, "10:55", "11:40", "第四节"),
            TimeSlotJsonModel(5, "14:00", "14:45", "第五节"),
            TimeSlotJsonModel(6, "14:55", "15:40", "第六节"),
            TimeSlotJsonModel(7, "16:00", "16:45", "第七节"),
            TimeSlotJsonModel(8, "16:55", "17:40", "第八节"),
            TimeSlotJsonModel(9, "19:00", "19:45", "第九节"),
            TimeSlotJsonModel(10, "19:55", "20:40", "第十节"),
            TimeSlotJsonModel(11, "20:50", "21:35", "第十一节"),
            TimeSlotJsonModel(12, "21:45", "22:30", "第十二节")
        )
    }

    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) {
            return ""
        }

        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                val value = cell.numericCellValue
                if (value == value.toLong().toDouble()) {
                    value.toLong().toString()
                } else {
                    value.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue.trim()
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toString()
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }
}