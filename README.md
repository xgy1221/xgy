# 学管云 · 教培 SaaS（重构版）

多机构教培管理系统。设计原则：**租户与安全先行，单一数据源，现场归小程序、经营归 Web**。

> 分支 `cursor/xueguan-edu-0196` 为 orphan 重写基线。部署默认走 **宝塔面板**（MySQL / Redis / Java / Nginx），不依赖 Docker。

## 产品一句话

机构是租户，手机号是人，学员是机构资产。  
一期只打通：**白名单开通 → 报读 → 排课 → 上课 → 消课 → 看账**。

## 端分工

| 端 | 角色 | 职责 |
|----|------|------|
| `miniprogram/` | 家长 / 老师 | 课表、下课、评价消课、比赛报名 |
| `web-admin/` | 教务 / 合伙 / 管理 | 学员、报读、白名单、排课、教案、活动、财务、权限、审计 |
| `backend/` | — | 唯一真相：JWT + MySQL + Redis + Flyway |

## 本地开发（无 Docker）

准备本机或宝塔已装好的：

- **MySQL 8**：库名建议 `xgy`，用户/密码自定  
- **Redis**：默认 `6379`  
- **JDK 21** + Maven  
- **Node 20+**（仅 Web 前端）

```bash
# 1. 改后端配置（或用环境变量）
#    backend/src/main/resources/application.yml
#    spring.datasource.* / spring.data.redis.* / app.jwt.secret

# 2. 后端（首次启动 Flyway 自动建表+演示数据）
cd backend && mvn spring-boot:run

# 3. Web
cd web-admin && npm install && npm run dev
```

- API：http://localhost:8080  
- Web：http://localhost:5173（已代理 `/api`）  
- 演示短信码：`123456`（上线务必换掉）

生产部署见 **[docs/DEPLOY_BAOTA.md](docs/DEPLOY_BAOTA.md)**。  
内网宝塔一键脚本：`scripts/baota/deploy-on-server.sh`（示例 IP `192.168.50.4`）。  
面板打不开：`scripts/baota/diagnose-bt-panel.sh`。

### 演示账号

| 手机号 | 角色 | 机构 |
|--------|------|------|
| 13800000003 | 教务 | 学趣思维 |
| 13800000002 | 老师 | 学趣思维 |
| 13800000001 | 家长（两孩跨机构） | 学趣 + 启航 |
| 13800000004 | 合伙人 | 学趣思维 |
| 13800000000 | 管理员 | 学趣思维 |
| 13800000040 | 教务 | 启航英语 |

## 文档

- [产品](docs/PRODUCT.md) · [架构](docs/ARCHITECTURE.md) · [安全](docs/SECURITY.md) · [API](docs/API.md)  
- [宝塔部署](docs/DEPLOY_BAOTA.md) · [拆成独立仓](docs/SPLIT_REPO.md)

## 技术栈

Java 21 / Spring Boot 3.3 / MySQL 8 / Redis / Flyway / JWT · React 19 + Vite · 微信小程序  
