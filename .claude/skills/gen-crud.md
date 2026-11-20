---
name: gen-crud
description: 生成新的 CRUD 功能（实体、Service、Controller、Mapper）。当用户说"新建接口"、"加一个实体"、"创建一个 CRUD"、"增加一个功能模块"时自动触发。
---

# 生成 CRUD 代码

为校园活动管理系统快速搭建一个新功能的完整 CRUD 代码。

## 步骤

1. **确认需求**
   - 询问实体名称（如 Ticket、Template）
   - 询问主要字段和类型
   - 确认是否需要管理侧（/admin/**）还是公开接口

2. **生成 Entity**
   - 放 `eventSys/src/main/java/com/campus/eventsys/实体/`
   - 使用 `@TableName(value = "表名", autoResultMap = true)` 注解
   - 字段使用驼峰命名，自动映射下划线数据库字段
   - 包含 `@TableLogic` 标记逻辑删除字段 `deleted`

3. **生成 Mapper**
   - 放 `eventSys/src/main/java/com/campus/eventsys/mapper/`
   - 继承 `BaseMapper<Entity>`

4. **生成 Service 接口 + 实现**
   - 接口放 `service/`，实现放 `service/impl/`
   - 实现继承 `ServiceImpl<Mapper, Entity>`

5. **生成 Controller**
   - 放 `eventSys/src/main/java/com/campus/eventsys/controller/`
   - 使用 `Result<T>` 统一返回
   - 管理接口加 `@PreAuthorize("hasRole('ADMIN')")`
   - 查询接口允许公开访问

6. **更新 database schema**
   - 在 `eventSys/src/main/resources/schema.sql` 添加建表语句
   - 确保字段命名用下划线

## 代码规范
- 不使用 Lombok，手动写 getter/setter
- Repository 层叫 Mapper（MyBatis Plus 惯例）
- 统一用 `Result.success(data)` 返回成功
