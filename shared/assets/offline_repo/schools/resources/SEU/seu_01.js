(function (root) {
    "use strict";

    const ADAPTER_NAME = "东南大学网上办事大厅课表适配";
    const ALLOWED_HOST = "ehall.seu.edu.cn";
    const SCHEDULE_PATH = "/jwapp/sys/wdkb/";
    const COURSE_SELECTOR = ".mtt_item_kcmc";

    const SEU_TIME_SLOTS = [
        [1, "08:00", "08:45"], [2, "08:50", "09:35"], [3, "09:50", "10:35"],
        [4, "10:40", "11:25"], [5, "11:30", "12:15"], [6, "14:00", "14:45"],
        [7, "14:50", "15:35"], [8, "15:50", "16:35"], [9, "16:40", "17:25"],
        [10, "17:30", "18:15"], [11, "19:00", "19:45"], [12, "19:50", "20:35"],
        [13, "20:40", "21:25"]
    ].map(([number, startTime, endTime]) => ({ number, startTime, endTime }));

    function bridgePromise() {
        return root.shiguangBridgePromise || root.AndroidBridgePromise;
    }

    function bridgeSync() {
        return root.shiguangBridge || root.AndroidBridge;
    }

    function toast(message) {
        const bridge = bridgeSync();
        if (bridge && typeof bridge.showToast === "function") bridge.showToast(message);
        else console.info(`[${ADAPTER_NAME}] ${message}`);
    }

    function normalizeText(value) {
        return String(value == null ? "" : value)
            .replace(/\u00a0/g, " ")
            .replace(/\s+/g, " ")
            .trim();
    }

    function parseWeekday(value) {
        const text = normalizeText(value).toLowerCase();
        const chinese = { "一": 1, "二": 2, "三": 3, "四": 4, "五": 5, "六": 6, "日": 7, "天": 7 };
        const chineseMatch = text.match(/(?:星期|周)\s*([1-7一二三四五六日天])/);
        if (chineseMatch) return chinese[chineseMatch[1]] || Number.parseInt(chineseMatch[1], 10);
        const english = text.match(/\b(mon|tue|wed|thu|fri|sat|sun)(?:day)?\b/);
        if (english) return ["mon", "tue", "wed", "thu", "fri", "sat", "sun"].indexOf(english[1]) + 1;
        return null;
    }

    function parseWeekText(value) {
        const weeks = new Set();
        const normalized = normalizeText(value)
            .replace(/[第周]/g, "")
            .replace(/[（）]/g, (char) => char === "（" ? "(" : ")")
            .replace(/至|~|—/g, "-");

        normalized.split(/[,，、;；]/).forEach((rawPart) => {
            const odd = /单/.test(rawPart);
            const even = /双/.test(rawPart);
            const part = rawPart.replace(/[单双()]/g, "").trim();
            if (!part) return;
            const range = part.match(/^(\d+)\s*-\s*(\d+)$/);
            if (range) {
                const start = Number.parseInt(range[1], 10);
                const end = Number.parseInt(range[2], 10);
                for (let week = start; week <= end; week += 1) {
                    if (odd && week % 2 === 0) continue;
                    if (even && week % 2 !== 0) continue;
                    weeks.add(week);
                }
                return;
            }
            const single = Number.parseInt(part, 10);
            if (Number.isInteger(single) && single > 0) weeks.add(single);
        });
        return Array.from(weeks).sort((a, b) => a - b);
    }

    function parseSections(value) {
        const text = normalizeText(value).replace(/至|~|—/g, "-");
        const range = text.match(/(?:第\s*)?(\d+)\s*-\s*(\d+)\s*节?/);
        if (range) {
            const startSection = Number.parseInt(range[1], 10);
            const endSection = Number.parseInt(range[2], 10);
            if (startSection > 0 && endSection >= startSection) return { startSection, endSection };
        }
        const single = text.match(/(?:第\s*)?(\d+)\s*节/);
        if (single) {
            const section = Number.parseInt(single[1], 10);
            if (section > 0) return { startSection: section, endSection: section };
        }
        return null;
    }

    function parseScheduleDetail(value) {
        const detailText = normalizeText(value);
        const tokens = detailText.split(/[,，]/).map(normalizeText).filter(Boolean);
        const dayIndex = tokens.findIndex((token) => parseWeekday(token) !== null);
        if (dayIndex < 0) throw new Error(`无法解析星期：${detailText}`);
        const sectionIndex = tokens.findIndex((token, index) => index > dayIndex && parseSections(token) !== null);
        if (sectionIndex < 0) throw new Error(`无法解析节次：${detailText}`);
        const weeks = parseWeekText(tokens.slice(0, dayIndex).join(","));
        if (weeks.length === 0) throw new Error(`无法解析周次：${detailText}`);

        const sections = parseSections(tokens[sectionIndex]);
        const remaining = tokens.slice(sectionIndex + 1);
        const groupIndex = remaining.findIndex((token) => /教学班|群号|班号/.test(token));
        const positionTokens = groupIndex >= 0 ? remaining.slice(0, groupIndex) : remaining;
        const remarkTokens = groupIndex >= 0 ? remaining.slice(groupIndex) : [];
        return {
            day: parseWeekday(tokens[dayIndex]),
            startSection: sections.startSection,
            endSection: sections.endSection,
            weeks,
            position: normalizeText(positionTokens.join(",").replace(/^(?:地点|教室)[:：]\s*/, "")),
            remark: normalizeText(remarkTokens.join(","))
        };
    }

    function descriptorFromElement(element) {
        const detailElement = element.querySelector(".mtt_item_room");
        const detail = normalizeText(detailElement && (detailElement.innerText || detailElement.textContent));
        const directText = Array.from(element.childNodes || [])
            .filter((node) => node.nodeType === 3)
            .map((node) => normalizeText(node.textContent))
            .filter(Boolean)
            .join(" ");
        const childTexts = Array.from(element.children || [])
            .filter((child) => child !== detailElement)
            .map((child) => normalizeText(child.innerText || child.textContent))
            .filter(Boolean);
        const allLines = String(element.innerText || element.textContent || "")
            .split(/\n+/)
            .map(normalizeText)
            .filter(Boolean)
            .filter((line) => line !== detail);
        return {
            name: directText || allLines[0] || "",
            teacher: childTexts[0] || allLines[1] || "",
            detail
        };
    }

    function normalizeDescriptor(descriptor) {
        const name = normalizeText(descriptor.name);
        if (!name) throw new Error("发现课程名称为空的课表节点");
        const parsed = parseScheduleDetail(descriptor.detail);
        return {
            name,
            teacher: normalizeText(descriptor.teacher),
            position: parsed.position,
            day: parsed.day,
            startSection: parsed.startSection,
            endSection: parsed.endSection,
            weeks: parsed.weeks,
            isCustomTime: false,
            remark: parsed.remark || null
        };
    }

    function buildCoursesFromDescriptors(descriptors) {
        const result = [];
        const seen = new Set();
        descriptors.forEach((descriptor) => {
            const course = normalizeDescriptor(descriptor);
            const key = [course.name, course.teacher, course.position, course.day,
                course.startSection, course.endSection, course.weeks.join(",")].join("|");
            if (!seen.has(key)) {
                seen.add(key);
                result.push(course);
            }
        });
        return result;
    }

    function buildCoursesFromDocument(documentObject) {
        return buildCoursesFromDescriptors(
            Array.from(documentObject.querySelectorAll(COURSE_SELECTOR)).map(descriptorFromElement)
        );
    }

    async function waitForRenderedCourses(timeoutMillis) {
        const startedAt = Date.now();
        while (Date.now() - startedAt < timeoutMillis) {
            if (root.document && root.document.querySelectorAll(COURSE_SELECTOR).length > 0) return;
            await new Promise((resolve) => root.setTimeout(resolve, 300));
        }
    }

    async function runImportFlow() {
        try {
            if (!root.location || root.location.hostname !== ALLOWED_HOST || !root.location.pathname.includes(SCHEDULE_PATH)) {
                throw new Error("请先完成统一身份认证，并进入网上办事大厅的“我的课表”页面");
            }
            const bridge = bridgePromise();
            if (!bridge) throw new Error("未检测到拾光课程表桥接接口");

            await waitForRenderedCourses(6000);
            const courses = buildCoursesFromDocument(root.document);
            if (courses.length === 0) {
                throw new Error("页面没有可导入的课程。请切换到“学期课表”，等待课表显示完整后再执行导入");
            }

            const semesterNode = root.document.querySelector("#dqxnxq2");
            const semesterLabel = normalizeText(semesterNode && semesterNode.textContent);
            const confirmed = await bridge.showAlert(
                "导入东南大学个人课表",
                `将从网上办事大厅“我的课表”读取${semesterLabel ? ` ${semesterLabel} ` : "当前学期"}页面中已显示的 ${courses.length} 个课程时段。不会访问选课系统或执行任何写操作。`,
                "开始导入"
            );
            if (!confirmed) return false;

            const saved = await bridge.saveImportedCourses(JSON.stringify(courses));
            if (!saved) throw new Error("应用未能保存课程数据");
            await bridge.savePresetTimeSlots(JSON.stringify(SEU_TIME_SLOTS));

            toast(`成功导入 ${courses.length} 个课程时段`);
            const sync = bridgeSync();
            if (sync && typeof sync.notifyTaskCompletion === "function") sync.notifyTaskCompletion();
            return true;
        } catch (error) {
            const message = error && error.message ? error.message : String(error);
            console.error(`[${ADAPTER_NAME}] 导入失败：${message}`);
            toast(`东南大学导入失败：${message}`);
            const bridge = bridgePromise();
            if (bridge && typeof bridge.showAlert === "function") {
                await bridge.showAlert("东南大学导入失败", message, "知道了");
            }
            return false;
        }
    }

    root.SeuShiguangAdapter = Object.freeze({
        parseWeekday, parseWeekText, parseSections, parseScheduleDetail,
        buildCoursesFromDescriptors, buildCoursesFromDocument,
        timeSlots: SEU_TIME_SLOTS, runImportFlow
    });

    if (!root.__SHIGUANG_TEST_MODE__) runImportFlow();
})(typeof window !== "undefined" ? window : globalThis);
