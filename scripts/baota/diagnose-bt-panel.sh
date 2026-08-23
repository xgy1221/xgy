#!/usr/bin/env bash
# 在宝塔服务器上执行：排查面板主页打不开
# 用法：sudo bash scripts/baota/diagnose-bt-panel.sh
set -euo pipefail

RED='\033[0;31m'; GRN='\033[0;32m'; YLW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GRN}[OK]${NC} $*"; }
warn() { echo -e "${YLW}[!!]${NC} $*"; }
bad()  { echo -e "${RED}[NG]${NC} $*"; }
hr()   { echo "----------------------------------------"; }

echo "=== 宝塔面板诊断 $(date '+%F %T') ==="
hr

# 1. 是否安装
if [[ -x /etc/init.d/bt ]] || command -v bt >/dev/null 2>&1; then
  ok "检测到宝塔命令/服务脚本"
else
  bad "未找到 bt 命令或 /etc/init.d/bt —— 可能未安装宝塔，或 PATH 异常"
  echo "  安装文档: https://www.bt.cn/new/download.html"
fi

# 2. 面板进程
if pgrep -af 'BT-Panel|panel|BT-Task' >/dev/null 2>&1; then
  ok "面板相关进程在跑"
  pgrep -af 'BT-Panel|panel|BT-Task' | head -10 || true
else
  bad "未发现 BT-Panel / BT-Task 进程"
  echo "  尝试: bt start   或   /etc/init.d/bt start"
fi

# 3. 默认入口信息
hr
echo ">>> bt default（面板地址/账号，若可用）"
if command -v bt >/dev/null 2>&1; then
  bt default 2>&1 || warn "bt default 执行失败"
else
  warn "无 bt 命令，跳过 bt default"
fi

# 4. 端口监听
hr
echo ">>> 常见面板端口监听情况"
PANEL_PORTS=(8888 888 7800 443 80)
for p in "${PANEL_PORTS[@]}"; do
  if ss -lntu 2>/dev/null | grep -qE ":${p}\\b" || netstat -lntu 2>/dev/null | grep -qE ":${p}\\b"; then
    ok "端口 ${p} 正在监听"
    (ss -lntp 2>/dev/null || netstat -lntp 2>/dev/null || true) | grep -E ":${p}\\b" | head -5 || true
  else
    warn "端口 ${p} 未监听"
  fi
done

# 自定义端口：读宝塔配置
PORT_FILE="/www/server/panel/data/port.pl"
if [[ -f "$PORT_FILE" ]]; then
  CUSTOM_PORT="$(tr -d '[:space:]' < "$PORT_FILE")"
  ok "配置文件中的面板端口: ${CUSTOM_PORT} (${PORT_FILE})"
  if ss -lntu 2>/dev/null | grep -qE ":${CUSTOM_PORT}\\b"; then
    ok "自定义端口 ${CUSTOM_PORT} 已监听"
  else
    bad "配置端口 ${CUSTOM_PORT} 未监听 —— 面板可能未启动"
  fi
else
  warn "未找到 ${PORT_FILE}（面板未装完或数据目录不同）"
fi

# 5. 本机 HTTP 探测
hr
echo ">>> 本机访问探测"
PROBE_PORTS=()
[[ -f "$PORT_FILE" ]] && PROBE_PORTS+=("$(tr -d '[:space:]' < "$PORT_FILE")")
PROBE_PORTS+=(8888 80)
for p in "${PROBE_PORTS[@]}"; do
  code="$(curl -sS -m 3 -o /dev/null -w '%{http_code}' "http://127.0.0.1:${p}/" 2>/dev/null || echo '000')"
  if [[ "$code" != "000" ]]; then
    ok "http://127.0.0.1:${p}/ -> HTTP ${code}"
  else
    bad "http://127.0.0.1:${p}/ 无响应"
  fi
done

# 6. 防火墙 / 安全
hr
echo ">>> 防火墙与安全组提示"
if command -v firewall-cmd >/dev/null 2>&1 && systemctl is-active firewalld >/dev/null 2>&1; then
  warn "firewalld 运行中，请确认已放行面板端口"
  firewall-cmd --list-ports 2>/dev/null || true
elif command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -qi active; then
  warn "ufw 已启用"
  ufw status numbered 2>/dev/null | head -30 || true
else
  ok "未检测到活跃的 firewalld/ufw（仍可能有云厂商安全组拦截）"
fi

if command -v iptables >/dev/null 2>&1; then
  echo "iptables INPUT 摘要（前 20 条）:"
  iptables -L INPUT -n 2>/dev/null | head -20 || true
fi

# 7. 磁盘 / 内存
hr
echo ">>> 磁盘与内存"
df -h / /www 2>/dev/null | head -10 || df -h /
free -h | head -3

ROOT_USE="$(df -P / | awk 'NR==2{gsub(/%/,"",$5); print $5}')"
if [[ "${ROOT_USE:-0}" -ge 95 ]]; then
  bad "根分区使用率 ${ROOT_USE}% —— 磁盘满会导致面板无法打开"
fi

# 8. 常见修复建议
hr
cat <<'EOF'
>>> 常见修复步骤（在服务器 SSH 里执行）

1) 重启面板
   bt restart
   # 或: /etc/init.d/bt restart

2) 查看面板入口（端口/用户/密码/安全入口）
   bt default

3) 若端口被改过，浏览器必须带端口，例如:
   http://192.168.50.4:8888/xxxxx
   （安全入口路径也要带上，缺一不可）

4) 放行防火墙（示例，按实际端口改）
   # firewalld
   firewall-cmd --permanent --add-port=8888/tcp && firewall-cmd --reload
   # 宝塔自身: 安全 -> 防火墙 -> 放行面板端口
   # 云服务器还要在控制台安全组放行

5) 仅内网可访问时
   - 本机必须与 192.168.50.4 同一局域网
   - 云端 Agent / 公网无法直连该私网 IP

6) 面板损坏可尝试
   curl -sSO http://download.bt.cn/install/update6.sh && bash update6.sh
   # 或官方修复命令（以当前宝塔文档为准）

EOF

echo "=== 诊断结束 ==="
