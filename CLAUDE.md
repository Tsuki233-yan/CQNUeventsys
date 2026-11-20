# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

校园活动综合管理系统。后端 `eventSys/`：Java 17 + Spring Boot 3.2 + MyBatis Plus 3.5 + SQLite + JWT。前端 `eventsys_front/`：Vue 3.5 + TypeScript + Vite 7 + Element Plus + Pinia。

## 常用命令

```bash
# 后端（端口 8080，context-path /api）
cd eventSys
mvn spring-boot:run           # 启动
mvn test                      # 全部测试
mvn test -Dtest=ClassName     # 单个测试类

# 前端（端口 5173，/api 代理到 localhost:8080）
cd eventsys_front
npm run dev                   # 启动
npm run build                 # 类型检查 + 构建
```
Swagger 文档：`http://localhost:8080/api/swagger-ui.html`

## 后端架构 (`eventSys/`)

`com.campus.eventsys` 下标准分层：

| 层 | 职责 |
|---|---|
| `controller/` | REST 接口。`/auth/**` 公开，`/admin/**` 需 ADMIN 角色，其余需登录。活动列表/详情、分类、成果、评论、评价的查询接口公开。 |
| `service/` + `service/impl/` | 业务逻辑，controller 仅做薄层转发 |
| `mapper/` | MyBatis Plus BaseMapper，复杂查询用 `QueryWrapper`/`LambdaQueryWrapper` |
| `entity/` | 实体类，驼峰自动映射下划线字段 |
| `config/` | SecurityConfig（无状态 JWT + CORS）、DatabaseInitializer（自定义初始化 SQLite）、MyBatisPlusConfig、WebMvcConfig |
| `security/` | JwtUtil + JwtAuthenticationFilter（从 `Authorization: Bearer` 取 token） |
| `common/` | `Result<T>` 统一响应 `{code, message, data}`、BusinessException、GlobalExceptionHandler |
| `scheduler/` | 定时任务自动流转活动状态 |

**关键约定：**
- 响应格式：成功 `Result.success(data)` → `{code: 200, message: "操作成功", data}`。错误码 400/401/403/500
- 活动状态流转：`-2` 下架 → `-1` 拒绝 → `0` 草稿 → `1` 待审核 → `2` 报名中 → `3` 即将开始 → `4` 进行中 → `5` 已结束
- 角色：`role=0` 普通用户，`role=1` 管理员。管理接口 `/admin/**` 需要 `ROLE_ADMIN`
- 文件上传：`uploads/images/`，通过 `/api/images/` 访问，最大 10MB
- SQLite 数据库文件在运行目录 `event_database.db`，启动时由 DatabaseInitializer 执行 `schema.sql` + `data.sql`
- 逻辑删除：`deleted=0/1`

## 前端架构 (`eventsys_front/`)

| 目录 | 职责 |
|---|---|
| `src/router/` | Vue Router (createWebHistory)，路由守卫通过 localStorage token 判断登录态。meta: `public` / `guestOnly` / `requiresAuth` / `requiresAdmin` |
| `src/stores/` | Pinia。`user.ts` 管理 token + 用户信息，login/register/logout 操作 |
| `src/api/` | Axios 层。`request.ts` 实例 baseURL `http://localhost:8080/api`。请求拦截器注入 JWT，响应拦截器对 401 自动登出跳转，code=200 时自动解包返回 `data` 字段 |
| `src/views/` | 按路由的 Vue SFC，管理页面在 `admin/` 子目录 |

**关键约定：**
- Vite 配了 `@` → `src/` 别名
- Element Plus 全局注册，所有图标组件已全局注册
- Axios 响应拦截器 code=200 时直接返回 `response.data.data`，调用方拿到的就是业务数据
