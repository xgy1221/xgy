# 宝塔面板部署（推荐）

不使用 Docker。用宝塔安装 **MySQL、Redis、Nginx、JDK 21**，再用仓库脚本一键部署即可。

目标内网示例：`http://192.168.50.4/`（Web）+ 本机 `8080`（仅反代，不对公网暴露）。

## 0. 一键部署（推荐）

在 **宝塔所在服务器** 上（SSH 登录 `192.168.50.4` 后）执行：

```bash
# 1) 拉取代码（任选一种）
cd /www/wwwroot
git clone https://github.com/xgy1221/xgy.git xueguan && cd xueguan
git checkout cursor/baota-deploy-192-1152   # 或合并后的主分支名

# 2) 宝塔软件商店先装好：Nginx、MySQL 8、Redis、JDK 21（或本机 Temurin 21）
#    以及编译用：Maven、Node 20+（若用 SKIP_BUILD 上传产物可省略）

# 3) 部署（会建库、打 jar、构建前端、写 systemd、写 Nginx）
sudo bash scripts/baota/deploy-on-server.sh
```

常用覆盖：

```bash
sudo DB_PASSWORD='强密码' JWT_SECRET="$(openssl rand -hex 32)" \
  SERVER_NAME=192.168.50.4 \
  bash scripts/baota/deploy-on-server.sh
```

若本机已有 `backend/target/*.jar` 与 `web-admin/dist/`：

```bash
sudo SKIP_BUILD=1 SERVER_NAME=192.168.50.4 bash scripts/baota/deploy-on-server.sh
```

部署后访问：`http://192.168.50.4/`  
演示登录：手机号 `13800000003`，验证码 `123456`。

Nginx 模板也在：`deploy/nginx/xueguan-admin.conf`。  
环境变量示例：`scripts/baota/xueguan.env.example`。

> **说明：** Cursor Cloud Agent 跑在公网云主机上，**无法直接 SSH / 访问你的局域网 `192.168.50.4`**。  
> 部署必须在能连上该内网 IP 的机器上执行上述脚本（或把仓库拷到服务器后执行）。

## 1. 宝塔软件

| 软件 | 建议版本 | 用途 |
|------|----------|------|
| Nginx | 任意稳定版 | 反代 API + 托管 Web 静态资源 |
| MySQL | 8.0 | 业务库 |
| Redis | 7.x | 会话黑名单 / 登录限流 |
| JDK | 21 | 跑 Spring Boot jar |
| Maven / Node | 构建时需要 | 也可在别的机器编好再 `SKIP_BUILD=1` |

也可用宝塔「Java 项目管理器 / Supervisor」代替脚本里的 systemd。

## 2. 建库

宝塔 → 数据库 → 添加：

- 库名：`xgy`（可改，与配置一致即可）  
- 用户 / 密码：自定，**不要用演示弱密码上生产**

字符集：`utf8mb4`。一键脚本在能 `mysql -uroot` 时会自动建库。

## 3. 配置后端

复制并改环境变量（推荐），或改 `application.yml`：

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/xgy?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USER='你的库用户'
export DB_PASSWORD='你的库密码'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export JWT_SECRET='请换成至少32字节的随机串'
export SMS_DEMO_CODE=''   # 生产接真短信后留空并改校验逻辑
export SERVER_PORT=8080
```

首次启动会跑 Flyway，自动建表并写入演示数据。

打包：

```bash
cd backend
mvn -DskipTests package
# 产物：target/xueguan-yun-*.jar
```

运行示例：

```bash
java -jar target/xueguan-yun-1.0.0-SNAPSHOT.jar
```

## 4. 构建 Web 管理端

```bash
cd web-admin
npm ci
npm run build
# 产物在 dist/
```

把 `dist/` 上传到站点目录，例如 `/www/wwwroot/xueguan-admin/`。

## 5. Nginx 反代（示例）

同域 IP / 域名 + `/api` 反代（内网示例）：

```nginx
server {
    listen 80;
    server_name 192.168.50.4;

    root /www/wwwroot/xueguan-admin;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

完整文件见 `deploy/nginx/xueguan-admin.conf`。申请 SSL：宝塔站点 → 证书（内网 IP 一般用 HTTP 即可）。

## 6. CORS

同域反代时浏览器走相对路径 `/api`，通常无需额外 CORS。若前后端不同源，在 `application.yml` 增加：

```yaml
app:
  security:
    cors-allowed-origins:
      - http://192.168.50.4
      - https://admin.example.com
```

仓库默认已包含 `http://192.168.50.4`。

## 7. 小程序

- `miniprogram/services/api.js` 的 `baseUrl` 改为可访问的 API 地址（内网调试可用 `http://192.168.50.4`）  
- 微信公众平台配置 request 合法域名（真机正式环境需 HTTPS 公网域名）  
- 演示码仅开发用；正式接短信服务商  

## 8. 上线检查清单

- [ ] `JWT_SECRET` 已换强随机  
- [ ] MySQL / Redis 密码非默认  
- [ ] CORS 只有正式管理端域名（公网时）  
- [ ] 固定短信码已关闭（公网时）  
- [ ] Nginx 仅反代必要路径，Jar 不直接对公网  
- [ ] 宝塔防火墙：对外 80/443，8080 仅本机；面板端口仅管理网段  

## 9. 宝塔面板主页打不开（排障）

已知历史入口：

```text
https://192.168.50.4:29846/site/php
```

说明：

- 端口是 **29846**（不是默认 8888），协议是 **HTTPS**
- `/site/php` 是登录后的「网站 / PHP」页面；若整站都进不去，应先恢复面板登录页，而不是死磕这个路径
- Cloud Agent 从公网探测该地址会 **超时**；请在与服务器同一局域网的电脑上测，或 SSH 进服务器本机测

在服务器 SSH 执行：

```bash
sudo bash scripts/baota/diagnose-bt-panel.sh
```

针对本机端口的快速命令：

```bash
bt default
cat /www/server/panel/data/port.pl          # 当前端口，历史为 29846
cat /www/server/panel/data/admin_path.pl    # 安全入口（登录必须带）
ls /www/server/panel/data/ssl.pl            # 存在则必须用 https://
ss -lntp | grep 29846
curl -k -I https://127.0.0.1:29846/
bt restart
```

| 现象 / 原因 | 处理 |
|-------------|------|
| 面板进程未运行 | `bt start` 或 `bt restart` |
| 端口不是 29846 了 | `bt default` / `port.pl` 看新端口，改收藏夹 |
| 缺安全入口 | 只开 `https://IP:端口/` 会 404；必须带 `admin_path.pl` 里的路径 |
| `/site/php` 单独打不开 | 先打开登录页登录；未登录直接进子页面会失败 |
| HTTPS 证书/开关异常 | 本机 `curl -k https://127.0.0.1:29846/`；可临时关面板 SSL 再开 |
| 本机 curl 通、浏览器不通 | 放行防火墙 **TCP 29846**；确认电脑与 `192.168.50.4` 同网段 |
| 磁盘满 | `df -h`，清理后再 `bt restart` |
| 面板损坏 | 按[宝塔官方](https://www.bt.cn)修复/更新（以官网命令为准） |

**注意区分：**

- **宝塔面板**打不开 → `https://192.168.50.4:29846/` + 安全入口  
- **学管云站点**打不开 → 查 Nginx `80`、`xueguan-yun` 服务、`/www/wwwlogs/xueguan-admin.error.log`

更细的安全说明见 `docs/SECURITY.md`。
