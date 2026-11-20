package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理控制器
 * 提供用户管理功能：查询、修改、删除、设置管理员
 */
@Tag(name = "管理员-用户管理", description = "管理员使用的用户管理接口\n\n" +
        "用户角色说明：\n" +
        "- role=0: 普通用户\n" +
        "- role=1: 管理员\n\n" +
        "用户状态说明：\n" +
        "- status=0: 禁用（封禁）\n" +
        "- status=1: 正常")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户列表（支持模糊搜索）
     */
    @Operation(summary = "用户列表", description = "分页获取用户列表，支持模糊搜索和筛选。\n\n" +
                "搜索范围：用户名、昵称、学号、工号、邮箱\n" +
                "筛选条件：角色(role)、状态(status)、身份(userType)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词（用户名/昵称/学号/工号/邮箱）") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选：0-普通用户，1-管理员") @RequestParam(required = false) Integer role,
            @Parameter(description = "状态筛选：0-禁用，1-正常") @RequestParam(required = false) Integer status,
            @Parameter(description = "身份筛选：0-学生，1-教师，-1-未绑定身份") @RequestParam(required = false) Integer userType) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 模糊搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getStudentId, keyword)
                    .or()
                    .like(User::getTeacherId, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }

        // 角色筛选
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        // 身份筛选（真实用户身份绑定）：0-学生，1-教师，-1-未绑定身份（user_type 为空，或学生但学号为空）
        if (userType != null) {
            if (userType == -1) {
                wrapper.and(w -> w.isNull(User::getUserType)
                        .or(s -> s.eq(User::getUserType, 0)
                                .and(x -> x.isNull(User::getStudentId).or().eq(User::getStudentId, ""))));
            } else {
                wrapper.eq(User::getUserType, userType);
            }
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageResult = userService.page(new Page<>(page, size), wrapper);
        return Result.success(pageResult);
    }

    /**
     * 获取用户列表（重载，不按身份筛选）
     * 供内部调用/单元测试使用，等价于 userType 传 null
     */
    public Result<Page<User>> list(Integer page, Integer size, String keyword, Integer role, Integer status) {
        return list(page, size, keyword, role, status, null);
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "用户详情", description = "根据ID获取用户详细信息。返回数据中密码字段已隐藏")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/detail/{id}")
    public Result<User> detail(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 隐藏密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     * 注意：封禁/解禁用户请使用 /ban/{id} 和 /unban/{id} 接口
     */
    @Operation(summary = "更新用户", description = "更新用户基本信息（昵称、邮箱等）。\n\n" +
                "注意：封禁/解禁用户请使用专门的 /ban/{id} 和 /unban/{id} 接口，不要通过此接口修改 status 字段")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "昵称") @RequestParam(required = false) String nickname,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "学院") @RequestParam(required = false) String college,
            @Parameter(description = "专业") @RequestParam(required = false) String major,
            @Parameter(description = "年级") @RequestParam(required = false) String grade,
            @Parameter(description = "性别：0-女，1-男") @RequestParam(required = false) Integer gender,
            @Parameter(description = "个人简介") @RequestParam(required = false) String bio) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 更新字段（不包含status，封禁/解禁请使用专门接口）
        if (nickname != null) user.setNickname(nickname);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        if (realName != null) user.setRealName(realName);
        if (college != null) user.setCollege(college);
        if (major != null) user.setMajor(major);
        if (grade != null) user.setGrade(grade);
        if (gender != null) user.setGender(gender);
        if (bio != null) user.setBio(bio);

        userService.updateById(user);
        return Result.success("更新成功", null);
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置密码", description = "重置指定用户的密码。新密码长度不能少于6位，密码将使用BCrypt加密存储")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/reset-password/{id}")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "新密码", required = true) @RequestParam String newPassword) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);

        return Result.success("密码重置成功", null);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Operation(summary = "删除用户", description = "删除用户（逻辑删除）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 设置用户为管理员
     */
    @Operation(summary = "设置管理员", description = "将指定用户设置为管理员。将 role 从 0 更新为 1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/set-admin/{id}")
    public Result<Void> setAdmin(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setRole(1); // 1-管理员
        userService.updateById(user);

        return Result.success("已设置为管理员", null);
    }

    /**
     * 取消用户管理员权限
     */
    @Operation(summary = "取消管理员", description = "取消指定用户的管理员权限。将 role 从 1 更新为 0")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/cancel-admin/{id}")
    public Result<Void> cancelAdmin(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setRole(0); // 0-普通用户
        userService.updateById(user);

        return Result.success("已取消管理员权限", null);
    }

    /**
     * 封禁用户（禁用）
     */
    @Operation(summary = "封禁用户", description = "封禁指定用户，将 status 设为 0。封禁后用户将无法登录。\n\n" +
                "注意：不能封禁管理员，需先取消管理员权限")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "封禁成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "400", description = "不能封禁管理员")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/ban/{id}")
    public Result<Void> banUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不能封禁管理员
        if (user.getRole() != null && user.getRole() == 1) {
            return Result.error("不能封禁管理员，请先取消管理员权限");
        }

        // 检查是否已经是禁用状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("该用户已被封禁");
        }

        user.setStatus(0); // 0-禁用
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("封禁成功", null);
        } else {
            return Result.error("封禁失败");
        }
    }

    /**
     * 解禁用户（启用）
     */
    @Operation(summary = "解禁用户", description = "解禁被封禁的用户，将 status 设为 1，恢复其正常使用权限")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "解禁成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "400", description = "用户状态正常，无需解禁")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/unban/{id}")
    public Result<Void> unbanUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {

        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查是否已经是正常状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            return Result.error("该用户状态正常，无需解禁");
        }

        user.setStatus(1); // 1-正常
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("解禁成功", null);
        } else {
            return Result.error("解禁失败");
        }
    }
}
