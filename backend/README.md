# 学管云 Backend

Spring Boot 3.3 后端服务，包名 `com.xgy.cloud`。

## 技术栈

- Java 21 / Spring Boot 3.3.5
- Spring Web / Data JPA / Security / Data Redis
- MySQL 8 + Flyway
- JJWT 0.12.6 / Lombok / Validation

## 快速启动

### 1. 启动依赖服务

```bash
cd backend
docker compose up -d
```

会启动：

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL 8.0 | 3306 | 库 `xgy`，用户 `xgy` / `xgy123`，root/`root123` |
| Redis 7 | 6379 | 会话 / 上次角色 / Token 黑名单 |

### 2. 启动应用

```bash
cd backend
mvn spring-boot:run
```

或：

```bash
mvn -DskipTests package
java -jar target/xueguan-yun-1.0.0-SNAPSHOT.jar
```

默认端口 **8080**，配置见 `src/main/resources/application.yml`。

首次启动 Flyway 自动建表并写入演示数据。

### 3. 编译检查

```bash
mvn -q -DskipTests compile
```

## 演示账号

短信验证码统一为 **`123456`**（开发演示）。

| 手机号 | 姓名 | 角色 | 机构 |
|--------|------|------|------|
| 13800000001 | 王女士 | 家长（两孩跨机构） | 学趣 + 启航 |
| 13800000002 | 李老师 | 老师 | 学趣思维 |
| 13800000003 | 赵教务 | 教务 | 学趣思维 |
| 13800000004 | 陈合伙人 | 合伙/教务/老师 | 学趣思维 |
| 13800000000 | 周总 | 管理/合伙/教务/老师 | 学趣思维 |
| 13800000040 | 启航教务 | 教务 | 启航英语 |
| 13800000041 | 韩老师 | 老师 | 启航英语 |

演示机构：`org_xuequ` 学趣思维（id=1）、`org_qihang` 启航英语（id=2）。

## 登录示例

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000003","smsCode":"123456"}'
```

后续请求携带：`Authorization: Bearer <token>`。

## API 列表

统一响应：`{ "code": 0, "message": "ok", "data": ... }`

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 手机号+短信登录（公开） |
| GET | `/api/auth/me` | 当前用户与角色 |
| POST | `/api/auth/switch-role` | 切换角色 `{role, orgId?}` |
| POST | `/api/auth/switch-student` | 家长切换学员 `{studentId}` |
| POST | `/api/auth/logout` | 退出并拉黑 Token |

### 机构 `/api/orgs`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/orgs` | 机构列表（家长用于切换孩子所属机构） |

### 学员 `/api/students`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/students` | 列表（员工按 org；家长跨机构看自己的孩子） |
| GET | `/api/students/by-phone?phone=` | 按手机号查学员 |
| POST | `/api/students` | 创建 |
| PUT | `/api/students/{id}` | 更新 |

### 教案 `/api/packages`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/packages` | 列表 |
| POST | `/api/packages` | 创建 |
| PUT | `/api/packages/{id}` | 更新 |

### 报读 `/api/enrollments`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/enrollments?studentId=` | 学员报读列表 |
| POST | `/api/enrollments` | 代录/更新报读 |

### 班级 `/api/classes`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/classes` | 列表 |
| POST | `/api/classes` | 建班 |
| POST | `/api/classes/{id}/students` | 加学员 `{studentId}` |

### 课次 `/api/lessons`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/lessons?date=yyyy-MM-dd` | 按日期列表 |
| POST | `/api/lessons` | 排课 |
| POST | `/api/lessons/{id}/makeup` | 临补 |
| POST | `/api/lessons/{id}/rate-by-teacher` | 老师评价并消课 |
| POST | `/api/lessons/{id}/rate-by-student` | 学生评价老师 |

### 财务 `/api/finance`（合伙人 / 管理员）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/finance/summary` | 财务汇总 |
| GET | `/api/finance/orders` | 订单流水 |

### 教师 `/api/teachers`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/teachers` | 列表 |
| POST | `/api/teachers` | 创建 |

### 白名单 `/api/whitelist`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/whitelist` | 列表 |
| POST | `/api/whitelist` | 添加 |

## 租户隔离说明

- 员工：JWT 绑定 `orgId`，业务查询一律按机构过滤。
- 家长：手机号全局唯一；学员按 `orgId + parent_phone + student_name` 唯一；切换学员即切换机构上下文。
- Redis Key：`xgy:session:{userId}`、`xgy:lastRole:{phone}`、`xgy:lastStudent:{phone}`、`xgy:blacklist:{token}`。
