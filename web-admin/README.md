# 学管云 Web 管理端

教务 / 合伙 / 管理员。**只连同一后端**，无本地业务库。

```bash
# 先起 backend（本机 MySQL/Redis 或宝塔）
cd web-admin && npm install && npm run dev
```

Vite 开发代理 `/api` → `http://localhost:8080`。登录演示码 `123456`。

生产：`npm run build`，把 `dist/` 丢到宝塔站点，Nginx 反代 `/api`，见 `docs/DEPLOY_BAOTA.md`。
