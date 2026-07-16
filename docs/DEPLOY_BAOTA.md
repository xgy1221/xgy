# 宝塔面板部署（推荐）

不使用 Docker。用宝塔安装 **MySQL、Redis、Nginx、Java 项目** 即可。

## 1. 宝塔软件

| 软件 | 建议版本 | 用途 |
|------|----------|------|
| Nginx | 任意稳定版 | 反代 API + 托管 Web 静态资源 |
| MySQL | 8.0 | 业务库 |
| Redis | 7.x | 会话黑名单 / 登录限流 |
| PM2 管理器 或 Supervisor | — | 守护 Java 进程（也可用 systemd） |
| 可选：Java 项目管理器 | — | 一键跑 jar |

本机还需 **JDK 21**（宝塔「Java 项目管理器」或手动装 Temurin 21）。

## 2. 建库

宝塔 → 数据库 → 添加：

- 库名：`xgy`（可改，与配置一致即可）  
- 用户 / 密码：自定，**不要用演示弱密码上生产**

字符集：`utf8mb4`。

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

宝塔可用「Java 项目 / Supervisor / systemd」把该命令设为开机守护。

## 4. 构建 Web 管理端

```bash
cd web-admin
npm ci
npm run build
# 产物在 dist/
```

把 `dist/` 上传到站点目录，例如 `/www/wwwroot/xueguan-admin/`。

## 5. Nginx 反代（示例）

两个域名或同域路径均可。示例：同域 `admin.example.com` + `/api` 反代。

```nginx
server {
    listen 80;
    server_name admin.example.com;

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

若 API 单独域名 `api.example.com`，把 `location /` 整站反代到 `8080`，Web 构建时把请求基址改成该域名（或继续用相对路径 `/api` 并在网关聚合）。

申请 SSL：宝塔站点 → 证书。

## 6. CORS

生产在后端配置允许的管理端源，例如环境变量或 `application.yml`：

```yaml
app:
  security:
    cors-allowed-origins:
      - https://admin.example.com
```

## 7. 小程序

- `miniprogram/services/api.js` 的 `baseUrl` 改为 `https://api.example.com`（或你的 API 域名）  
- 微信公众平台配置 request 合法域名  
- 演示码仅开发用；正式接短信服务商  

## 8. 上线检查清单

- [ ] `JWT_SECRET` 已换强随机  
- [ ] MySQL / Redis 密码非默认  
- [ ] CORS 只有正式管理端域名  
- [ ] 固定短信码已关闭  
- [ ] Nginx 仅反代必要路径，Jar 不直接对公网  
- [ ] 宝塔防火墙 / 安全组：对外 80/443，8080 仅本机  

更细的安全说明见 `docs/SECURITY.md`。  
