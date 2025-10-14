import subprocess
import os
import re
import sys 
from datetime import date

# --- 1. 常量与配置 ---

COMMIT_TYPES = {
    'feat': '✨ 新增功能 (Features)',
    'fix': '🐛 Bug 修复 (Bug Fixes)',
    'improve': '💡 功能与体验优化 (Improvements)',
    'perf': '🚀 性能与代码改进 (Improvements)',
    'refactor': '🚀 性能与代码改进 (Improvements)',
    'style': '🚀 性能与代码改进 (Improvements)',
    'docs': '📚 文档更新 (Documentation)',
}

# 维护性提交将被排除在更新日志之外
EXCLUDED_TYPES = ['chore', 'ci', 'build', 'test']
OTHER_CATEGORY = '🚧 其他提交 (Other Commits)'
CHANGELOG_PATH = 'CHANGELOG.md'


# --- 2. 辅助函数 ---

def get_first_commit_hash():
    """获取仓库的第一个提交的哈希值"""
    try:
        # UTF-8 编码
        return subprocess.check_output('git rev-list --max-parents=0 HEAD', shell=True, text=True, encoding='utf-8').strip()
    except subprocess.CalledProcessError:
        return None

def is_valid_ref(ref):
    """检查给定的引用 (ref) 是否存在于 Git 仓库中"""
    if not ref:
        return False
    try:
        subprocess.check_output(f'git rev-parse --verify {ref}', shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL)
        return True
    except subprocess.CalledProcessError:
        return False


def generate_changelog(version_title, previous_tag):
    """
    根据给定的版本标题和对比标签生成更新日志内容。
    """

    # 1. 验证对比标签
    if previous_tag and not is_valid_ref(previous_tag):
        print(f"警告：指定的对比标签 '{previous_tag}' 不存在或无效，将从第一个 Commit 开始比较。")
        previous_tag = None

    # 2. 确定 Git Log 范围
    if not previous_tag:
        # 如果未找到上一个标签，则查找初始 Commit 作为起始点
        initial_commit = get_first_commit_hash()

        if initial_commit:
            # 比较范围是 [initial_commit]...HEAD (当前 Commit)
            range_str = f"{initial_commit}...HEAD"
            print(f"警告：从仓库的第一个提交 [{initial_commit}] 开始计算日志。")
        else:
            # 如果连第一个 Commit 都找不到（例如仓库太新或深度问题），直接用 HEAD (全部历史)
            range_str = "HEAD"
            print("警告：无法确定初始提交，将尝试获取完整的提交历史。")

    else:
        # 比较范围是 [previous_tag]...HEAD (当前 Commit)
        range_str = f"{previous_tag}...HEAD"

    print(f"正在使用版本标题 '{version_title}'，从提交范围 [{range_str}] 生成更新日志...")

    # 3. 执行 git log 获取提交信息
    log_format = '%H|||%s|||%an'
    try:
        # 明确指定 UTF-8 编码
        logs_output = subprocess.check_output(f'git log --pretty=format:"{log_format}" {range_str}', shell=True, text=True, encoding='utf-8').strip()
        logs = logs_output.split('\n')
    except subprocess.CalledProcessError as e:
        # 如果 Git Log 失败，打印错误但不退出
        print(f"执行 git log 失败或范围内无提交，Git 错误输出: {e.stderr}")
        logs = []

    # 4. 解析并分类提交 (逻辑保持不变)
    categories = {}
    commit_regex = re.compile(r'^(\w+)(?:\([^)]+\))?: (.*)')

    for log in logs:
        if not log: continue
        try:
            hash, subject, author_name = log.split('|||')
        except ValueError:
            continue

        match = commit_regex.match(subject)

        description = subject
        category_title = OTHER_CATEGORY

        if match:
            type_prefix = match.group(1)
            description = match.group(2)

            if type_prefix in EXCLUDED_TYPES:
                continue

            category_title = COMMIT_TYPES.get(type_prefix, OTHER_CATEGORY)
        else:
            is_excluded = False
            for excluded_prefix in EXCLUDED_TYPES:
                if subject.startswith(excluded_prefix + ':'):
                    is_excluded = True
                    break

            if is_excluded or subject.startswith('Merge '):
                continue

        if category_title not in categories:
            categories[category_title] = []

        categories[category_title].append(f"- {description}")

    # 5. 生成新的 Markdown 内容
    new_changelog = f"## {version_title}\n\n"

    ordered_titles = [
        COMMIT_TYPES.get('feat'),
        COMMIT_TYPES.get('fix'),
        COMMIT_TYPES.get('improve'),
        COMMIT_TYPES.get('perf'),
        COMMIT_TYPES.get('docs'),
        OTHER_CATEGORY
    ]

    for title in ordered_titles:
        if title in categories and categories[title]:
            new_changelog += f"### {title}\n\n"
            new_changelog += '\n'.join(categories[title]) + '\n\n'

    if new_changelog.strip() == f"## {version_title}":
        print("警告: 范围内没有可添加到更新日志的有效提交。")
        return

    # 6. 直接写入新的更新日志
    final_content = new_changelog

    with open(CHANGELOG_PATH, 'w', encoding='utf-8') as f:
        f.write(final_content)

    print(f"CHANGELOG.md 已成功更新，版本: {version_title}")


# --- 3. 命令行入口 ---

if __name__ == '__main__':
    # 【最终修正】增加 try/except 块来捕获所有未处理的异常，并将其打印到 stdout
    try:
        if len(sys.argv) < 2:
            print("致命错误：缺少必要的参数。")
            print("用法: python generate_changelog.py <版本标题> [对比标签]")
            sys.exit(1)

        version_title = sys.argv[1]
        previous_tag = sys.argv[2] if len(sys.argv) > 2 else None

        generate_changelog(version_title, previous_tag)

    except Exception as e:
        import traceback
        # 打印详细堆栈信息到标准输出
        print("\n--- 致命异常追踪 START ---")
        traceback.print_exc(file=sys.stdout)
        print("--- 致命异常追踪 END ---\n")
        # 确保它返回非零代码
        sys.exit(1)
