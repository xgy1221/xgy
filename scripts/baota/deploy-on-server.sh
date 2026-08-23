#!/usr/bin/env bash
# 在宝塔服务器本机执行：部署学管云（Web 管理端 + Java API）
# 前置：已装宝塔，并装好 Nginx / MySQL 8 / Redis / JDK 21
#
# 用法（在仓库根目录）:
#   sudo bash scripts/baota/deploy-on-server.sh
#
# 可选环境变量（部署前 export）:
#   SITE_ROOT=/www/wwwroot/xueguan-admin
#   APP_DIR=/www/wwwroot/xueguan-app
#   SERVER_NAME=192.168.50.4
#   DB_NAME=xgy DB_USER=xgy DB_PASSWORD=... JWT_SECRET=...
#   SKIP_BUILD=1          # 若已有 jar 与 web-admin/dist 可跳过构建
#   SKIP_MYSQL=1          # 跳过建库（库已存在）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SITE_ROOT="${SITE_ROOT:-/www/wwwroot/xueguan-admin}"
APP_DIR="${APP_DIR:-/www/wwwroot/xueguan-app}"
SERVER_NAME="${SERVER_NAME:-192.168.50.4}"
DB_NAME="${DB_NAME:-xgy}"
DB_USER="${DB_USER:-xgy}"
DB_PASSWORD="${DB_PASSWORD:-xgy123}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 32 2>/dev/null || echo 'change-me-please-use-a-long-random-secret-32bytes')}"
SERVER_PORT="${SERVER_PORT:-8080}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
SKIP_BUILD="${SKIP_BUILD:-0}"
SKIP_MYSQL="${SKIP_MYSQL:-0}"

RED='\033[0;31m'; GRN='\033[0;32m'; YLW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GRN}[OK]${NC} $*"; }
warn() { echo -e "${YLW}[!!]${NC} $*"; }
die()  { echo -e "${RED}[ERR]${NC} $*"; exit 1; }

[[ "$(id -u)" -eq 0 ]] || die "请用 root 执行（sudo bash $0）"

echo "=== 学管云宝塔部署 ==="
echo "仓库: $ROOT"
echo "站点: $SITE_ROOT"
echo "应用: $APP_DIR"
echo "域名/IP: $SERVER_NAME"
echo

# ---- 依赖检查 ----
need_cmds=(java nginx)
for c in "${need_cmds[@]}"; do
  command -v "$c" >/dev/null 2>&1 || die "缺少命令: $c（请在宝塔安装对应软件）"
done
java -version 2>&1 | head -1
JAVA_VER="$(java -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')"
if [[ "${JAVA_VER:-0}" -lt 21 ]]; then
  die "需要 JDK 21+，当前 major=${JAVA_VER:-unknown}"
fi
ok "JDK 可用"

if ! command -v redis-cli >/dev/null 2>&1; then
  warn "未找到 redis-cli，请确认 Redis 已安装并启动"
else
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping 2>/dev/null | grep -qi pong \
    && ok "Redis 可达" \
    || warn "Redis ping 失败，请启动 Redis"
fi

# ---- MySQL ----
if [[ "$SKIP_MYSQL" != "1" ]]; then
  if command -v mysql >/dev/null 2>&1; then
    echo "尝试创建数据库 ${DB_NAME} / 用户 ${DB_USER} ..."
    # 优先用宝塔 root 无密码本机 socket；失败则提示手动建库
    if mysql -uroot -e "SELECT 1" >/dev/null 2>&1; then
      mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
      ok "MySQL 库/用户已就绪"
    else
      warn "无法用 mysql -uroot 免密登录。请在宝塔「数据库」手动建库后设 SKIP_MYSQL=1 重跑"
      warn "  库名=${DB_NAME} 用户=${DB_USER} 密码=${DB_PASSWORD} 字符集=utf8mb4"
    fi
  else
    warn "未找到 mysql 客户端，请手动在宝塔建库"
  fi
fi

# ---- 构建 ----
mkdir -p "$APP_DIR" "$SITE_ROOT"
JAR_SRC=""
DIST_SRC=""

if [[ "$SKIP_BUILD" == "1" ]]; then
  JAR_SRC="$(ls -1 "$ROOT"/backend/target/xueguan-yun-*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true)"
  DIST_SRC="$ROOT/web-admin/dist"
  [[ -n "$JAR_SRC" && -f "$JAR_SRC" ]] || die "SKIP_BUILD=1 但找不到 backend/target/xueguan-yun-*.jar"
  [[ -d "$DIST_SRC" ]] || die "SKIP_BUILD=1 但找不到 web-admin/dist"
else
  command -v mvn >/dev/null 2>&1 || die "需要 mvn（或改用 SKIP_BUILD=1 并预先上传 jar）"
  command -v npm >/dev/null 2>&1 || die "需要 npm（或改用 SKIP_BUILD=1 并预先上传 dist）"
  echo "构建后端..."
  (cd "$ROOT/backend" && mvn -DskipTests package)
  JAR_SRC="$(ls -1 "$ROOT"/backend/target/xueguan-yun-*.jar | grep -v '\.original$' | head -1)"
  echo "构建前端..."
  (cd "$ROOT/web-admin" && npm ci && npm run build)
  DIST_SRC="$ROOT/web-admin/dist"
fi
ok "产物就绪: $JAR_SRC"
ok "产物就绪: $DIST_SRC"

# ---- 发布文件 ----
cp -f "$JAR_SRC" "$APP_DIR/xueguan-yun.jar"
rsync -a --delete "$DIST_SRC"/ "$SITE_ROOT"/
ok "已复制 jar -> $APP_DIR/xueguan-yun.jar"
ok "已同步静态资源 -> $SITE_ROOT"

# ---- 运行环境 ----
ENV_FILE="$APP_DIR/xueguan.env"
cat > "$ENV_FILE" <<EOF
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=${SERVER_PORT}
DB_URL=jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
REDIS_HOST=${REDIS_HOST}
REDIS_PORT=${REDIS_PORT}
JWT_SECRET=${JWT_SECRET}
SMS_DEMO_CODE=123456
EOF
chmod 600 "$ENV_FILE"
ok "环境文件: $ENV_FILE"

# ---- systemd ----
UNIT=/etc/systemd/system/xueguan-yun.service
cat > "$UNIT" <<EOF
[Unit]
Description=Xueguan Yun API
After=network.target mysql.service redis.service
Wants=network.target

[Service]
Type=simple
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/java -jar ${APP_DIR}/xueguan-yun.jar
Restart=on-failure
RestartSec=5
User=root
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable xueguan-yun.service
systemctl restart xueguan-yun.service
sleep 3
if systemctl is-active --quiet xueguan-yun.service; then
  ok "systemd 服务 xueguan-yun 已启动"
else
  warn "服务未处于 active，请查看: journalctl -u xueguan-yun -n 80 --no-pager"
fi

# ---- Nginx ----
NGINX_CONF="/www/server/panel/vhost/nginx/xueguan-admin.conf"
# 若无宝塔 vhost 目录，退回 sites-available 风格
if [[ ! -d "$(dirname "$NGINX_CONF")" ]]; then
  NGINX_CONF="/etc/nginx/conf.d/xueguan-admin.conf"
fi
mkdir -p "$(dirname "$NGINX_CONF")"
cat > "$NGINX_CONF" <<EOF
server {
    listen 80;
    server_name ${SERVER_NAME};

    root ${SITE_ROOT};
    index index.html;

    client_max_body_size 32m;

    location /api/ {
        proxy_pass http://127.0.0.1:${SERVER_PORT}/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    access_log /www/wwwlogs/xueguan-admin.log;
    error_log  /www/wwwlogs/xueguan-admin.error.log;
}
EOF
mkdir -p /www/wwwlogs 2>/dev/null || true
nginx -t && nginx -s reload
ok "Nginx 已写入并重载: $NGINX_CONF"

# ---- 健康检查 ----
sleep 2
API_CODE="$(curl -sS -m 5 -o /dev/null -w '%{http_code}' "http://127.0.0.1:${SERVER_PORT}/api/ping" 2>/dev/null || echo 000)"
WEB_CODE="$(curl -sS -m 5 -o /dev/null -w '%{http_code}' "http://127.0.0.1/" -H "Host: ${SERVER_NAME}" 2>/dev/null || echo 000)"
echo
echo "本机探测: API /api/ping -> ${API_CODE} ; Web / -> ${WEB_CODE}"
echo
ok "部署脚本执行完成"
cat <<EOF

访问地址（局域网）:
  http://${SERVER_NAME}/

演示登录（短信码）:
  手机号 13800000003  验证码 123456

若页面打不开:
  1) 确认本机与服务器同一局域网
  2) 宝塔防火墙 / 系统防火墙放行 80
  3) journalctl -u xueguan-yun -n 100 --no-pager
  4) nginx -t && tail -50 /www/wwwlogs/xueguan-admin.error.log

宝塔面板打不开请另跑:
  sudo bash scripts/baota/diagnose-bt-panel.sh
EOF
