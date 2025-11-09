# shiguangschedule

本仓库为拾光课程表（shiguangschedule）项目的主仓库，包含软件的全部源代码及相关资源。项目采用开源模式，欢迎社区开发者参与贡献和适配。

## 项目定位

拾光课程表是一款面向中国高校师生的课程表管理工具，支持通过适配脚本导入各类教务系统课程数据，方便用户高效管理个人课表。项目注重开放性和可扩展性，鼓励社区开发者参与适配和功能完善。

## 关于项目版本

项目分为 **开发版（`dev`）** 和 **正式版（`prod`）** 两个版本。

主要特性和区别如下：

1.  **安全性：正式版**开启了**基准灯塔标签验证**，确保用户导入的适配脚本是安全可靠的。
2.  **仓库可见性：正式版**默认**隐藏了自定义/私有仓库**，防止普通用户误用未经官方验证的脚本，提供了更高的安全性。
3.  **版本标识：开发版**使用 `.dev` 后缀，允许其与正式版共存，便于开发者进行测试。
4.  **调试工具：** **正式版**会禁用 **DevTools** 选项，**防止普通用户误触启用调试功能，从而避免潜在的信息泄露或配置被意外修改的风险**。

**重要提示：**

**正式版 (`prod`)** 默认**开启了安全验证**并**隐藏了自定义仓库**，为普通用户提供了更严格的安全保障。**强烈推荐普通用户使用正式版。**  

正式版图标是**蓝色**背景 开发者版图标是**红色**背景 注意不要搞混了

### 语言支持
- 简体中文
- 繁体中文
- 英语

-----

## 如何参与

1. Fork 本仓库，提交你的改进或教务适配使用的可调用组件。
2. 提交 Pull Request，等待审核合并(main分支已经开启分支保护,提交需要提交到dev分支)。
3. 如有问题或建议，欢迎在 GitHub 提交 Issue 或加入社区讨论。

## 相关链接

- 项目主页：[https://github.com/XingHeYuZhuan/shiguangschedule](https://github.com/XingHeYuZhuan/shiguangschedule)
- 适配脚本仓库：[https://github.com/XingHeYuZhuan/shiguang_warehouse](https://github.com/XingHeYuZhuan/shiguang_warehouse)
- 查看如何适配,Wiki：[https://github.com/XingHeYuZhuan/shiguangschedule/wiki](https://github.com/XingHeYuZhuan/shiguangschedule/wiki)
- 浏览器测试插件:[https://github.com/XingHeYuZhuan/shiguang_Tester](https://github.com/XingHeYuZhuan/shiguang_Tester)

---

如有问题或建议，欢迎提交 Issue 或 PR。

## 贡献  
欢迎任何人提交你的贡献  
### 教务适配贡献  
[![app-Contributors](https://stg.contrib.rocks/image?repo=XingHeYuZhuan/shiguang_warehouse)](https://github.com/XingHeYuZhuan/shiguang_warehouse/graphs/contributors)  

### 软件开发贡献  
[![app-Contributors](https://stg.contrib.rocks/image?repo=XingHeYuZhuan/shiguangschedule)](https://github.com/XingHeYuZhuan/shiguangschedule/graphs/contributors)  
