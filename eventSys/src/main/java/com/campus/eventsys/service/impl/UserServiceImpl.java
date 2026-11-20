package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.common.BusinessException;
import com.campus.eventsys.config.TimeZoneConfig;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.mapper.UserMapper;
import com.campus.eventsys.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 身份绑定模拟校验规则（可替换为学籍接口对接）：
     * - 学生学号：10 位数字，前 4 位为入学年份（2020~2026），如 2023011234
     * - 教师工号：6 位数字，如 200101
     * 生产环境建议对接学校学籍/教工系统做真实身份核验，此处仅做格式与唯一性校验
     */
    private static final String STUDENT_ID_PATTERN = "^202[0-6]\\d{6}$";
    private static final String TEACHER_ID_PATTERN = "^\\d{6}$";

    /** 身份类型：学生 */
    private static final int USER_TYPE_STUDENT = 0;
    /** 身份类型：教师 */
    private static final int USER_TYPE_TEACHER = 1;

    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    @Value("${file.access.url:/uploads/images/}")
    private String accessUrl;

    @Override
    public User getByUsername(String username) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    @Override
    public User getByStudentId(String studentId) {
        // @TableLogic 自动追加 deleted=0，仅在未删除用户中查重
        return baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId)
        );
    }

    @Override
    public User getByTeacherId(String teacherId) {
        // @TableLogic 自动追加 deleted=0，仅在未删除用户中查重
        return baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getTeacherId, teacherId)
        );
    }

    @Override
    public boolean isIdentityBound(User user) {
        if (user == null || user.getUserType() == null) {
            return false;
        }
        if (user.getUserType() == USER_TYPE_STUDENT) {
            return user.getStudentId() != null && !user.getStudentId().isEmpty();
        }
        if (user.getUserType() == USER_TYPE_TEACHER) {
            return user.getTeacherId() != null && !user.getTeacherId().isEmpty();
        }
        return false;
    }

    @Override
    public boolean bindIdentity(Long userId, Integer userType, String studentId, String teacherId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 已绑定身份的用户不允许重复绑定（如需变更请联系管理员）
        if (isIdentityBound(user)) {
            throw new BusinessException("您已完成身份绑定，如需变更请联系管理员");
        }

        // 格式与唯一性校验（未删除用户中学号/工号不可重复）
        String normalizedId = validateIdentity(userType, studentId, teacherId, user.getId());

        // 使用 UpdateWrapper 显式写入，保证另一身份的编号列可被清空为 NULL
        // （updateById 默认忽略 null 字段，无法清除历史遗留的学号/工号）
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getUserType, userType)
                .set(User::getStudentId, userType == USER_TYPE_STUDENT ? normalizedId : null)
                .set(User::getTeacherId, userType == USER_TYPE_TEACHER ? normalizedId : null)
                .set(User::getUpdateTime, java.time.ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime());

        boolean success = update(updateWrapper);
        if (success) {
            log.info("用户 [{}] 身份绑定成功: userType={}, 学号/工号已更新", user.getUsername(), userType);
        }
        return success;
    }

    @Override
    public boolean register(User user) {
        if (getByUsername(user.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 身份绑定校验：注册时必须选择学生/教师身份并填写对应学号/工号
        String normalizedId = validateIdentity(user.getUserType(), user.getStudentId(), user.getTeacherId(), null);
        if (user.getUserType() != null && user.getUserType() == USER_TYPE_STUDENT) {
            user.setStudentId(normalizedId);
            user.setTeacherId(null);
        } else if (user.getUserType() != null && user.getUserType() == USER_TYPE_TEACHER) {
            user.setTeacherId(normalizedId);
            user.setStudentId(null);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        return save(user);
    }

    /**
     * 身份绑定格式与唯一性校验
     *
     * @param userType 身份：0-学生，1-教师
     * @param studentId 学号
     * @param teacherId 工号
     * @param selfUserId 当前用户ID（补填绑定时用于排除自身重复，注册时传 null）
     * @return 归一化后的学号/工号（去首尾空格）
     */
    private String validateIdentity(Integer userType, String studentId, String teacherId, Long selfUserId) {
        if (userType == null) {
            throw new BusinessException(400, "请选择身份（学生或教师）");
        }
        if (userType != USER_TYPE_STUDENT && userType != USER_TYPE_TEACHER) {
            throw new BusinessException(400, "身份类型不合法，仅支持学生或教师");
        }

        if (userType == USER_TYPE_STUDENT) {
            // 学生：必填 10 位数字学号，前 4 位为入学年份 2020~2026（模拟校验规则，可替换为学籍接口对接）
            if (studentId == null || studentId.trim().isEmpty()) {
                throw new BusinessException(400, "学生必须填写学号");
            }
            String normalized = studentId.trim();
            if (!normalized.matches(STUDENT_ID_PATTERN)) {
                throw new BusinessException(400, "学号格式不正确：须为10位数字，且前4位为入学年份（2020~2026）");
            }
            // 唯一性校验：学号在未删除用户中不可重复（排除自身）
            User exist = getByStudentId(normalized);
            if (exist != null && !exist.getId().equals(selfUserId)) {
                throw new BusinessException(400, "该学号已绑定其他账号，请核对后重试");
            }
            return normalized;
        }

        // 教师：必填 6 位数字工号（模拟校验规则，可替换为教工接口对接）
        if (teacherId == null || teacherId.trim().isEmpty()) {
            throw new BusinessException(400, "教师必须填写工号");
        }
        String normalizedTeacherId = teacherId.trim();
        if (!normalizedTeacherId.matches(TEACHER_ID_PATTERN)) {
            throw new BusinessException(400, "工号格式不正确：须为6位数字");
        }
        // 唯一性校验：工号在未删除用户中不可重复（排除自身）
        User exist = getByTeacherId(normalizedTeacherId);
        if (exist != null && !exist.getId().equals(selfUserId)) {
            throw new BusinessException(400, "该工号已绑定其他账号，请核对后重试");
        }
        return normalizedTeacherId;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile avatarFile) {
        try {
            // 检查用户是否存在
            User user = getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 检查文件类型
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("只能上传图片文件");
            }

            // 检查文件大小（限制10MB）
            if (avatarFile.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("头像图片大小不能超过10MB");
            }

            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建上传目录: {}", uploadDir);
            }

            // 用户头像独立目录
            String avatarDir = "avatars";
            Path avatarPath = uploadDir.resolve(avatarDir);
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath);
                log.info("创建头像目录: {}", avatarPath);
            }

            // 删除旧头像文件（如果存在）
            String oldAvatar = user.getAvatar();
            if (oldAvatar != null && !oldAvatar.isEmpty()) {
                deleteOldAvatar(oldAvatar, avatarPath);
            }

            // 生成唯一文件名（使用用户ID前缀便于识别）
            String originalFilename = avatarFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = "user_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + extension;

            // 保存文件
            Path targetPath = avatarPath.resolve(newFilename);
            avatarFile.transferTo(targetPath.toFile());

            // 生成访问URL
            String avatarUrl = accessUrl + avatarDir + "/" + newFilename;
            log.info("头像上传成功: userId={}, url={}", userId, avatarUrl);

            // 更新用户头像字段
            user.setAvatar(avatarUrl);
            updateById(user);

            return avatarUrl;

        } catch (IOException e) {
            log.error("头像上传失败: userId={}", userId, e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除旧头像文件
     */
    private void deleteOldAvatar(String oldAvatarUrl, Path avatarPath) {
        try {
            // 从URL中提取文件名
            String oldFilename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
            Path oldFilePath = avatarPath.resolve(oldFilename);

            if (Files.exists(oldFilePath)) {
                Files.delete(oldFilePath);
                log.info("删除旧头像文件: {}", oldFilePath);
            }
        } catch (Exception e) {
            log.warn("删除旧头像文件失败: {}", oldAvatarUrl, e);
            // 不影响新头像上传，只记录日志
        }
    }

    @Override
    public boolean updateUserInfo(User user) {
        // 不允许修改的字段
        user.setUsername(null);
        user.setPassword(null);
        user.setRole(null);
        user.setCreateTime(null);
        // 身份绑定字段不允许通过普通资料修改接口变更（绑定走 /user/bind-identity 专用接口）
        user.setUserType(null);
        user.setStudentId(null);
        user.setTeacherId(null);

        return updateById(user);
    }
}
