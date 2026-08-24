import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const adapterPath = path.resolve(
  here,
  "../../shared/assets/offline_repo/schools/resources/SEU/seu_01.js"
);
const fixturePath = path.join(here, "fixtures/selected-courses.json");

const context = {
  console,
  globalThis: null,
  __SHIGUANG_TEST_MODE__: true
};
context.globalThis = context;
vm.createContext(context);
vm.runInContext(fs.readFileSync(adapterPath, "utf8"), context, { filename: adapterPath });

const adapter = context.SeuShiguangAdapter;
const fixture = JSON.parse(fs.readFileSync(fixturePath, "utf8"));

assert.equal(adapter.parseWeekday("星期1"), 1);
assert.equal(adapter.parseWeekday("星期三"), 3);
assert.equal(adapter.parseWeekday("Sunday"), 7);
assert.deepEqual(Array.from(adapter.parseWeekText("1-8周(单)")), [1, 3, 5, 7]);
assert.deepEqual(Array.from(adapter.parseWeekText("2,4,6-8周")), [2, 4, 6, 7, 8]);

const detail = adapter.parseScheduleDetail("1-16周(双),星期五,8-9节,教一-201,教学班群号：123");
assert.deepEqual(JSON.parse(JSON.stringify(detail)), {
  day: 5,
  startSection: 8,
  endSection: 9,
  weeks: [2, 4, 6, 8, 10, 12, 14, 16],
  position: "教一-201",
  remark: "教学班群号：123"
});

const courses = adapter.buildCoursesFromDescriptors(fixture);
assert.equal(courses.length, 2, "Duplicate rendered course blocks should be removed");
assert.deepEqual(JSON.parse(JSON.stringify(courses[0])), {
  name: "测试课程甲",
  teacher: "测试教师",
  position: "测试楼-101",
  day: 1,
  startSection: 3,
  endSection: 4,
  weeks: [1, 3, 5, 7, 9, 11, 13, 15],
  isCustomTime: false,
  remark: "教学班群号：123456"
});
assert.equal(adapter.timeSlots.length, 13);
assert.deepEqual(JSON.parse(JSON.stringify(adapter.timeSlots[12])), {
  number: 13,
  startTime: "20:40",
  endTime: "21:25"
});

console.log("SEU eHall rendered timetable parser tests passed.");
