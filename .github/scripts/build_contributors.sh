#!/bin/bash

# 配置信息
REPO_APP="XingHeYuZhuan/shiguangschedule"
REPO_JIAOWU="XingHeYuZhuan/shiguang_warehouse"

API_URL_APP="https://api.github.com/repos/${REPO_APP}/contributors"
API_URL_JIAOWU="https://api.github.com/repos/${REPO_JIAOWU}/contributors"

CONTRIBUTORS_BASE_DIR="./app/src/main/assets/contributors_data"
OUTPUT_JSON_FILE="${CONTRIBUTORS_BASE_DIR}/contributors.json"
AVATAR_DIR="${CONTRIBUTORS_BASE_DIR}/avatars"

# 隔离列表 定义要排除的贡献者用户名 (login)，例如：CI/CD bot、AI 工具等
EXCLUDE_USERS_ARRAY=(
    "github-actions"
    "dependabot[bot]"
    "renovate[bot]"
    "some-ai-bot"
)

EXCLUDE_JSON=$(printf "%s\n" "${EXCLUDE_USERS_ARRAY[@]}" | jq -R . | jq -s .)

# 依赖检查和初始化
command -v curl >/dev/null || { echo "错误: 需要'curl'工具" >&2; exit 1; }
command -v jq >/dev/null || { echo "错误: 需要'jq'工具来解析 JSON" >&2; exit 1; }

echo "--- 正在初始化目录和文件... ---" >&2

rm -rf "${CONTRIBUTORS_BASE_DIR}"
mkdir -p "${AVATAR_DIR}"

# 核心处理函数
fetch_and_process_repo() {
    local API_URL="$1"
    local REPO_NAME="$2"

    echo "--- 正在从 [${REPO_NAME}] 获取贡献者列表... ---" >&2
    DATA=$(curl -s "${API_URL}")

    if [ -z "$DATA" ]; then
        echo "警告: 无法从 ${REPO_NAME} 获取数据，跳过。" >&2
        echo "[]"
        return
    fi

    # 1. 格式化数据并过滤，将纯净的 JSON 数组存储在变量中
    FINAL_LIST=$(echo "$DATA" | jq --argjson EXCLUDES "$EXCLUDE_JSON" -c '
        # 过滤和映射为最终结构
        map(select((.login | IN($EXCLUDES[])) | not))
        |
        map({
            name: .login,
            url: .html_url,
            avatar: ("avatars/" + (.id | tostring) + ".png")
        })
    ')

    echo "--- 成功获取数据，正在下载头像... ---" >&2

    # 2. 使用纯净的 FINAL_LIST 变量来驱动下载循环
    echo "$FINAL_LIST" | jq -c '.[]' | while read -r contributor; do
        ID=$(echo "$contributor" | jq -r '(.avatar | sub("avatars/"; "") | sub(".png"; "") )')
        LOGIN=$(echo "$contributor" | jq -r '.name')

        AVATAR_URL_BASE=$(echo "$DATA" | jq -r ".[] | select(.login == \"$LOGIN\") | .avatar_url")
        AVATAR_URL_RESIZED="${AVATAR_URL_BASE}&s=160"
        FINAL_FILE="${AVATAR_DIR}/${ID}.png"

        if [ ! -f "${FINAL_FILE}" ]; then
            echo "    [下载] ${ID}.png" >&2
            curl -s -o "${FINAL_FILE}" "${AVATAR_URL_RESIZED}"
        fi
    done

    # 3. 将变量中的纯净 JSON 数组输出到 stdout 作为函数返回值
    echo "$FINAL_LIST"
}

# 执行流程和 JSON 合并

# 捕获纯净的 JSON 数组 (stdout)
APP_DEV_LIST=$(fetch_and_process_repo "${API_URL_APP}" "App 开发")

# 捕获纯净的 JSON 数组 (stdout)
JIAOWU_ADAPTER_LIST=$(fetch_and_process_repo "${API_URL_JIAOWU}" "教务适配")

# 使用单一的 jq 命令安全构造最终 JSON 对象
jq -n \
    --argjson app_dev "$APP_DEV_LIST" \
    --argjson jiaowu_adapter "$JIAOWU_ADAPTER_LIST" \
    '{
        app_dev: $app_dev,
        jiaowu_adapter: $jiaowu_adapter
    }' > "${OUTPUT_JSON_FILE}"

echo "--- 任务完成，数据已安全生成至 ${OUTPUT_JSON_FILE}。 ---" >&2