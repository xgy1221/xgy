# 架构

## 原则

1. **Security-first**：限流、白名单、RBAC、CORS、审计从第一天就有  
2. **Single source of truth**：只有 backend  
3. **Thin clients**：页面不持有业务规则，规则在服务端  

## 领域

```
Person(phone)
  ├─ StaffMembership(orgId, roles[])
  └─ Student(orgId, parentPhone)…

Org
  ├─ Campus
  ├─ CoursePackage → Enrollment → LessonAttendee(consume)
  ├─ Class → Lesson → Attendance
  ├─ Order
  ├─ Activity → Signup
  ├─ PhoneWhitelist
  └─ AuditLog
```

## 认证

- `POST /api/auth/login`：短信 OTP（演示码可配）  
- JWT 声明：`userId, phone, orgId, roles, currentRole, currentStudentId`  
- 切角色 / 切学员会换发 Token，旧 Token 进 Redis 黑名单  

## 模块边界

| 包 | 职责 |
|----|------|
| `security` | JWT、CORS、限流辅助、SecurityUtils |
| `tenant` | TenantContext |
| `domain` / `repository` | 表实体 |
| `service` | 业务 + 权限断言 |
| `web` | 薄 Controller |

## 部署

- **推荐生产**：宝塔安装 MySQL + Redis + Nginx，Jar 本机守护，见 `docs/DEPLOY_BAOTA.md`  
- **本地开发**：本机或宝塔已有的 MySQL/Redis，直接 `mvn spring-boot:run` + `npm run dev`  
- 不依赖 Docker  
