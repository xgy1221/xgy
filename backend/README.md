# 学管云 Backend

Spring Boot 3.3 · 包名 `com.xgy.cloud` · 默认端口 **8080**

## 本地启动（无 Docker）

先自备 MySQL 8 + Redis（本机或宝塔）。

```bash
cd backend
# 按需改 application.yml，或导出 DB_* / REDIS_* / JWT_SECRET
mvn spring-boot:run
```

首次启动 Flyway 自动建表并写入演示数据。演示短信码默认 `123456`。

## 打包上宝塔

```bash
mvn -DskipTests package
java -jar target/xueguan-yun-1.0.0-SNAPSHOT.jar
```

完整步骤见仓库根目录 `docs/DEPLOY_BAOTA.md`。

## 配置项（环境变量优先）

| 变量 | 含义 |
|------|------|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | MySQL |
| `REDIS_HOST` / `REDIS_PORT` | Redis |
| `JWT_SECRET` / `JWT_EXPIRE_DAYS` | 令牌 |
| `SMS_DEMO_CODE` | 演示验证码（生产勿用） |
| `SERVER_PORT` | 端口，默认 8080 |

安全默认值见 `app.security.*` 与 `docs/SECURITY.md`。API 以 Controller / `docs/API.md` 为准。
