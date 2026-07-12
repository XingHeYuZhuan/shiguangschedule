# 时光课表 - 导入文件 JSON 结构说明

## 概述

本文档详细描述了时光课表应用支持的导入文件 JSON 数据结构，包括单个课表导入和全盘备份两种格式。

---

## 一、单个课表导入格式

### 1.1 完整结构

```json
{
  "courses": [
    {
      "id": "string (可选)",
      "name": "string (必填)",
      "teacher": "string (必填)",
      "position": "string (必填)",
      "day": "integer (必填)",
      "startSection": "integer (可选)",
      "endSection": "integer (可选)",
      "weeks": [1, 2, 3],
      "isCustomTime": "boolean",
      "customStartTime": "string (可选)",
      "customEndTime": "string (可选)",
      "color": "integer (可选)",
      "remark": "string (可选)"
    }
  ],
  "timeSlots": [
    {
      "number": "integer (必填)",
      "startTime": "string (必填)",
      "endTime": "string (必填)",
      "alias": "string (可选)"
    }
  ],
  "config": {
    "semesterStartDate": "string (可选)",
    "semesterTotalWeeks": "integer",
    "defaultClassDuration": "integer",
    "defaultBreakDuration": "integer",
    "firstDayOfWeek": "integer"
  }
}
```

### 1.2 字段详细说明

#### courses (课程列表)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | String | 否 | 课程唯一标识，导入时可省略，系统自动生成 |
| name | String | 是 | 课程名称 |
| teacher | String | 是 | 教师姓名 |
| position | String | 是 | 上课地点 |
| day | Int | 是 | 星期几，取值范围 1-7（1=周一，7=周日） |
| startSection | Int | 否 | 起始节次 |
| endSection | Int | 否 | 结束节次 |
| weeks | List\<Int\> | 是 | 周次列表，如 [1, 2, 3, 4, 5] 表示第1-5周上课 |
| isCustomTime | Boolean | 否 | 是否使用自定义时间，默认 false |
| customStartTime | String | 否 | 自定义开始时间，格式 "HH:mm"，如 "08:00" |
| customEndTime | String | 否 | 自定义结束时间，格式 "HH:mm"，如 "08:45" |
| color | Int | 否 | 课程颜色值，十进制整数，如 16777215（白色） |
| remark | String | 否 | 备注信息 |

#### timeSlots (节次时间表)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| number | Int | 是 | 节次序号，从1开始 |
| startTime | String | 是 | 开始时间，格式 "HH:mm" |
| endTime | String | 是 | 结束时间，格式 "HH:mm" |
| alias | String | 否 | 节次别名，如 "第一节" |

#### config (课表配置)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| semesterStartDate | String | 否 | null | 学期开始日期，格式 "yyyy-MM-dd" |
| semesterTotalWeeks | Int | 否 | 20 | 学期总周数 |
| defaultClassDuration | Int | 否 | 45 | 默认课时长度（分钟） |
| defaultBreakDuration | Int | 否 | 10 | 默认课间休息时间（分钟） |
| firstDayOfWeek | Int | 否 | 1 | 一周起始日（1=周一，7=周日） |

### 1.3 示例

```json
{
  "courses": [
    {
      "name": "高等数学",
      "teacher": "张教授",
      "position": "教学楼A-101",
      "day": 1,
      "startSection": 1,
      "endSection": 2,
      "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
      "color": 65535,
      "remark": "每周一上午"
    },
    {
      "name": "大学英语",
      "teacher": "李老师",
      "position": "语言中心-203",
      "day": 3,
      "startSection": 3,
      "endSection": 4,
      "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      "isCustomTime": true,
      "customStartTime": "14:00",
      "customEndTime": "15:30"
    }
  ],
  "timeSlots": [
    { "number": 1, "startTime": "08:00", "endTime": "08:45", "alias": "第一节" },
    { "number": 2, "startTime": "08:55", "endTime": "09:40", "alias": "第二节" },
    { "number": 3, "startTime": "10:00", "endTime": "10:45", "alias": "第三节" },
    { "number": 4, "startTime": "10:55", "endTime": "11:40", "alias": "第四节" },
    { "number": 5, "startTime": "14:00", "endTime": "14:45", "alias": "第五节" },
    { "number": 6, "startTime": "14:55", "endTime": "15:40", "alias": "第六节" }
  ],
  "config": {
    "semesterStartDate": "2024-09-02",
    "semesterTotalWeeks": 16,
    "defaultClassDuration": 45,
    "defaultBreakDuration": 10,
    "firstDayOfWeek": 1
  }
}
```

---

## 二、全盘备份格式

> **注意**：全盘备份文件实际使用 **CBOR 二进制编码**（用于 WebDAV 高密二进制流场景），JSON 格式仅用于单表导入场景。以下为等效的 JSON 结构参考。

### 2.1 完整结构

```json
{
  "backupTimestamp": "long (必填)",
  "appVersionCode": "integer (必填)",
  "currentCourseTableId": "string (必填)",
  "allTables": [
    {
      "tableId": "string (必填)",
      "tableName": "string (必填)",
      "createdAt": "long (必填)",
      "tableData": {
        "courses": [...],
        "timeSlots": [...],
        "config": {...}
      }
    }
  ]
}
```

### 2.2 字段详细说明

#### TotalAppBackupEnvelope (备份信封)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| backupTimestamp | Long | 是 | 备份生成时间戳（毫秒） |
| appVersionCode | Int | 是 | 数据协议版本号，当前为 1 |
| currentCourseTableId | String | 是 | 备份前用户当前激活的课表 UUID |
| allTables | List\<SingleTablePack\> | 是 | 系统中所有课表的集合 |

#### SingleTablePack (单个课表包裹)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tableId | String | 是 | 课表在数据库中的物理 UUID 主键 |
| tableName | String | 是 | 课表名称 |
| createdAt | Long | 是 | 课表创建时间戳，用于恢复后列表排序 |
| tableData | CourseTableExportModel | 是 | 课表数据（使用导出模型，与导入模型有差异） |

#### tableData 课程字段差异说明

`tableData.courses` 使用的是 `ExportCourseJsonModel`（导出模型），与导入模型 `ImportCourseJsonModel` 的字段必填性有所不同：

| 字段名 | 导入模型 | 导出模型 | 说明 |
|--------|----------|----------|------|
| id | 可选 | **必填** | 导出时必须包含课程唯一标识 |
| color | 可选 | **必填** | 导出时必须包含颜色值 |
| name | 必填 | 必填 | 课程名称 |
| teacher | 必填 | 必填 | 教师姓名 |
| position | 必填 | 必填 | 上课地点 |
| day | 必填 | 必填 | 星期几 |
| weeks | 必填 | 必填 | 周次列表 |

### 2.3 示例

```json
{
  "backupTimestamp": 1715000000000,
  "appVersionCode": 1,
  "currentCourseTableId": "550e8400-e29b-41d4-a716-446655440000",
  "allTables": [
    {
      "tableId": "550e8400-e29b-41d4-a716-446655440000",
      "tableName": "2024秋季学期",
      "createdAt": 1710000000000,
      "tableData": {
        "courses": [...],
        "timeSlots": [...],
        "config": {...}
      }
    },
    {
      "tableId": "550e8400-e29b-41d4-a716-446655440001",
      "tableName": "2024春季学期",
      "createdAt": 1700000000000,
      "tableData": {
        "courses": [...],
        "timeSlots": [...],
        "config": {...}
      }
    }
  ]
}
```

---

## 三、数据协议版本

| 版本号 | 说明 |
|--------|------|
| 1 | 基础全量多课表备份协议规范 |

> 未来如果重构底层数据架构，可手动升级版本号，并编写迁移清洗流程。

---

## 四、JSON 解析配置

系统使用以下配置解析导入的 JSON 文件：

- `ignoreUnknownKeys = true`: 旧版 App 遇到新加字段时会跳过而不崩溃
- `encodeDefaults = true`: 导出时即使字段是默认值也会包含在 JSON 中
- `coerceInputValues = true`: 自动处理类型不匹配的情况

---

## 五、核心代码参考

数据模型定义于：[CourseImportExport.kt](../app/src/main/java/com/xingheyuzhuan/shiguangschedule/data/repository/CourseImportExport.kt)
