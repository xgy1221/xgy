# 安全

## 已内建

| 项 | 说明 |
|----|------|
| 登录限流 | Redis：手机号 / IP |
| 白名单开通 | `require-whitelist-for-new-parent=true` |
| RBAC | `requireStaff` / `requireAcademicOrAdmin` / 老师课次归属 |
| CORS | 默认仅 localhost Vite |
| JWT 黑名单 | 登出、切角色 |
| 审计 | 登录、授权、录单、消课 |
| 密钥外置 | `JWT_SECRET` `DB_*` `REDIS_*` `SMS_DEMO_CODE` |

## 上线前

1. 强随机 `JWT_SECRET`  
2. 换库密码  
3. 接真短信，关掉演示码  
4. CORS 改为正式 Web 域名  
5. HTTPS + 合法域名（小程序）  
6. 按 `docs/DEPLOY_BAOTA.md` 用宝塔部署（不依赖 Docker）  
