package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.security.JwtUtil;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供用户登录和注册接口
 *
 * 用户信息管理接口请使用 UserController (/user)
 */
@Tag(name = "认证管理", description = "用户登录、注册接口。用户信息管理请使用 /user 接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功后返回JWT令牌和用户信息。\n\n" +
                "返回数据结构：\n" +
                "- token: JWT令牌，后续请求通过 Authorization: Bearer {token} 方式携带\n" +
                "- user: 用户信息对象（含id、username、nickname、avatar、role等字段）\n\n" +
                "用户角色说明：role=0 普通用户，role=1 管理员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Parameter(description = "登录请求参数", required = true)
            @RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username, request.password)
        );

        String token = jwtUtil.generateToken(request.username);
        User user = userService.getByUsername(request.username);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        return Result.success("登录成功", result);
    }

    /**
     * 用户注册
     * 注册时必须完成身份绑定（真实用户身份绑定）：
     * - 学生：userType=0，必填 10 位数字学号（前 4 位为入学年份 2020~2026）
     * - 教师：userType=1，必填 6 位数字工号
     * 学号/工号在未删除用户中不可重复；校验不通过返回明确中文错误（BusinessException）
     * 【模拟校验规则，可替换为学籍接口对接】
     */
    @Operation(summary = "用户注册", description = "注册新用户，用户名不能重复，注册时须绑定身份。\n\n" +
                "请求体为用户对象，必填字段：\n" +
                "- username: 用户名（唯一）\n" +
                "- password: 密码\n" +
                "- userType: 身份（0-学生，1-教师）\n\n" +
                "身份绑定字段（按身份必填）：\n" +
                "- 学生（userType=0）：studentId 必填，10 位数字，前 4 位为入学年份（2020~2026）\n" +
                "- 教师（userType=1）：teacherId 必填，6 位数字\n\n" +
                "校验规则：学号/工号格式校验 + 唯一性校验（未删除用户中不可重复），不合法返回 400 与中文错误提示\n" +
                "【模拟校验规则，可替换为学籍接口对接】\n\n" +
                "可选字段：nickname、email、phone、realName、college、major、grade、gender、bio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "用户名已存在/未选择身份/学号或工号格式不正确/学号或工号已被绑定")
    })
    @PostMapping("/register")
    public Result<Void> register(
            @Parameter(description = "用户信息（含身份绑定字段 userType/studentId/teacherId）", required = true)
            @RequestBody @Valid User user) {
        userService.register(user);
        return Result.success("注册成功", null);
    }

    /**
     * 获取当前登录用户信息
     * 注意：HTTP 接口已迁至 UserController (/user/info)，此方法保留给内部调用/单元测试
     */
    public Result<User> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        return Result.success(user);
    }

    /**
     * 上传用户头像
     * 注意：HTTP 接口已迁至 UserController (/user/upload-avatar)，此方法保留给内部调用/单元测试
     */
    public Result<String> uploadAvatar(MultipartFile avatarFile, Authentication authentication) {
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
     * 注意：HTTP 接口已迁至 UserController (/user/update-info)，此方法保留给内部调用/单元测试
     */
    public Result<Void> updateUserInfo(String nickname, String email, String phone, String realName,
                                       String college, String major, String grade, Integer gender,
                                       String bio, Authentication authentication) {
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
     * 登录请求参数
     */
    @Schema(description = "登录请求参数")
    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
            String username,
            @NotBlank(message = "密码不能为空") @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
            String password
    ) {}
}
