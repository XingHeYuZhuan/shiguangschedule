// 文件: school.js

// 1. 显示一个公告信息弹窗
async function demoAlert() {
    try {
        console.log("即将显示公告弹窗...");
        const confirmed = await window.AndroidBridgePromise.showAlert(
            "重要通知",
            "这是一个弹窗示例。",
            "好的"
        );
        if (confirmed) {
            console.log("用户点击了确认按钮。Alert Promise Resolved: " + confirmed);
            AndroidBridge.showToast("Alert：用户点击了确认！");
            return true; // 成功时返回 true
        } else {
            console.log("用户点击了取消按钮或关闭了弹窗。Alert Promise Resolved: " + confirmed);
            AndroidBridge.showToast("Alert：用户取消了！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示公告弹窗时发生错误:", error);
        AndroidBridge.showToast("Alert：显示弹窗出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 2. 显示带输入框的弹窗，并进行简单验证
function validateName(name) {
    if (name === null || name.trim().length === 0) {
        return "输入不能为空！";
    }
    if (name.length < 2) {
        return "姓名至少需要2个字符！";
    }
    return false;
}

async function demoPrompt() {
    try {
        console.log("即将显示输入框弹窗...");
        const name = await window.AndroidBridgePromise.showPrompt(
            "输入你的姓名",
            "请输入至少2个字符",
            "测试用户",
            "validateName"
        );
        if (name !== null) {
            console.log("用户输入的姓名是: " + name);
            AndroidBridge.showToast("欢迎你，" + name + "！");
            return true; // 成功时返回 true
        } else {
            console.log("用户取消了输入。");
            AndroidBridge.showToast("Prompt：用户取消了输入！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示输入框弹窗时发生错误:", error);
        AndroidBridge.showToast("Prompt：显示输入框出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 3. 显示一个单选列表弹窗
async function demoSingleSelection() {
    const fruits = ["苹果", "香蕉", "橙子", "葡萄", "西瓜", "芒果"];
    try {
        console.log("即将显示单选列表弹窗...");
        const selectedIndex = await window.AndroidBridgePromise.showSingleSelection(
            "选择你喜欢的水果",
            JSON.stringify(fruits),
            2
        );
        if (selectedIndex !== null && selectedIndex >= 0 && selectedIndex < fruits.length) {
            console.log("用户选择了: " + fruits[selectedIndex] + " (索引: " + selectedIndex + ")");
            AndroidBridge.showToast("你选择了 " + fruits[selectedIndex]);
            return true; // 成功时返回 true
        } else {
            console.log("用户取消了选择。");
            AndroidBridge.showToast("Single Selection：用户取消了选择！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示单选列表弹窗时发生错误:", error);
        AndroidBridge.showToast("Single Selection：显示列表出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 仍然可以使用原始的 AndroidBridge 对象
AndroidBridge.showToast("这是一个来自 JS 的 Toast 消息，会很快消失！");

async function demoSaveCourses() {
    // ... 代码保持不变 ...
    console.log("正在准备测试课程数据...");
    const testCourses = [
    {
        "name": "高等数学",
        "teacher": "张教授",
        "position": "教101",
        "day": 1,
        "startSection": 1,
        "endSection": 2,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    },
    {
        "name": "大学英语",
        "teacher": "李老师",
        "position": "文史楼203",
        "day": 1,
        "startSection": 2,
        "endSection": 4,
        "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    },
    {
        "name": "数据结构",
        "teacher": "王副教授",
        "position": "信息楼B301",
        "day": 7,
        "startSection": 2,
        "endSection": 2,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "数据结构",
        "teacher": "王副教授",
        "position": "信息楼B301",
        "day": 7,
        "startSection": 3,
        "endSection": 3,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "数据结构",
        "teacher": "王副教授",
        "position": "信息楼B301",
        "day": 7,
        "startSection": 4,
        "endSection": 4,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "数据结构",
        "teacher": "王副教授",
        "position": "信息楼B301",
        "day": 7,
        "startSection": 5,
        "endSection": 5,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "数据结构",
        "teacher": "王副教授",
        "position": "信息楼B301",
        "day": 7,
        "startSection": 6,
        "endSection": 6,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "计算机组成原理",
        "teacher": "赵教授",
        "position": "实验楼401",
        "day": 4,
        "startSection": 4,
        "endSection": 4,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    },
    {
        "name": "操作系统",
        "teacher": "钱副教授",
        "position": "信息楼C205",
        "day": 5,
        "startSection": 5,
        "endSection": 5,
        "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    },
    {
        "name": "计算机网络",
        "teacher": "孙教授",
        "position": "信息楼D103",
        "day": 6,
        "startSection": 6,
        "endSection": 6,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "软件工程",
        "teacher": "周副教授",
        "position": "创新楼301",
        "day": 7,
        "startSection": 7,
        "endSection": 7,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    },
    {
        "name": "数据库原理",
        "teacher": "吴教授",
        "position": "信息楼E201",
        "day": 1,
        "startSection": 8,
        "endSection": 8,
        "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    },
    {
        "name": "人工智能",
        "teacher": "郑副教授",
        "position": "智能楼101",
        "day": 2,
        "startSection": 9,
        "endSection": 9,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "机器学习",
        "teacher": "冯教授",
        "position": "智能楼203",
        "day": 3,
        "startSection": 10,
        "endSection": 10,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    },
    {
        "name": "编译原理",
        "teacher": "陈副教授",
        "position": "信息楼F105",
        "day": 4,
        "startSection": 11,
        "endSection": 11,
        "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    },
    {
        "name": "计算机图形学",
        "teacher": "褚教授",
        "position": "图形楼301",
        "day": 5,
        "startSection": 12,
        "endSection": 12,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "网络安全",
        "teacher": "卫副教授",
        "position": "安全楼201",
        "day": 6,
        "startSection": 13,
        "endSection": 13,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    },
    {
        "name": "分布式系统",
        "teacher": "蒋教授",
        "position": "云楼101",
        "day": 7,
        "startSection": 14,
        "endSection": 14,
        "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19]
    },
    {
        "name": "大数据技术",
        "teacher": "沈副教授",
        "position": "数据楼301",
        "day": 1,
        "startSection": 15,
        "endSection": 15,
        "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
    },
    {
        "name": "物联网技术",
        "teacher": "韩教授",
        "position": "物联楼201",
        "day": 2,
        "startSection": 16,
        "endSection": 16,
        "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    }
    ];

    try {
        console.log("正在尝试导入课程...");
        const result = await window.AndroidBridgePromise.saveImportedCourses(JSON.stringify(testCourses));
        if (result === true) {
            console.log("课程导入成功！");
            AndroidBridge.showToast("测试课程导入成功！");
        } else {
            console.log("课程导入未成功，结果：" + result);
            AndroidBridge.showToast("测试课程导入失败，请查看日志。");
        }
    } catch (error) {
        console.error("导入课程时发生错误:", error);
        AndroidBridge.showToast("导入课程失败: " + error.message);
    }
}

// 5. 导入预设时间段（保持不变）
async function importPresetTimeSlots() {
    console.log("正在准备预设时间段数据...");
    const presetTimeSlots = [
        { "number": 1, "startTime": "08:00", "endTime": "08:01" },
        { "number": 2, "startTime": "09:00", "endTime": "09:01" },
        { "number": 3, "startTime": "10:00", "endTime": "10:01" },
        { "number": 4, "startTime": "11:00", "endTime": "11:01" },
        { "number": 5, "startTime": "12:00", "endTime": "12:01" },
        { "number": 6, "startTime": "13:00", "endTime": "13:01" },
        { "number": 7, "startTime": "14:00", "endTime": "14:01" },
        { "number": 8, "startTime": "15:00", "endTime": "15:01" },
        { "number": 9, "startTime": "16:00", "endTime": "16:01" },
        { "number": 10, "startTime": "17:00", "endTime": "17:01" },
        { "number": 11, "startTime": "18:00", "endTime": "18:01" },
        { "number": 12, "startTime": "19:00", "endTime": "19:01" },
        { "number": 13, "startTime": "20:00", "endTime": "20:01" },
        { "number": 14, "startTime": "21:00", "endTime": "21:01" },
        { "number": 15, "startTime": "22:00", "endTime": "22:01" },
        { "number": 16, "startTime": "23:00", "endTime": "23:01" }
    ];

    try {
        console.log("正在尝试导入预设时间段...");
        const result = await window.AndroidBridgePromise.savePresetTimeSlots(JSON.stringify(presetTimeSlots));
        if (result === true) {
            console.log("预设时间段导入成功！");
            window.AndroidBridge.showToast("测试时间段导入成功！");
        } else {
            console.log("预设时间段导入未成功，结果：" + result);
            window.AndroidBridge.showToast("测试时间段导入失败，请查看日志。");
        }
    } catch (error) {
        console.error("导入时间段时发生错误:", error);
        window.AndroidBridge.showToast("导入时间段失败: " + error.message);
    }
}

/**
 * 编排这些异步操作，并在用户取消时停止后续执行。
 */
async function runAllDemosSequentially() {
    AndroidBridge.showToast("所有演示将按顺序开始...");

    // 1. 运行第一个演示：Alert
    const alertResult = await demoAlert();
    if (!alertResult) {
        console.log("用户取消了 Alert 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    // 2. 运行第二个演示：Prompt
    const promptResult = await demoPrompt();
    if (!promptResult) {
        console.log("用户取消了 Prompt 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    // 3. 运行第三个演示：SingleSelection
    const selectionResult = await demoSingleSelection();
    if (!selectionResult) {
        console.log("用户取消了 Single Selection 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    console.log("所有弹窗演示已完成。");
    AndroidBridge.showToast("所有弹窗演示已完成！");

    // 以下是数据导入，与用户交互无关，可以继续
    await demoSaveCourses();
    await importPresetTimeSlots();
}

// 启动所有演示
runAllDemosSequentially();