# 校园活动综合管理系统

校园活动发布与管理平台，支持活动的创建、审核、报名、评价全流程管理。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis Plus 3.5 |
| 数据库 | SQLite |
| 认证 | JWT（无状态） |
| 前端 | Vue 3.5 + TypeScript + Vite 7 + Element Plus + Pinia |
| API 文档 | SpringDoc OpenAPI（Swagger） |

## 快速启动

```bash
# 后端（端口 8080，context-path /api）
cd eventSys
mvn spring-boot:run

# 前端（端口 5173，/api 代理到 localhost:8080）
cd eventsys_front
npm install
npm run dev
```

Swagger 文档：http://localhost:8080/api/swagger-ui.html

## 目录结构

```
├── eventSys/                 # 后端 Spring Boot
│   └── src/main/java/com/campus/eventsys/
│       ├── controller/       # REST 接口
│       ├── service/          # 业务逻辑
│       ├── mapper/           # MyBatis Plus 数据访问
│       ├── entity/           # 实体类
│       ├── config/           # 安全、数据库、Web 配置
│       ├── security/         # JWT 工具与过滤器
│       └── common/           # 统一响应、异常处理
├── eventsys_front/           # 前端 Vue 3
│   └── src/
│       ├── router/           # 路由与守卫
│       ├── stores/           # Pinia 状态管理
│       ├── api/              # Axios 请求层
│       └── views/            # 页面组件
└── .claude/                  # Claude Code 配置
    └── skills/               # 自定义技能
```

## 功能特性

- 活动全生命周期管理：草稿 → 待审核 → 报名中 → 即将开始 → 进行中 → 已结束
- 角色权限：普通用户 / 管理员，JWT 无状态认证
- 真实用户身份绑定：学生绑学号、教师绑工号，绑定前不允许报名活动
- 活动分类、成果展示、评论与评价
- 文件上传（图片，最大 10MB）
- 定时任务自动流转活动状态
- 逻辑删除

## 用户身份绑定（学生绑学号、教师绑工号）

系统对普通用户实行真实身份绑定，用于报名实名制管理：

| 身份 | 标识字段 | 校验规则（模拟） |
|------|----------|------------------|
| 学生 | `user_type=0` + `student_id` | 10 位数字，前 4 位为入学年份（2020~2026），如 `2023011234` |
| 教师 | `user_type=1` + `teacher_id` | 6 位数字，如 `200101` |

- **注册绑定**：注册（`POST /auth/register`）时必须选择身份并填写对应学号/工号，后端做格式校验 + 唯一性校验（学号/工号在未删除用户中不可重复），不合法返回 400 与中文错误提示。
- **历史账号兜底**：未绑定身份的老账号（`user_type` 为空或学号/工号为空）登录后在「个人中心」提示「请先完善身份认证」，可通过 `POST /user/bind-identity` 补填绑定；绑定前不允许报名活动（后端报名接口 `POST /activities/participant/join/{activityId}` 校验，前端报名按钮同步禁用）。管理员账号（`role=1`）为平台运营角色，豁免绑定校验。
- **数据兼容**：`user` 表新增 `user_type`（默认 0）与 `teacher_id` 列；启动时 `DatabaseInitializer` 通过 `PRAGMA table_info(user)` 检查列是否存在、缺失则 `ALTER TABLE ADD COLUMN`，老库升级不丢数据。
- **管理端**：管理员在「用户管理」可查看身份与学号/工号列，并支持按身份（学生/教师/未绑定）筛选。

> ⚠️ **说明**：以上学号/工号校验规则为**模拟校验规则**（格式 + 唯一性），仅用于教学演示环境；生产环境建议替换为学校**学籍/教工系统接口对接**做真实身份核验（对接点集中在 `UserServiceImpl.validateIdentity` 与 `/auth/register`、`/user/bind-identity` 两个入口）。

## 开源协议

MIT License
