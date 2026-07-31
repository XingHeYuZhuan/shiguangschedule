#!/bin/bash

# 配置信息
REPO_APP="XingHeYuZhuan/shiguangschedule"
REPO_JIAOWU="XingHeYuZhuan/shiguang_warehouse"

API_URL_APP="https://api.github.com/repos/${REPO_APP}/contributors"
API_URL_JIAOWU="https://api.github.com/repos/${REPO_JIAOWU}/contributors"

CONTRIBUTORS_BASE_DIR="./shared/src/commonMain/composeResources/files/contributors_data"
OUTPUT_JSON_FILE="${CONTRIBUTORS_BASE_DIR}/contributors.json"
AVATAR_DIR="${CONTRIBUTORS_BASE_DIR}/avatars"

# 隔离列表 定义要排除的贡献者用户名 (login)
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

    local PAGE=1
    local PER_PAGE=100
    local RAW_DATA="[]"

    echo "--- 正在从 [${REPO_NAME}] 获取全部贡献者列表... ---" >&2

    # 分页循环抓取
    while true; do
        echo "正在读取第 ${PAGE} 页..." >&2

        local PAGE_DATA
        PAGE_DATA=$(curl -s "${API_URL}?per_page=${PER_PAGE}&page=${PAGE}")

        if [ -z "$PAGE_DATA" ] || ! echo "$PAGE_DATA" | jq -e 'if type == "array" then true else false end' >/dev/null 2>&1; then
            local ERR_MSG=$(echo "$PAGE_DATA" | jq -r '.message? // "未知错误/格式非法"')
            echo "错误: 无法从 ${REPO_NAME} 获取第 ${PAGE} 页数据 (原因: ${ERR_MSG})，终止此仓库抓取。" >&2
            break
        fi

        # 2. 如果当前页返回的是空数组 []，说明已经抓取完毕
        local DATA_LENGTH=$(echo "$PAGE_DATA" | jq '. | length')
        if [ "$DATA_LENGTH" -eq 0 ]; then
            break
        fi

        # 将当前页的数据合并到总数据中
        RAW_DATA=$(jq -n --argjson base "$RAW_DATA" --argjson new "$PAGE_DATA" '$base + $new')

        # 如果当前页返回的数据少于 per_page，说明这已经是最后一页了，提前退出
        if [ "$DATA_LENGTH" -lt "$PER_PAGE" ]; then
            break
        fi

        ((PAGE++))
    done

    if [ "$RAW_DATA" = "[]" ]; then
        echo "警告: 没有从 ${REPO_NAME} 获取到任何数据。" >&2
        echo "[]"
        return
    fi

    # 1. 过滤和格式化最终结构
    local FINAL_LIST
    FINAL_LIST=$(echo "$RAW_DATA" | jq --argjson EXCLUDES "$EXCLUDE_JSON" -c '
        map(select((.login | IN($EXCLUDES[])) | not))
        |
        map({
            name: .login,
            url: .html_url,
            avatar: ("avatars/" + (.id | tostring) + ".png")
        })
    ')

    echo "--- 成功获取数据，正在下载头像... ---" >&2

    # 使用过滤后的列表下载头像
    echo "$FINAL_LIST" | jq -c '.[]' | while read -r contributor; do
        local ID=$(echo "$contributor" | jq -r '(.avatar | sub("avatars/"; "") | sub(".png"; "") )')
        local LOGIN=$(echo "$contributor" | jq -r '.name')

        # 从原始总数据中匹配出对应的 avatar_url
        local AVATAR_URL_BASE=$(echo "$RAW_DATA" | jq -r ".[] | select(.login == \"$LOGIN\") | .avatar_url")
        local AVATAR_URL_RESIZED="${AVATAR_URL_BASE}&s=160"
        local FINAL_FILE="${AVATAR_DIR}/${ID}.png"

        if [ ! -f "${FINAL_FILE}" ]; then
            echo "    [下载] ${ID}.png" >&2
            curl -s -o "${FINAL_FILE}" "${AVATAR_URL_RESIZED}"
        fi
    done

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