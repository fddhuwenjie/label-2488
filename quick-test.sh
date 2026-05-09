#!/usr/bin/env bash
# ============================================================
# README 快速验证（curl 示例）一键脚本
# ============================================================

set -uo pipefail

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# 检测可用的 python 命令（兼容 python3 / python）
PY=""
if command -v python3 &>/dev/null; then
  PY="python3"
elif command -v python &>/dev/null; then
  PY="python"
else
  echo -e "${RED}错误：未找到 python3 或 python，请先安装 Python${NC}"
  exit 1
fi

echo ""

# 1. 管理员登录，获取 JWT 令牌
echo -e "${CYAN}# 1. 管理员登录，获取 JWT 令牌${NC}"
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  "$PY" -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

if [ -z "$TOKEN" ] || [ "$TOKEN" = "None" ]; then
  echo -e "${RED}管理员登录失败，请确认服务已启动${NC}"
  exit 1
fi
echo -e "${GREEN}✓ 登录成功，Token: ${TOKEN:0:40}...${NC}\n"

# 2. 使用令牌查询图书列表
echo -e "${CYAN}# 2. 使用令牌查询图书列表${NC}"
curl -s -X GET "$BASE_URL/api/books?pageNum=1&pageSize=5" \
  -H "Authorization: Bearer $TOKEN" | "$PY" -m json.tool
echo ""

# 3. 普通用户借阅图书（bookId=1）
echo -e "${CYAN}# 3. 普通用户登录${NC}"
USER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}' | \
  "$PY" -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo -e "${GREEN}✓ user1 登录成功${NC}\n"

echo -e "${CYAN}# 4. 普通用户借阅图书（bookId=1）${NC}"
curl -s -X POST "$BASE_URL/api/borrows" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1}' | "$PY" -m json.tool

echo -e "\n${GREEN}══ 快速验证完毕 ══${NC}"
