package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {

    User getByUsername(String username);

    /**
     * 根据学号查询用户
     *
     * @param studentId 学号
     * @return 用户信息
     */
    User getByStudentId(String studentId);

    /**
     * 根据工号查询用户
     *
     * @param teacherId 工号
     * @return 用户信息
     */
    User getByTeacherId(String teacherId);

    /**
     * 判断用户是否已完成身份绑定（学生绑定学号、教师绑定工号）
     * 未绑定身份的用户不允许报名活动
     *
     * @param user 用户信息
     * @return true-已绑定
     */
    boolean isIdentityBound(User user);

    /**
     * 历史用户补填身份绑定（个人中心"完善身份认证"）
     * 校验规则与注册一致：学生填10位数字学号（前4位为入学年份2020~2026）、教师填6位数字工号
     * 【模拟校验规则，可替换为学籍接口对接】
     *
     * @param userId 用户ID
     * @param userType 身份：0-学生，1-教师
     * @param studentId 学号（学生必填）
     * @param teacherId 工号（教师必填）
     * @return 是否绑定成功
     */
    boolean bindIdentity(Long userId, Integer userType, String studentId, String teacherId);

    boolean register(User user);

    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param avatarFile 头像文件
     * @return 头像访问URL
     */
    String uploadAvatar(Long userId, MultipartFile avatarFile);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 是否更新成功
     */
    boolean updateUserInfo(User user);
}
