(function (root) {
    "use strict";

    const ADAPTER_NAME = "东南大学网上办事大厅课表适配";
    const ALLOWED_HOST = "ehall.seu.edu.cn";
    const API_ROOT = "/gsapp/sys/wdbykbappseu/wdkcb";
    const COURSE_SELECTOR = ".mtt_item_kcmc";
    const ENDPOINTS = Object.freeze({
        semesterList: `${API_ROOT}/getTermList.do`,
        sections: `${API_ROOT}/getSectionList.do`,
        courses: `${API_ROOT}/getTermCourseList.do`
    });
    const SEU_TIME_SLOTS = [
        [1, "08:00", "08:45"], [2, "08:50", "09:35"], [3, "09:50", "10:35"],
        [4, "10:40", "11:25"], [5, "11:30", "12:15"], [6, "14:00", "14:45"],
        [7, "14:50", "15:35"], [8, "15:50", "16:35"], [9, "16:40", "17:25"],
        [10, "17:30", "18:15"], [11, "19:00", "19:45"], [12, "19:50", "20:35"],
        [13, "20:40", "21:25"]
    ].map(([number, startTime, endTime]) => ({ number, startTime, endTime }));

    function bridgePromise() { return root.shiguangBridgePromise || root.AndroidBridgePromise; }
    function bridgeSync() { return root.shiguangBridge || root.AndroidBridge; }
    function toast(message) {
        const bridge = bridgeSync();
        if (bridge && typeof bridge.showToast === "function") bridge.showToast(message);
        else console.info(`[${ADAPTER_NAME}] ${message}`);
    }
    function normalizeText(value) {
        return String(value == null ? "" : value).replace(/\u00a0/g, " ").replace(/\s+/g, " ").trim();
    }

    function normalizeTime(value) {
        const match = normalizeText(value).match(/^(\d{1,2}):(\d{2})$/);
        if (!match) return null;
        const hour = Number.parseInt(match[1], 10);
        const minute = Number.parseInt(match[2], 10);
        if (!Number.isInteger(hour) || hour < 0 || hour > 23 ||
            !Number.isInteger(minute) || minute < 0 || minute > 59) return null;
        return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
    }

    function parseWeekday(value) {
        const text = normalizeText(value).toLowerCase();
        const chinese = { "一": 1, "二": 2, "三": 3, "四": 4, "五": 5, "六": 6, "日": 7, "天": 7 };
        const match = text.match(/(?:星期|周)\s*([1-7一二三四五六日天])/);
        if (match) return chinese[match[1]] || Number.parseInt(match[1], 10);
        const english = text.match(/\b(mon|tue|wed|thu|fri|sat|sun)(?:day)?\b/);
        return english ? ["mon", "tue", "wed", "thu", "fri", "sat", "sun"].indexOf(english[1]) + 1 : null;
    }

    function parseWeekText(value) {
        const weeks = new Set();
        const normalized = normalizeText(value).replace(/[第周]/g, "")
            .replace(/[（）]/g, (char) => char === "（" ? "(" : ")").replace(/至|~|—/g, "-");
        normalized.split(/[,，、;；]/).forEach((rawPart) => {
            const odd = /单/.test(rawPart);
            const even = /双/.test(rawPart);
            const part = rawPart.replace(/[单双()]/g, "").trim();
            const range = part.match(/^(\d+)\s*-\s*(\d+)$/);
            if (range) {
                for (let week = Number(range[1]); week <= Number(range[2]); week += 1) {
                    if ((!odd || week % 2 === 1) && (!even || week % 2 === 0)) weeks.add(week);
                }
            } else if (/^\d+$/.test(part) && Number(part) > 0) weeks.add(Number(part));
        });
        return Array.from(weeks).sort((a, b) => a - b);
    }

    function parseWeeksFromSkzc(value) {
        const text = normalizeText(value);
        if (/^[01]+$/.test(text)) {
            return Array.from(text).flatMap((flag, index) => flag === "1" ? [index + 1] : []);
        }
        return parseWeekText(text);
    }

    function parseSections(value) {
        const text = normalizeText(value).replace(/至|~|—/g, "-");
        const range = text.match(/(?:第\s*)?(\d+)\s*-\s*(\d+)\s*节?/);
        if (range && Number(range[1]) > 0 && Number(range[2]) >= Number(range[1])) {
            return { startSection: Number(range[1]), endSection: Number(range[2]) };
        }
        const single = text.match(/(?:第\s*)?(\d+)\s*节/);
        return single && Number(single[1]) > 0
            ? { startSection: Number(single[1]), endSection: Number(single[1]) } : null;
    }

    function parseScheduleDetail(value) {
        const detailText = normalizeText(value);
        const tokens = detailText.split(/[,，]/).map(normalizeText).filter(Boolean);
        const dayIndex = tokens.findIndex((token) => parseWeekday(token) !== null);
        if (dayIndex < 0) throw new Error(`无法解析星期：${detailText}`);
        const sectionIndex = tokens.findIndex((token, index) => index > dayIndex && parseSections(token));
        if (sectionIndex < 0) throw new Error(`无法解析节次：${detailText}`);
        const weeks = parseWeekText(tokens.slice(0, dayIndex).join(","));
        if (weeks.length === 0) throw new Error(`无法解析周次：${detailText}`);
        const sections = parseSections(tokens[sectionIndex]);
        const remaining = tokens.slice(sectionIndex + 1);
        const groupIndex = remaining.findIndex((token) => /教学班|群号|班号/.test(token));
        return {
            day: parseWeekday(tokens[dayIndex]), startSection: sections.startSection,
            endSection: sections.endSection, weeks,
            position: normalizeText((groupIndex >= 0 ? remaining.slice(0, groupIndex) : remaining).join(",")
                .replace(/^(?:地点|教室)[:：]\s*/, "")),
            remark: normalizeText(groupIndex >= 0 ? remaining.slice(groupIndex).join(",") : "")
        };
    }

    function descriptorFromElement(element) {
        const detailElement = element.querySelector(".mtt_item_room");
        const detail = normalizeText(detailElement && (detailElement.innerText || detailElement.textContent));
        const directText = Array.from(element.childNodes || []).filter((node) => node.nodeType === 3)
            .map((node) => normalizeText(node.textContent)).filter(Boolean).join(" ");
        const childTexts = Array.from(element.children || []).filter((child) => child !== detailElement)
            .map((child) => normalizeText(child.innerText || child.textContent)).filter(Boolean);
        const lines = String(element.innerText || element.textContent || "").split(/\n+/)
            .map(normalizeText).filter(Boolean).filter((line) => line !== detail);
        return { name: directText || lines[0] || "", teacher: childTexts[0] || lines[1] || "", detail };
    }

    function buildCoursesFromDescriptors(descriptors) {
        const seen = new Set();
        return descriptors.flatMap((descriptor) => {
            try {
                const parsed = parseScheduleDetail(descriptor.detail);
                return [{
                    name: normalizeText(descriptor.name), teacher: normalizeText(descriptor.teacher),
                    position: parsed.position, day: parsed.day, startSection: parsed.startSection,
                    endSection: parsed.endSection, weeks: parsed.weeks, isCustomTime: false,
                    remark: parsed.remark || null
                }];
            } catch (error) {
                console.warn(`[${ADAPTER_NAME}] 跳过无法解析的课程条目`, descriptor,
                    error && error.message ? error.message : error);
                return [];
            }
        }).filter((course) => {
            if (!course.name) return false;
            const key = [course.name, course.teacher, course.position, course.day,
                course.startSection, course.endSection, course.weeks.join(",")].join("|");
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        });
    }
    function buildCoursesFromDocument(documentObject) {
        return buildCoursesFromDescriptors(Array.from(documentObject.querySelectorAll(COURSE_SELECTOR)).map(descriptorFromElement));
    }

    function extractRows(payload, key) {
        const rows = payload && payload.datas && payload.datas[key] && payload.datas[key].rows;
        return Array.isArray(rows) ? rows : [];
    }

    async function postApi(path, body) {
        const response = await root.fetch(path, {
            method: "POST", credentials: "include", redirect: "follow",
            headers: {
                "Accept": "application/json, text/javascript, */*; q=0.01",
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest"
            },
            body: body || ""
        });
        const text = await response.text();
        if (!response.ok) throw new Error(`课表接口返回 HTTP ${response.status}`);
        if (/^\s*</.test(text)) {
            if (/login|统一身份认证|auth\.seu\.edu\.cn/i.test(text)) throw new Error("登录会话已失效，请重新完成统一身份认证");
            throw new Error("课表接口返回了网页而不是数据，可能没有应用访问权限");
        }
        try { return JSON.parse(text); }
        catch (_) { throw new Error("课表接口返回的数据格式无法识别"); }
    }

    function parseSemesterRow(row) {
        if (!row) return null;
        const code = normalizeText(row.TERMCODE || row.DM || row.XNXQDM);
        if (!code) return null;
        const parts = code.split("-");
        return {
            code,
            academicYear: parts.length >= 3 ? `${parts[0]}-${parts[1]}` : normalizeText(row.XN),
            term: parts.length >= 3 ? parts[2] : normalizeText(row.XQ),
            label: normalizeText(row.TERMNAME || row.MC || row.XNXQMC || code),
            isCurrent: Number(row.ISCURRENT) === 1
        };
    }

    function parseApiCourse(row) {
        if (!row) return null;
        const skzcWeeks = parseWeeksFromSkzc(row.ZCBH || row.SKZC);
        const weeks = skzcWeeks.length ? skzcWeeks : parseWeekText(row.ZCMC || row.SKZCMC || "");
        const day = Number.parseInt(row.SKXQ, 10);
        const startSection = Number.parseInt(row.KSJC, 10);
        const endSection = Number.parseInt(row.JSJC, 10);
        const name = normalizeText(row.KCM);
        if (!name || !Number.isInteger(day) || day < 1 || day > 7 ||
            !Number.isInteger(startSection) || startSection < 1 ||
            !Number.isInteger(endSection) || endSection < startSection || !weeks.length) return null;
        return {
            name, teacher: normalizeText(row.JSXM || row.RKJS || row.SKJS || "未知教师").replace(/\//g, ","),
            position: normalizeText([row.CAMPUSNAME, row.JASMC || row.SKDD || row.JSMC]
                .filter(Boolean).join(" ")) || "待定", day,
            startSection, endSection, weeks, isCustomTime: false,
            remark: normalizeText(row.KBZ || row.BZ) || null
        };
    }
    function parseApiCourses(rows) {
        const seen = new Set();
        return (rows || []).map(parseApiCourse).filter((course) => {
            if (!course) return false;
            const key = [course.name, course.teacher, course.position, course.day,
                course.startSection, course.endSection, course.weeks.join(",")].join("|");
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        }).sort((a, b) => a.day - b.day || a.startSection - b.startSection || a.name.localeCompare(b.name, "zh-CN"));
    }

    function parseTimeSlots(rows) {
        const slots = (rows || []).filter((row) => row && (row.SFSY == null || Number(row.SFSY) === 1))
            .map((row) => ({ number: Number.parseInt(row.VALUE || row.DM || row.JC, 10),
                startTime: normalizeTime(row.KSSJ), endTime: normalizeTime(row.JSSJ) }))
            .filter((slot) => Number.isInteger(slot.number) && slot.number > 0 &&
                slot.startTime !== null && slot.endTime !== null && slot.startTime < slot.endTime)
            .sort((a, b) => a.number - b.number);
        const isValidSequence = slots.length > 0 && slots.every((slot, index) =>
            slot.number === index + 1 && (index === 0 || slot.startTime >= slots[index - 1].endTime));
        return isValidSequence ? slots : SEU_TIME_SLOTS;
    }
    function parseCalendar(rows) {
        const row = (rows || [])[0];
        if (!row) return null;
        const date = normalizeText(row.XQKSRQ || row.KSRQ).split(/[ T]/)[0];
        const weeks = Number.parseInt(row.ZZC || row.ZC, 10);
        return {
            semesterStartDate: /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : null,
            semesterTotalWeeks: Number.isInteger(weeks) && weeks > 0 ? weeks : 20
        };
    }

    async function selectSemester(bridge) {
        const response = await postApi(ENDPOINTS.semesterList);
        const semesters = (Array.isArray(response.data) ? response.data : [])
            .map(parseSemesterRow).filter(Boolean);
        if (!semesters.length) throw new Error("没有获取到可导入的学期");
        const current = semesters.find((semester) => semester.isCurrent) || null;
        if (current) {
            const choice = await bridge.showSingleSelection("选择学期",
                JSON.stringify([`当前学期：${current.label}`, "选择其他学期"]), 0);
            if (choice == null) return null;
            if (Number(choice) === 0) return current;
        }
        const selected = await bridge.showSingleSelection("选择其他学期",
            JSON.stringify(semesters.map((semester) => semester.label)), 0);
        return selected == null ? null : semesters[Number(selected)] || null;
    }

    async function fetchCourses(semester) {
        const payload = await postApi(ENDPOINTS.courses, `termCode=${encodeURIComponent(semester.code)}`);
        const rows = Array.isArray(payload.theorySchedule) ? payload.theorySchedule : [];
        return { courses: parseApiCourses(rows), rawRows: rows };
    }

    function deriveCalendarFromCourses(rows) {
        let startDate = null;
        let totalWeeks = 0;
        for (const row of rows || []) {
            const weeks = parseWeeksFromSkzc(row.ZCBH || row.SKZC || row.ZCMC);
            if (!weeks.length) continue;
            totalWeeks = Math.max(totalWeeks, ...weeks);
            if (startDate || !/^\d{4}-\d{2}-\d{2}$/.test(normalizeText(row.KSRQ))) continue;
            const day = Number.parseInt(row.SKXQ, 10);
            if (day < 1 || day > 7) continue;
            const date = new Date(`${row.KSRQ}T00:00:00Z`);
            date.setUTCDate(date.getUTCDate() - (day - 1) - ((weeks[0] - 1) * 7));
            startDate = date.toISOString().slice(0, 10);
        }
        return startDate ? { semesterStartDate: startDate, semesterTotalWeeks: totalWeeks || 20 } : null;
    }

    async function importFromApi(bridge) {
        const semester = await selectSemester(bridge);
        if (!semester) return null;
        toast(`正在读取 ${semester.label} 课表…`);
        const [courseResult, timeSlots] = await Promise.all([
            fetchCourses(semester),
            postApi(ENDPOINTS.sections).then((data) => parseTimeSlots(data.sectionList)).catch(() => SEU_TIME_SLOTS)
        ]);
        const courses = courseResult.courses;
        const calendar = deriveCalendarFromCourses(courseResult.rawRows);
        if (!courses.length) throw new Error(`${semester.label} 没有获取到课程数据`);
        return { courses, timeSlots, calendar, source: "办事大厅课表接口" };
    }

    function importFromRenderedPage() {
        if (!root.document) return null;
        const courses = buildCoursesFromDocument(root.document);
        if (!courses.length) return null;
        return { courses, timeSlots: SEU_TIME_SLOTS, calendar: null, source: "课表页面" };
    }

    async function saveImport(bridge, result) {
        if (typeof bridge.saveImportedCourseTable !== "function") {
            throw new Error("当前应用版本不支持安全的整表导入，请升级应用后重试");
        }
        const courseTable = {
            courses: result.courses,
            timeSlots: result.timeSlots,
            config: result.calendar
        };
        if (!await bridge.saveImportedCourseTable(JSON.stringify(courseTable))) {
            throw new Error("应用未能保存课表数据");
        }
    }

    async function runImportFlow() {
        try {
            if (!root.location || root.location.hostname !== ALLOWED_HOST) {
                throw new Error("请先完成统一身份认证，返回东南大学网上办事大厅后再执行导入");
            }
            const bridge = bridgePromise();
            if (!bridge) throw new Error("未检测到拾光课程表桥接接口");
            if (!await bridge.showAlert("导入东南大学个人课表",
                "将使用当前登录会话只读查询网上办事大厅的学期和个人课表数据，不访问选课系统，也不会向学校系统写入任何内容。", "开始读取")) return false;

            let result;
            try { result = await importFromApi(bridge); }
            catch (apiError) {
                console.warn(`[${ADAPTER_NAME}] 接口读取失败，尝试页面解析`, apiError);
                result = importFromRenderedPage();
                if (!result) throw apiError;
                toast("接口读取失败，已改用页面中显示的课表");
            }
            if (!result) return false;
            await saveImport(bridge, result);
            toast(`成功从${result.source}导入 ${result.courses.length} 个课程时段`);
            const sync = bridgeSync();
            if (sync && typeof sync.notifyTaskCompletion === "function") sync.notifyTaskCompletion();
            return true;
        } catch (error) {
            const message = error && error.message ? error.message : String(error);
            console.error(`[${ADAPTER_NAME}] 导入失败：${message}`);
            toast(`东南大学导入失败：${message}`);
            const bridge = bridgePromise();
            if (bridge && typeof bridge.showAlert === "function") await bridge.showAlert("东南大学导入失败", message, "知道了");
            return false;
        }
    }

    root.SeuShiguangAdapter = Object.freeze({
        parseWeekday, parseWeekText, parseWeeksFromSkzc, parseSections, parseScheduleDetail,
        buildCoursesFromDescriptors, buildCoursesFromDocument, parseSemesterRow,
        parseApiCourse, parseApiCourses, parseTimeSlots, parseCalendar, deriveCalendarFromCourses,
        normalizeTime, saveImport,
        timeSlots: SEU_TIME_SLOTS, endpoints: ENDPOINTS, runImportFlow
    });
    if (!root.__SHIGUANG_TEST_MODE__) runImportFlow();
})(typeof window !== "undefined" ? window : globalThis);
