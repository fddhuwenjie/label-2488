#!/usr/bin/env bash
# =============================================================================
# 校园图书借阅管理系统 - 一键演示脚本
# Campus Library - Quick Start & API Demo
#
# 支持平台：macOS / Linux / Windows (Git Bash / WSL)
# 功能：检测并安装依赖 → 生成 .env → 启动 Docker → 运行完整 API 测试
# =============================================================================

set -uo pipefail

# ─── 颜色常量 ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PASS=0
FAIL=0

# ─── 工具函数 ─────────────────────────────────────────────────────────────────
log_info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()      { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()    { echo -e "\n${BOLD}${CYAN}══ $* ══${NC}"; }
log_banner()  {
  echo -e "${BOLD}${CYAN}"
  echo "  ╔══════════════════════════════════════════════════╗"
  echo "  ║        校园图书借阅管理系统  Demo Script         ║"
  echo "  ║      Campus Library - Quick Start & API Test     ║"
  echo "  ╚══════════════════════════════════════════════════╝"
  echo -e "${NC}"
}

# 从 JSON 字符串中提取字段值（兼容 python3 不可用的情况）
json_val() {
  local json="$1"
  local key="$2"
  # 优先 python3，其次 python，最后 grep 回退
  local py=""
  if command -v python3 &>/dev/null; then
    py="python3"
  elif command -v python &>/dev/null; then
    py="python"
  fi
  if [ -n "$py" ]; then
    echo "$json" | "$py" -c "
import sys, json
try:
    d = json.load(sys.stdin)
    keys = '$key'.split('.')
    v = d
    for k in keys:
        v = v[k]
    print(v)
except Exception:
    print('')
" 2>/dev/null
  else
    # 简易 grep 回退（仅支持一层 key）
    echo "$json" | grep -o "\"${key}\":\"[^\"]*\"" | head -1 \
      | sed "s/\"${key}\":\"//;s/\"//"
  fi
}

# 打印 curl 测试结果
assert_ok() {
  local desc="$1"
  local http_code="$2"
  local expected="${3:-200}"
  if [ "$http_code" = "$expected" ]; then
    log_ok "[PASS] $desc (HTTP $http_code)"
    PASS=$((PASS + 1))
  else
    log_error "[FAIL] $desc (期望 HTTP ${expected}，实际 $http_code)"
    FAIL=$((FAIL + 1))
  fi
}

# ─── 1. 检测操作系统 ──────────────────────────────────────────────────────────
detect_os() {
  case "$(uname -s 2>/dev/null)" in
    Darwin)            OS="mac"     ;;
    Linux)             OS="linux"   ;;
    MINGW*|MSYS*|CYGWIN*) OS="win" ;;
    *)                 OS="unknown" ;;
  esac
  log_info "操作系统：$OS  (uname=$(uname -s 2>/dev/null || echo 'N/A'))"
}

# ─── 2. 检测 / 安装依赖 ───────────────────────────────────────────────────────
ensure_homebrew() {
  if ! command -v brew &>/dev/null; then
    log_warn "未检测到 Homebrew，正在安装..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    # Apple Silicon 路径修正
    [[ -f /opt/homebrew/bin/brew ]] && eval "$(/opt/homebrew/bin/brew shellenv)"
  fi
}

install_on_mac() {
  local pkg="$1"
  local cmd="${2:-$1}"
  if ! command -v "$cmd" &>/dev/null; then
    log_warn "$cmd 未安装，通过 Homebrew 安装 $pkg ..."
    ensure_homebrew
    brew install "$pkg"
  fi
}

check_docker() {
  if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
    log_ok "Docker 已就绪 ($(docker --version | head -1))"
    return 0
  fi

  log_warn "Docker 未运行或未安装"

  case "$OS" in
    mac)
      if ! command -v docker &>/dev/null; then
        log_info "尝试通过 Homebrew 安装 Docker Desktop..."
        ensure_homebrew
        brew install --cask docker || true
      fi
      log_info "正在启动 Docker Desktop（最多等待 60 秒）..."
      open -a Docker 2>/dev/null || true
      for i in $(seq 1 12); do
        sleep 5
        if docker info &>/dev/null 2>&1; then
          log_ok "Docker Desktop 已启动"
          return 0
        fi
        echo -n "  等待中... ($((i*5))s)"
      done
      log_error "Docker Desktop 启动超时，请手动启动后重试"
      exit 1
      ;;
    linux)
      if command -v apt-get &>/dev/null; then
        log_info "通过 apt 安装 Docker..."
        sudo apt-get update -qq
        sudo apt-get install -y docker.io docker-compose-v2
        sudo systemctl start docker
        sudo usermod -aG docker "$USER" 2>/dev/null || true
      elif command -v yum &>/dev/null; then
        log_info "通过 yum 安装 Docker..."
        sudo yum install -y docker
        sudo systemctl start docker
      else
        log_error "无法自动安装 Docker，请手动安装后重试"
        exit 1
      fi
      ;;
    win)
      log_error "Windows 请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop/"
      log_info  "安装完成并启动后，在 Git Bash 中重新运行此脚本"
      exit 1
      ;;
    *)
      log_error "无法识别的操作系统，请手动安装 Docker"
      exit 1
      ;;
  esac
}

check_compose() {
  # 优先使用 docker compose (v2)，回退到 docker-compose (v1)
  if docker compose version &>/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    log_ok "docker compose v2 可用"
  elif command -v docker-compose &>/dev/null; then
    COMPOSE_CMD="docker-compose"
    log_ok "docker-compose v1 可用"
  else
    case "$OS" in
      mac)
        install_on_mac "docker-compose" "docker-compose"
        COMPOSE_CMD="docker-compose"
        ;;
      linux)
        sudo apt-get install -y docker-compose 2>/dev/null \
          || sudo yum install -y docker-compose 2>/dev/null \
          || { log_error "无法安装 docker-compose"; exit 1; }
        COMPOSE_CMD="docker-compose"
        ;;
      win)
        log_error "请更新 Docker Desktop，它已内置 docker compose v2"
        exit 1
        ;;
    esac
  fi
}

check_curl() {
  if ! command -v curl &>/dev/null; then
    log_warn "curl 未安装，尝试自动安装..."
    case "$OS" in
      mac)   install_on_mac "curl" "curl" ;;
      linux) sudo apt-get install -y curl 2>/dev/null || sudo yum install -y curl ;;
      win)   log_error "请在 Git Bash 中运行，curl 通常随 Git for Windows 一并安装"; exit 1 ;;
    esac
  fi
  log_ok "curl 已就绪 ($(curl --version | head -1 | awk '{print $1,$2}'))"
}

check_openssl() {
  if ! command -v openssl &>/dev/null; then
    case "$OS" in
      mac)   install_on_mac "openssl" "openssl" ;;
      linux) sudo apt-get install -y openssl 2>/dev/null || true ;;
      win)   log_warn "openssl 未找到，将使用备用密钥生成方式" ;;
    esac
  fi
}

check_python() {
  if command -v python3 &>/dev/null; then
    log_ok "python3 已就绪"
  elif command -v python &>/dev/null; then
    log_ok "python 已就绪（将作为 python3 的替代）"
  else
    log_warn "python3/python 均未找到，JSON 解析将使用 grep 回退方案（结果可能不精确）"
    case "$OS" in
      mac)   install_on_mac "python3" "python3" ;;
      linux) sudo apt-get install -y python3 2>/dev/null || true ;;
    esac
  fi
}

# ─── 3. 生成 .env 文件 ────────────────────────────────────────────────────────
setup_env() {
  log_step "配置环境变量"
  cd "$SCRIPT_DIR"

  if [ -f ".env" ]; then
    log_ok ".env 已存在，跳过生成"
    # 检查 JWT_SECRET 是否还是占位符
    if grep -q "请替换为随机强密钥" .env 2>/dev/null; then
      log_warn ".env 中 JWT_SECRET 仍为占位符，正在自动替换..."
      _replace_jwt_secret
    fi
    return
  fi

  log_info "从 .env.example 生成 .env..."
  cp .env.example .env
  _replace_jwt_secret
  log_ok ".env 已生成"
}

_replace_jwt_secret() {
  local secret
  if command -v openssl &>/dev/null; then
    secret=$(openssl rand -base64 32)
  elif command -v python3 &>/dev/null; then
    secret=$(python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())")
  else
    # 固定开发用密钥（仅用于本地演示，生产环境必须替换）
    secret="Y2FtcHVzTGlicmFyeVNlY3JldEtleUZvckpXVEF1dGg="
    log_warn "使用内置演示密钥（非生产安全），建议安装 openssl 后重新运行"
  fi

  # macOS sed 与 GNU sed 语法略有差异
  if [[ "$OS" == "mac" ]]; then
    sed -i '' "s|请替换为随机强密钥|${secret}|" .env
  else
    sed -i  "s|请替换为随机强密钥|${secret}|" .env
  fi
  log_ok "JWT_SECRET 已生成并写入 .env"
}

# ─── 4. 启动 Docker 服务 ──────────────────────────────────────────────────────
start_services() {
  log_step "启动 Docker 服务"
  cd "$SCRIPT_DIR"

  log_info "执行: $COMPOSE_CMD up --build -d"
  $COMPOSE_CMD up --build -d

  log_info "等待 MySQL 就绪..."
  local timeout=120
  local elapsed=0
  while ! $COMPOSE_CMD exec -T mysql \
    mysqladmin ping -h localhost -u library -plibrary123 --silent &>/dev/null 2>&1; do
    sleep 3
    elapsed=$((elapsed + 3))
    echo -n "  MySQL 启动中... (${elapsed}s/${timeout}s)"$'\r'
    if [ "$elapsed" -ge "$timeout" ]; then
      echo ""
      log_error "MySQL 启动超时，请检查日志：$COMPOSE_CMD logs mysql"
      exit 1
    fi
  done
  echo ""
  log_ok "MySQL 已就绪"

  log_info "等待 Spring Boot 就绪（最多 120 秒）..."
  elapsed=0
  while true; do
    local health
    health=$(curl -s --connect-timeout 3 "$BASE_URL/actuator/health" 2>/dev/null || echo "")
    if echo "$health" | grep -q '"UP"'; then
      break
    fi
    sleep 5
    elapsed=$((elapsed + 5))
    echo -n "  Spring Boot 启动中... (${elapsed}s/120s)"$'\r'
    if [ "$elapsed" -ge 120 ]; then
      echo ""
      log_error "Spring Boot 启动超时，请检查日志：$COMPOSE_CMD logs backend"
      exit 1
    fi
  done
  echo ""
  log_ok "Spring Boot 已就绪 → $BASE_URL"
}

# ─── 5. API 测试套件 ──────────────────────────────────────────────────────────
run_tests() {
  log_step "开始 API 测试"
  local CODE RESP TOKEN USER_TOKEN BORROW_ID BOOK_ID

  # ── 认证模块 ──────────────────────────────────────────────────────────────

  echo -e "\n${BOLD}▌ 认证模块${NC}"

  # 注册新用户
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo_user","password":"demo123","email":"demo@test.com"}')
  CODE=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  # 200=注册成功，400=用户名已存在（重复运行时正常）
  if [ "$CODE" = "200" ] || [ "$CODE" = "400" ]; then
    log_ok "[PASS] POST /api/auth/register (HTTP $CODE)"
    PASS=$((PASS + 1))
  else
    log_error "[FAIL] POST /api/auth/register (HTTP $CODE)"
    FAIL=$((FAIL + 1))
  fi

  # 管理员登录
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
  CODE=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  assert_ok "POST /api/auth/login (admin)" "$CODE" "200"
  TOKEN=$(json_val "$BODY" "data.token")
  if [ -z "$TOKEN" ]; then
    log_error "无法提取 admin Token，后续测试将跳过"
    _print_summary; exit 1
  fi
  log_info "  Admin Token: ${TOKEN:0:40}..."

  # 普通用户登录
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"user1","password":"user123"}')
  CODE=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  assert_ok "POST /api/auth/login (user1)" "$CODE" "200"
  USER_TOKEN=$(json_val "$BODY" "data.token")

  # 无 Token 访问受保护接口 → 应返回 401
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/books")
  assert_ok "GET /api/books (无 Token → 401)" "$CODE" "401"

  # ── 用户模块 ──────────────────────────────────────────────────────────────

  echo -e "\n${BOLD}▌ 用户模块${NC}"

  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/users/me" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/users/me" "$CODE" "200"

  # 普通用户无权访问用户列表 → 403
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/users" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/users (普通用户 → 403)" "$CODE" "403"

  # 管理员可访问用户列表
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/users" \
    -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /api/users (admin)" "$CODE" "200"

  # ── 图书模块 ──────────────────────────────────────────────────────────────

  echo -e "\n${BOLD}▌ 图书模块${NC}"

  # 普通用户查询图书列表
  RESP=$(curl -s -w "\n%{http_code}" -X GET \
    "$BASE_URL/api/books?pageNum=1&pageSize=5" \
    -H "Authorization: Bearer $USER_TOKEN")
  CODE=$(echo "$RESP" | tail -1)
  assert_ok "GET /api/books?pageNum=1&pageSize=5" "$CODE" "200"

  # 关键字搜索
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/books?keyword=Spring&pageSize=3" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/books?keyword=Spring" "$CODE" "200"

  # 分类过滤（中文参数需 URL 编码，避免 Tomcat 拒绝非 ASCII 字节序列）
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    --data-urlencode "category=计算机" -G \
    "$BASE_URL/api/books" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/books?category=计算机" "$CODE" "200"

  # 普通用户新增图书 → 403
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/books" \
    -H "Authorization: Bearer $USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"title":"未授权图书","author":"测试","totalStock":1}')
  assert_ok "POST /api/books (普通用户 → 403)" "$CODE" "403"

  # 管理员新增图书
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/books" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "isbn":"978-0-demo-001",
      "title":"Demo Script 演示图书",
      "author":"自动化测试",
      "publisher":"测试出版社",
      "category":"计算机",
      "totalStock":3,
      "description":"由 demo.sh 自动创建"
    }')
  CODE=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  assert_ok "POST /api/books (admin 新增)" "$CODE" "200"
  BOOK_ID=$(json_val "$BODY" "data.id")
  log_info "  新图书 ID: $BOOK_ID"

  # 参数校验：缺少必填字段 title → 400
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/books" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"author":"测试","totalStock":1}')
  assert_ok "POST /api/books (缺 title → 400)" "$CODE" "400"

  # 查询图书详情
  if [ -n "$BOOK_ID" ] && [ "$BOOK_ID" != "None" ]; then
    CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "$BASE_URL/api/books/$BOOK_ID" \
      -H "Authorization: Bearer $USER_TOKEN")
    assert_ok "GET /api/books/$BOOK_ID" "$CODE" "200"

    # 管理员更新图书
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
      "$BASE_URL/api/books/$BOOK_ID" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"title":"Demo 图书（已更新）","author":"自动化测试","totalStock":5}')
    assert_ok "PUT /api/books/$BOOK_ID (admin 更新)" "$CODE" "200"
  fi

  # 查询不存在的图书 → 400
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/books/999999" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/books/999999 (不存在 → 400)" "$CODE" "400"

  # ── 借阅模块 ──────────────────────────────────────────────────────────────

  echo -e "\n${BOLD}▌ 借阅模块${NC}"

  # 使用已有图书 ID=1（初始化数据）
  local BORROW_BOOK_ID="1"

  # 普通用户借书
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/borrows" \
    -H "Authorization: Bearer $USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"bookId\": $BORROW_BOOK_ID}")
  CODE=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  # 200=借阅成功，400=库存不足或超限（多次运行时正常）
  if [ "$CODE" = "200" ] || [ "$CODE" = "400" ]; then
    log_ok "[PASS] POST /api/borrows (HTTP $CODE)"
    PASS=$((PASS + 1))
  else
    log_error "[FAIL] POST /api/borrows (HTTP $CODE)"
    FAIL=$((FAIL + 1))
  fi
  BORROW_ID=$(json_val "$BODY" "data.id")
  log_info "  借阅记录 ID: $BORROW_ID"

  # 查询当前用户的借阅记录
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/borrows/my" \
    -H "Authorization: Bearer $USER_TOKEN")
  assert_ok "GET /api/borrows/my" "$CODE" "200"

  # 管理员查询全部借阅记录
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/borrows" \
    -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /api/borrows (admin 全部)" "$CODE" "200"

  # 按状态过滤
  CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/borrows?status=BORROWED" \
    -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /api/borrows?status=BORROWED" "$CODE" "200"

  # 查询逾期记录（仅 admin）
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/borrows/overdue" \
    -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /api/borrows/overdue (admin)" "$CODE" "200"

  # 还书
  if [ -n "$BORROW_ID" ] && [ "$BORROW_ID" != "None" ] && [ "$BORROW_ID" != "0" ]; then
    RESP=$(curl -s -w "\n%{http_code}" -X PUT \
      "$BASE_URL/api/borrows/$BORROW_ID/return" \
      -H "Authorization: Bearer $USER_TOKEN")
    CODE=$(echo "$RESP" | tail -1)
    # 200=还书成功，400=已归还（重复运行时正常）
    if [ "$CODE" = "200" ] || [ "$CODE" = "400" ]; then
      log_ok "[PASS] PUT /api/borrows/$BORROW_ID/return (HTTP $CODE)"
      PASS=$((PASS + 1))
    else
      log_error "[FAIL] PUT /api/borrows/$BORROW_ID/return (HTTP $CODE)"
      FAIL=$((FAIL + 1))
    fi

    # 重复还书 → 400
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
      "$BASE_URL/api/borrows/$BORROW_ID/return" \
      -H "Authorization: Bearer $USER_TOKEN")
    assert_ok "PUT /api/borrows/$BORROW_ID/return (重复还书 → 400)" "$CODE" "400"
  fi

  # 下架新增的演示图书（清理）
  if [ -n "$BOOK_ID" ] && [ "$BOOK_ID" != "None" ]; then
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
      "$BASE_URL/api/books/$BOOK_ID" \
      -H "Authorization: Bearer $TOKEN")
    assert_ok "DELETE /api/books/$BOOK_ID (admin 下架)" "$CODE" "200"
  fi

  _print_summary

  # 运行 README 快速验证脚本
  if [ -f "$SCRIPT_DIR/quick-test.sh" ]; then
    log_step "README 快速验证（curl 示例）"
    bash "$SCRIPT_DIR/quick-test.sh"
  fi
}

_print_summary() {
  local total=$((PASS + FAIL))
  echo ""
  echo -e "${BOLD}══════════════════════════════════════════${NC}"
  echo -e "${BOLD}  测试结果汇总${NC}"
  echo -e "${BOLD}══════════════════════════════════════════${NC}"
  echo -e "  总计：${BOLD}$total${NC}  通过：${GREEN}${BOLD}$PASS${NC}  失败：${RED}${BOLD}$FAIL${NC}"
  echo ""
  if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}✓ 全部通过！系统运行正常${NC}"
  else
    echo -e "  ${RED}${BOLD}✗ 有 $FAIL 个用例失败，请查看上方日志${NC}"
  fi
  echo -e "${BOLD}══════════════════════════════════════════${NC}"
  echo ""
  echo -e "  API 地址：${CYAN}$BASE_URL${NC}"
  echo -e "  admin 账号：admin / admin123"
  echo -e "  用户账号：user1 / user123"
  echo ""
  echo -e "  停止服务：${YELLOW}docker-compose down${NC}"
  echo -e "  查看日志：${YELLOW}docker-compose logs -f backend${NC}"
  echo ""
}

# ─── 帮助信息 ─────────────────────────────────────────────────────────────────
show_help() {
  echo "用法: bash demo.sh [选项]"
  echo ""
  echo "  （无参数）   完整流程：检测依赖 → 启动服务 → API 测试"
  echo "  --test-only  跳过启动，只运行 API 测试（服务必须已在运行）"
  echo "  --stop       停止并移除 Docker 容器（保留数据卷）"
  echo "  --clean      停止容器并删除所有数据卷（彻底清理）"
  echo "  --help       显示此帮助"
  echo ""
}

# ─── 入口 ─────────────────────────────────────────────────────────────────────
main() {
  log_banner

  case "${1:-}" in
    --help|-h)
      show_help; exit 0 ;;
    --stop)
      log_step "停止服务"
      cd "$SCRIPT_DIR"
      detect_os; check_compose
      $COMPOSE_CMD down
      log_ok "服务已停止（数据已保留）"
      exit 0 ;;
    --clean)
      log_step "彻底清理"
      cd "$SCRIPT_DIR"
      detect_os; check_compose
      $COMPOSE_CMD down -v
      log_ok "容器与数据卷已删除"
      exit 0 ;;
    --test-only)
      log_step "仅运行 API 测试"
      run_tests
      exit 0 ;;
  esac

  # 完整流程
  log_step "1/5  检测操作系统"
  detect_os

  log_step "2/5  检查并安装依赖"
  check_curl
  check_openssl
  check_python
  check_docker
  check_compose

  log_step "3/5  配置环境变量"
  setup_env

  log_step "4/5  启动 Docker 服务"
  start_services

  log_step "5/5  API 测试"
  run_tests
}

main "$@"
