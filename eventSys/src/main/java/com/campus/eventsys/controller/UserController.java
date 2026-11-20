package com.campus.eventsys.controller;

import com.campus.eventsys.common.BusinessException;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户信息管理控制器
 * 提供当前登录用户的个人信息查看和修改接口
 *
 * 与 AuthController 的职责划分：
 * - AuthController (/auth)：认证相关操作（登录、注册）
 * - UserController (/user)：登录后的用户信息管理（查看资料、修改资料、上传头像）
 */
@Tag(name = "用户信息管理", description = "当前登录用户的个人信息管理接口，需登录后访问\n\n" +
        "与认证相关接口（登录/注册）请使用 /auth 接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息",
               description = "获取当前登录用户的详细信息。\n\n" +
                           "返回数据包含：id、username、nickname、avatar、email、phone、realName、college、major、grade、studentId、gender、bio、role、status、participatedCount、organizedCount等")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/info")
    public Result<User> getUserInfo(
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        return Result.success(user);
    }

    /**
     * 上传用户头像
     * 上传新头像会自动覆盖旧头像
     */
    @Operation(summary = "上传头像",
               description = "上传用户头像，支持jpg/png/gif格式，大小不超过5MB。上传新头像会自动删除旧头像文件。\n\n" +
                           "请求格式：multipart/form-data\n" +
                           "表单参数：avatar（图片文件，必填）\n\n" +
                           "返回数据：头像图片URL路径，如 /images/avatars/user_{id}_{uuid}.png")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功，data返回头像URL"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "上传失败：文件为空/非图片格式/超过5MB")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/upload-avatar", consumes = "multipart/form-data")
    public Result<String> uploadAvatar(
            @Parameter(description = "头像图片文件，支持jpg/png/gif格式", required = true)
            @RequestParam("avatar") MultipartFile avatarFile,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (avatarFile == null || avatarFile.isEmpty()) {
            return Result.error("请选择要上传的头像图片");
        }

        try {
            String avatarUrl = userService.uploadAvatar(user.getId(), avatarFile);
            return Result.success("头像上传成功", avatarUrl);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新当前用户信息
     */
    @Operation(summary = "更新用户信息",
               description = "更新当前登录用户的基本信息，所有字段均为可选，仅更新传入的字段。\n\n" +
                           "可更新字段：nickname、email、phone、realName、college、major、grade、gender、bio\n" +
                           "不可通过此接口修改：username（用户名不可改）、password（请使用修改密码接口）、role（角色不可改）、status（状态不可改）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/update-info")
    public Result<Void> updateUserInfo(
            @Parameter(description = "昵称") @RequestParam(required = false) String nickname,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "学院") @RequestParam(required = false) String college,
            @Parameter(description = "专业") @RequestParam(required = false) String major,
            @Parameter(description = "年级") @RequestParam(required = false) String grade,
            @Parameter(description = "性别：0-女，1-男") @RequestParam(required = false) Integer gender,
            @Parameter(description = "个人简介") @RequestParam(required = false) String bio,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname(nickname);
        updateUser.setEmail(email);
        updateUser.setPhone(phone);
        updateUser.setRealName(realName);
        updateUser.setCollege(college);
        updateUser.setMajor(major);
        updateUser.setGrade(grade);
        updateUser.setGender(gender);
        updateUser.setBio(bio);

        boolean success = userService.updateUserInfo(updateUser);
        if (success) {
            return Result.success("更新成功", null);
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 补填身份绑定（历史用户"完善身份认证"）
     * 未绑定身份的老账号登录后可在此绑定学号/工号，绑定后才能报名活动
     * 校验规则与注册一致，由 UserService.bindIdentity 统一处理（格式 + 唯一性，BusinessException 返回中文错误）
     * 【模拟校验规则，可替换为学籍接口对接】
     */
    @Operation(summary = "绑定身份（学号/工号）",
               description = "为未绑定身份的账号补填身份绑定。绑定前不允许报名活动。\n\n" +
                           "参数说明：\n" +
                           "- userType: 身份（0-学生，1-教师），必填\n" +
                           "- 学生（userType=0）：studentId 必填，10 位数字，前 4 位为入学年份（2020~2026）\n" +
                           "- 教师（userType=1）：teacherId 必填，6 位数字\n\n" +
                           "校验规则：\n" +
                           "- 已绑定身份的账号不可重复绑定（如需变更请联系管理员）\n" +
                           "- 学号/工号格式校验 + 唯一性校验（未删除用户中不可重复）\n\n" +
                           "【模拟校验规则，可替换为学籍接口对接】")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "绑定成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "未选择身份/学号或工号格式不正确/已被绑定/已完成绑定")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/bind-identity")
    public Result<Void> bindIdentity(
            @Parameter(description = "身份：0-学生，1-教师", required = true)
            @RequestParam Integer userType,
            @Parameter(description = "学号（学生必填，10位数字，前4位为入学年份2020~2026）")
            @RequestParam(required = false) String studentId,
            @Parameter(description = "工号（教师必填，6位数字）")
            @RequestParam(required = false) String teacherId,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        try {
            userService.bindIdentity(user.getId(), userType, studentId, teacherId);
            return Result.success("身份绑定成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }
}
