# Excel 导入功能说明

## 功能概述

在课表转换页面添加了 Excel 文件导入入口，支持选择 `.xlsx` 文件并解析其中的课程数据，导入到指定课表中。

## 操作入口

- 位置：设置 → 课表转换 → **从 Excel 文件导入**
- 选择文件后自动解析并导入到选中的课表

## 代码结构

### 1. UI 层

| 文件 | 作用 |
|------|------|
| `ui/settings/conversion/CourseTableConversionScreen.kt` | 添加 Excel 导入行和文件选择器 `importExcelLauncher` |
| `ui/settings/conversion/CourseTableConversionDialogs.kt` | 定义 `OpenExcelDocumentContract`，处理 `.xlsx` 文件选择 |
| `ui/settings/conversion/CourseTableConversionViewModel.kt` | 处理 Excel 导入业务逻辑（`handleExcelImport`） |

### 2. 解析层

| 文件 | 作用 |
|------|------|
| `tool/ExcelToJsonConverter.kt` | 解析 Excel 文件，转化为 `CourseTableImportModel` |

### 3. 数据层

| 文件 | 作用 |
|------|------|
| `data/repository/CourseImportExport.kt` | 定义导入导出相关的数据模型 |
| `data/repository/CourseConversionRepository.kt` | `importCourseTableFromJson()` 执行实际入库 |

## 数据流

```
用户点击"从 Excel 文件导入"
  → 选择课表弹窗
    → 系统文件选择器（过滤 .xlsx）
      → ContentResolver 打开 InputStream
        → ExcelToJsonConverter.convert(InputStream)
          → parseMatrixScheduleSheet() 逐行列解析
            → CourseTableImportModel
              → CourseConversionRepository.importCourseTableFromJson()
                → 清空原课程 → 写入新课程 + 周次
```

## Excel 文件格式要求

解析器按以下格式读取：

### 表头（第3行）
- 第1列：节次信息
- 第2~8列：星期一到星期日

### 数据行（第4行起）
- 第1列：节次范围，格式如 `(01,02)` 或 `第一二节`
- 后续列：课程单元格

### 课程单元格内容格式
每个单元格可包含多门课，用 `---------------------` 分隔。每门课至少包含3行：

```
课程名称
教师名(职称)
周次([周])[节次]
上课地点
```

示例：
```
软件工程与UML
金德(副教授)
1-3,5,7,9,11([周])[01-02节]
15102
```

### 周次格式支持
- `1-16([周])[03-04节]`
- `1-6([周])[01-02节]`
- `5([周])[07-08节]`
- `7-12`
- `1,2,3,5,7,9,11`

## 依赖

- Apache POI：用于读取 `.xlsx` 文件
- kotlinx.serialization：用于数据模型序列化
