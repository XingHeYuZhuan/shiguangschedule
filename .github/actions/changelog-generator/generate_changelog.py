import subprocess
import os
import re
import sys 
from datetime import date

# --- 1. 常量与提交分类配置 ---

# Conventional Commits 类型与 CHANGELOG 分类标题的映射关系
COMMIT_TYPES = {
    'feat': '✨ 新增功能 (Features)',
    'fix': '🐛 Bug 修复 (Bug Fixes)',
    'improve': '💡 功能与体验优化 (Improvements)',
    'perf': '🚀 性能与代码改进 (Improvements)',
    'refactor': '🚀 性能与代码改进 (Improvements)',
    'style': '🚀 性能与代码改进 (Improvements)',
    'docs': '📚 文档更新 (Documentation)',
}

# 忽略的维护性提交类型（不会出现在最终更新日志中）
EXCLUDED_TYPES = ['chore', 'ci', 'build', 'test']

# 未指定类型或不匹配规范时的默认分类
OTHER_CATEGORY = '🚧 其他提交 (Other Commits)'

# 日志输出的目标 Markdown 文件路径
CHANGELOG_PATH = 'CHANGELOG.md'


# --- 2. 辅助函数 ---

def get_latest_tag(is_prerelease=False):
    """
    获取离当前 HEAD 最近的前一个 Git 标签 (Tag)

    :param is_prerelease: bool, 是否为预发布版本。
                          - True: 抓取离 HEAD~1 最近的任意类型 Tag (包括 RC/Pre/正式版)；
                          - False: 使用正则严格筛选上一次发布的纯数字正式版 Tag (如 v1.0.0, 避开预发布 Tag)。
    :return: str 或 None, 匹配到的 Tag 名称
    """
    if is_prerelease:
        # 预发布场景：优先获取 HEAD~1 节点上的最近标签，避免抓取到刚刚打在当前 HEAD 上的标签
        try:
            cmd = 'git describe --tags --abbrev=0 HEAD~1'
            output = subprocess.check_output(
                cmd, shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
            ).strip()
            if output:
                return output
        except Exception:
            # 兜底处理：若 HEAD~1 无历史标签，尝试获取当前能查到的最新标签
            try:
                output = subprocess.check_output(
                    'git describe --tags --abbrev=0',
                    shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
                ).strip()
                return output if output else None
            except Exception:
                return None

    # 正式发布场景：读取所有按创建时间倒序排列的 Tag，筛选出符合语义化版本的正式版 Tag
    try:
        output = subprocess.check_output(
            'git tag --sort=-creatordate',
            shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
        ).strip()

        if not output:
            return None

        tags = output.split('\n')
        # 正则表达式：只匹配纯数字版本号 (例如 v1.0.0 或 1.0.0，自动过滤掉带有 -rc1、-beta 等后缀的预发布 Tag)
        official_pattern = re.compile(r'^v?\d+\.\d+\.\d+$')

        for tag in tags:
            tag = tag.strip()
            if official_pattern.match(tag):
                return tag
        return None
    except Exception:
        return None

def get_first_commit_hash():
    """获取当前仓库的初始（第一个）Commit Hash，用于无历史 Tag 时的全量日志收集"""
    try:
        return subprocess.check_output(
            'git rev-list --max-parents=0 HEAD',
            shell=True, text=True, encoding='utf-8'
        ).strip()
    except Exception:
        return None

def is_valid_ref(ref):
    """校验给定的 Git引用/Tag 是否在当前仓库中真实存在"""
    if not ref:
        return False
    try:
        subprocess.check_output(
            f'git rev-parse --verify {ref}',
            shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
        )
        return True
    except Exception:
        return False


# --- 3. 核心日志生成逻辑 ---

def generate_changelog(version_title, previous_tag=None, is_prerelease=False):
    """
    分析 Git 提交记录并生成 CHANGELOG.md

    :param version_title: str, 生成的日志版本大标题 (例: "v1.0.1")
    :param previous_tag: str (可选), 指定对比的基准起始 Tag。若为 None 则自动推导
    :param is_prerelease: bool, 是否为预发布版本，影响自动推导 Tag 时的过滤策略
    """
    # 1. 未手动指定基准 Tag 时，依据发布策略自动查找基准 Tag
    if not previous_tag:
        previous_tag = get_latest_tag(is_prerelease=is_prerelease)
        if previous_tag:
            print(f"自动识别到的对比版本标签: {previous_tag}")

    # 2. 校验 Tag 有效性，若无效则降级为全量提取
    if previous_tag and not is_valid_ref(previous_tag):
        print(f"警告：指定的对比标签 '{previous_tag}' 在仓库中不存在，将从首个提交开始计算。")
        previous_tag = None

    # 3. 构建 git log 搜索区间 (previous_tag...HEAD 或 从仓库起点到 HEAD)
    if not previous_tag:
        initial_commit = get_first_commit_hash()
        range_str = f"{initial_commit}...HEAD" if initial_commit else "HEAD"
    else:
        range_str = f"{previous_tag}...HEAD"

    # 4. 执行 Git 命令获取指定范围内的提交历史
    log_format = '%H|||%s|||%an'  # 格式: Commit Hash|||Commit Subject|||Author
    log_command = f'git -c i18n.logOutputEncoding=UTF-8 log --pretty=format:"{log_format}" {range_str}'

    try:
        logs_output = subprocess.check_output(
            log_command, shell=True, text=True, encoding='utf-8'
        ).strip()
        logs = logs_output.split('\n')
    except Exception as e:
        print(f"执行 git log 失败: {e}")
        return

    # 5. 解析提交信息并进行分类归档
    categories = {}
    # 解析 Conventional Commits 格式，例如: "feat(auth): add login feature" 或 "fix: resolve crash"
    commit_regex = re.compile(r'^(\w+)(?:\([^)]+\))?[:：]\s*(.*)', re.UNICODE)

    for log in logs:
        if not log or '|||' not in log:
            continue
        parts = log.split('|||')
        if len(parts) < 3:
            continue

        commit_hash, subject, author = parts[0], parts[1], parts[2]
        match = commit_regex.match(subject)

        description = subject
        category_title = OTHER_CATEGORY

        if match:
            type_prefix = match.group(1).lower()
            description = match.group(2)
            # 跳过无需列入 Changelog 的维护性提交
            if type_prefix in EXCLUDED_TYPES:
                continue
            category_title = COMMIT_TYPES.get(type_prefix, OTHER_CATEGORY)
        else:
            # 过滤 Merge 提交以及带排除前缀的普通提交
            if subject.startswith('Merge ') or any(subject.startswith(f"{ex}:") or subject.startswith(f"{ex}：") for ex in EXCLUDED_TYPES):
                continue

        if category_title not in categories:
            categories[category_title] = []
        categories[category_title].append(f"- {description}")

    # 6. 拼装 Markdown 内容
    new_changelog = f"## {version_title}\n\n"

    # 指定分类在 CHANGELOG 中显示的先后顺序
    ordered_titles = [
        COMMIT_TYPES['feat'],
        COMMIT_TYPES['fix'],
        COMMIT_TYPES['improve'],
        COMMIT_TYPES['perf'],
        COMMIT_TYPES['docs'],
        OTHER_CATEGORY
    ]

    has_content = False
    for title in ordered_titles:
        if title in categories and categories[title]:
            new_changelog += f"### {title}\n\n"
            new_changelog += '\n'.join(categories[title]) + '\n\n'
            has_content = True

    if not has_content:
        print("警告: 指定范围内没有找到有效的提交记录，跳过文件生成。")
        return

    # 7. 写入 Changelog 文件
    with open(CHANGELOG_PATH, 'w', encoding='utf-8') as f:
        f.write(new_changelog)

    print(f"CHANGELOG.md 已更新: {version_title}")


# --- 4. 命令行入口 ---

if __name__ == '__main__':
    try:
        # 参数 1: 版本标题 (缺省时默认为当前日期 vYYYY-MM-DD)
        v_title = sys.argv[1] if len(sys.argv) > 1 else f"v{date.today().isoformat()}"

        # 参数 2: 基准起始 Tag (可置空或传 "")
        p_tag = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] != '' else None

        # 参数 3: 是否为预发布版本 ('true' / 'false')
        is_pre = sys.argv[3].lower() == 'true' if len(sys.argv) > 3 else False

        generate_changelog(v_title, p_tag, is_prerelease=is_pre)
    except Exception as e:
        import traceback
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)