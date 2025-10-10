# generate_changelog.py

import subprocess
import os
import re
import sys 
from datetime import date

# --- 1. 常量与配置 ---

COMMIT_TYPES = {
    'feat': '✨ 新增功能 (Features)',
    'fix': '🐛 Bug 修复 (Bug Fixes)',
    'perf': '🚀 性能与代码改进 (Improvements)',
    'refactor': '🚀 性能与代码改进 (Improvements)',
    'style': '🚀 性能与代码改进 (Improvements)',
    'docs': '📚 文档更新 (Documentation)',
}

# 维护性提交将被排除在更新日志之外
EXCLUDED_TYPES = ['chore', 'ci', 'build', 'test']
OTHER_CATEGORY = '🚧 其他提交 (Other Commits)'
# 默认路径是当前执行目录，通常为仓库根目录
CHANGELOG_PATH = 'CHANGELOG.md'


# --- 2. 辅助函数 ---

def get_first_commit_hash():
    """获取仓库的第一个提交的哈希值"""
    try:
        # 修正：明确指定 UTF-8 编码
        return subprocess.check_output('git rev-list --max-parents=0 HEAD', shell=True, text=True, encoding='utf-8').strip()
    except subprocess.CalledProcessError:
        print("错误：无法获取初始提交哈希。请确保在 Git 仓库中运行。")
        sys.exit(1)

def is_valid_ref(ref):
    """检查给定的引用 (ref) 是否存在于 Git 仓库中"""
    if not ref:
        return False
    try:
        # 修正：明确指定 UTF-8 编码
        subprocess.check_output(f'git rev-parse --verify {ref}', shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL)
        return True
    except subprocess.CalledProcessError:
        return False


def generate_changelog(version_title, previous_tag):
    """
    根据给定的版本标题和对比标签生成更新日志内容。

    参数:
    - version_title (str): 用于日志标题（##）的版本号字符串。
    - previous_tag (str): Git 日志比较的起始标签或 Commit 哈希。
    """
    
    # 验证对比标签
    if previous_tag and not is_valid_ref(previous_tag):
        print(f"警告：指定的对比标签 '{previous_tag}' 不存在或无效，将从第一个 Commit 开始比较。")
        previous_tag = None 
    
    if not previous_tag:
        previous_tag = get_first_commit_hash()
    
    # 核心变化：比较范围是 [previous_tag]...HEAD (当前 Commit)
    range_str = f"{previous_tag}...HEAD"
    print(f"正在使用版本标题 '{version_title}'，从提交范围 [{range_str}] 生成更新日志...")

    # 2. 执行 git log 获取提交信息
    log_format = '%H|||%s|||%an'
    try:
        # 修正：明确指定 UTF-8 编码
        logs_output = subprocess.check_output(f'git log --pretty=format:"{log_format}" {range_str}', shell=True, text=True, encoding='utf-8').strip()
        logs = logs_output.split('\n')
    except subprocess.CalledProcessError as e:
        print(f"执行 git log 失败或范围内无提交: {e}")
        logs = []

    # 3. 解析并分类提交
    categories = {}
    # 约定式提交正则：type(scope): description
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
            
            # 约定式提交：直接检查并排除
            if type_prefix in EXCLUDED_TYPES:
                continue

            category_title = COMMIT_TYPES.get(type_prefix, OTHER_CATEGORY)
        else:
            # 非约定式提交：增强排除逻辑，捕获 'ci:xxx' 这种缺少空格的格式
            is_excluded = False
            
            # 检查是否以被排除的前缀（如 'ci:' 或 'chore:'）开头
            for excluded_prefix in EXCLUDED_TYPES:
                # 捕获 'ci:优化...' 这种不规范但仍是维护性的提交
                if subject.startswith(excluded_prefix + ':'):
                    is_excluded = True
                    break

            # 如果是 Merge PR 提交或属于被排除的前缀，则跳过
            if is_excluded or subject.startswith('Merge '):
                continue
            
        if category_title not in categories:
            categories[category_title] = []

        categories[category_title].append(f"- {description} (@{author_name})")

    # 4. 生成新的 Markdown 内容
    # 标题只使用版本号
    new_changelog = f"## {version_title}\n\n" 

    # 按照定义的顺序输出分类
    ordered_titles = [
        COMMIT_TYPES['feat'], 
        COMMIT_TYPES['fix'], 
        COMMIT_TYPES['perf'], 
        COMMIT_TYPES['docs'], 
        OTHER_CATEGORY
    ]
    
    for title in ordered_titles:
        if title in categories and categories[title]:
            new_changelog += f"### {title}\n\n"
            new_changelog += '\n'.join(categories[title]) + '\n\n'
            
    if new_changelog.strip() == f"## {version_title}": # 匹配警告信息
        print("警告: 范围内没有可添加到更新日志的有效提交。")
        return

    # 5. 直接写入新的更新日志，覆盖旧内容 (覆盖模式)
    final_content = new_changelog
        
    # 注意：使用 'w' 模式打开文件会自动覆盖现有内容
    with open(CHANGELOG_PATH, 'w', encoding='utf-8') as f:
        f.write(final_content)
    
    print(f"CHANGELOG.md 已成功更新，版本: {version_title}")


# --- 3. 命令行入口 ---

if __name__ == '__main__':
    # 检查参数数量：至少需要 版本标题
    if len(sys.argv) < 2:
        print("错误：缺少必要的参数。")
        print("用法: python generate_changelog.py <版本标题> [对比标签]")
        sys.exit(1)
        
    version_title = sys.argv[1] # <version_title> e.g. v1.0.0-dev
    previous_tag = sys.argv[2] if len(sys.argv) > 2 else None # [previous_tag] e.g. dev/v0.9.9
    
    generate_changelog(version_title, previous_tag)