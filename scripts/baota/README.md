# 宝塔脚本

| 脚本 | 作用 |
|------|------|
| `deploy-on-server.sh` | 在宝塔服务器上部署学管云（Jar + 前端 + systemd + Nginx） |
| `diagnose-bt-panel.sh` | 排查宝塔面板主页打不开 |
| `xueguan.env.example` | 后端环境变量模板 |

必须在 **能 SSH 到目标机** 的环境执行；公网 Cloud Agent 无法直连 `192.168.50.4`。

详见 [`docs/DEPLOY_BAOTA.md`](../../docs/DEPLOY_BAOTA.md)。
