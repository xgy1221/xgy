# API 概览

统一响应：`{ "code": 0, "message": "ok", "data": ... }`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ping` | 探活 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/me` | 当前用户 |
| POST | `/api/auth/switch-role` | 切角色 |
| POST | `/api/auth/switch-student` | 切学员 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/orgs` | 机构 |
| GET | `/api/campuses` | 校区 |
| CRUD | `/api/students` | 学员（软删 DELETE） |
| CRUD | `/api/packages` | 教案 |
| GET/POST | `/api/enrollments` | 报读 |
| CRUD | `/api/classes` + students | 班级 |
| GET/POST | `/api/lessons`… | 排课/下课/旷课/临补/评价 |
| GET/POST | `/api/activities`… | 活动 |
| GET/POST | `/api/finance/*` | 财务 |
| GET/POST/DELETE | `/api/whitelist` | 白名单 |
| GET/POST | `/api/users*` | 授权 |
| GET | `/api/teachers` | 教师 |
| GET | `/api/audit` | 审计 |

详情以 Controller 为准。  
