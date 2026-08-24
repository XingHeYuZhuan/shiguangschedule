import assert from "node:assert/strict";

globalThis.__SHIGUANG_TEST_MODE__ = true;
await import("../../shared/assets/offline_repo/schools/resources/SEU/seu_01.js");

const adapter = globalThis.SeuShiguangAdapter;
assert.ok(adapter, "adapter should be exported in test mode");

assert.deepEqual(adapter.parseWeeksFromSkzc("00111001"), [3, 4, 5, 8]);
assert.deepEqual(adapter.parseWeeksFromSkzc("1-8周(单),10周"), [1, 3, 5, 7, 10]);

assert.deepEqual(adapter.parseSemesterRow({ DM: "2025-2026-2", MC: "2025-2026学年第二学期" }), {
    code: "2025-2026-2",
    academicYear: "2025-2026",
    term: "2",
    label: "2025-2026学年第二学期",
    isCurrent: false
});

const courses = adapter.parseApiCourses([
    { KCM: "计算机网络", JSXM: "张三/李四", CAMPUSNAME: "九龙湖", JASMC: "教一-101", SKXQ: "3", KSJC: "3", JSJC: "5", ZCBH: "011111" },
    { KCM: "计算机网络", JSXM: "张三/李四", CAMPUSNAME: "九龙湖", JASMC: "教一-101", SKXQ: "3", KSJC: "3", JSJC: "5", ZCBH: "011111" },
    { KCM: "体育", SKJS: "王老师", JASMC: "体育馆", SKXQ: "5", KSJC: "1", JSJC: "2", ZCMC: "1-8周(双)" }
]);
assert.equal(courses.length, 2, "duplicate API rows should be removed");
assert.deepEqual(courses[0], {
    name: "计算机网络", teacher: "张三,李四", position: "九龙湖 教一-101", day: 3,
    startSection: 3, endSection: 5, weeks: [2, 3, 4, 5, 6], isCustomTime: false, remark: null
});
assert.deepEqual(courses[1].weeks, [2, 4, 6, 8]);

assert.equal(adapter.parseApiCourse({ KCM: "缺少排课字段", ZCBH: "1" }), null,
    "rows without weekday or section numbers should be skipped");
assert.equal(adapter.parseApiCourse({
    KCM: "非法星期", SKXQ: "NaN", KSJC: "1", JSJC: "2", ZCBH: "1"
}), null, "non-numeric weekdays should be skipped");

const fallbackCourses = adapter.buildCoursesFromDescriptors([
    { name: "可解析课程", teacher: "教师", detail: "1-2周,星期一,第1-2节,教一-101" },
    { name: "异常课程", teacher: "教师", detail: "缺少星期与节次" }
]);
assert.equal(fallbackCourses.length, 1, "one malformed DOM row must not abort the page fallback");
assert.equal(fallbackCourses[0].name, "可解析课程");

assert.deepEqual(adapter.parseTimeSlots([
    { DM: "2", KSSJ: "8:50", JSSJ: "9:35", SFSY: "1" },
    { DM: "1", KSSJ: "8:00", JSSJ: "8:45", SFSY: "1" },
    { DM: "3", KSSJ: "09:50", JSSJ: "10:35", SFSY: "0" }
]), [
    { number: 1, startTime: "08:00", endTime: "08:45" },
    { number: 2, startTime: "08:50", endTime: "09:35" }
]);
assert.equal(adapter.normalizeTime("24:00"), null, "out-of-range hours should be rejected");
assert.equal(adapter.normalizeTime("08:60"), null, "out-of-range minutes should be rejected");
assert.equal(adapter.parseTimeSlots([
    { DM: "2", KSSJ: "08:50", JSSJ: "09:35", SFSY: "1" }
]).length, 13, "non-contiguous API time slots should fall back to the known SEU schedule");

assert.deepEqual(adapter.parseCalendar([{ XQKSRQ: "2026-02-23 00:00:00", ZZC: "18" }]), {
    semesterStartDate: "2026-02-23",
    semesterTotalWeeks: 18
});

assert.deepEqual(adapter.deriveCalendarFromCourses([
    { KSRQ: "2026-09-01", SKXQ: 2, ZCBH: "0111" },
    { KSRQ: "2026-08-27", SKXQ: 4, ZCBH: "1111" }
]), {
    semesterStartDate: "2026-08-24",
    semesterTotalWeeks: 4
});

const importResult = {
    courses: [{ name: "测试课程" }],
    timeSlots: [{ number: 1, startTime: "08:00", endTime: "08:45" }],
    calendar: { semesterStartDate: "2026-08-24", semesterTotalWeeks: 18 }
};
let savedCourseTable;
await adapter.saveImport({
    saveImportedCourseTable: async (jsonString) => {
        savedCourseTable = JSON.parse(jsonString);
        return true;
    }
}, importResult);
assert.deepEqual(savedCourseTable, {
    courses: importResult.courses,
    timeSlots: importResult.timeSlots,
    config: importResult.calendar
}, "course data must be handed to native in one atomic import request");
await assert.rejects(
    () => adapter.saveImport({}, importResult),
    /不支持安全的整表导入/,
    "legacy three-step saving must not be used because it can leave partial data"
);

console.log("SEU adapter parser tests passed");
