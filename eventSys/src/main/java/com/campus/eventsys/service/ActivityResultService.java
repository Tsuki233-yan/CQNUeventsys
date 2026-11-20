package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.ActivityResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 活动成果展示贴服务接口
 */
public interface ActivityResultService extends IService<ActivityResult> {

    /**
     * 根据活动ID获取成果展示贴
     *
     * @param activityId 活动ID
     * @return 成果展示贴，不存在返回null
     */
    ActivityResult getByActivityId(Long activityId);

    /**
     * 检查活动是否已有成果展示贴
     *
     * @param activityId 活动ID
     * @return true-已存在，false-不存在
     */
    boolean existsByActivityId(Long activityId);

    /**
     * 增加浏览次数
     *
     * @param id 成果展示贴ID
     */
    void incrementViewCount(Long id);

    /**
     * 增加评论数量
     *
     * @param id 成果展示贴ID
     */
    void incrementCommentCount(Long id);

    /**
     * 减少评论数量
     *
     * @param id 成果展示贴ID
     */
    void decrementCommentCount(Long id);

    /**
     * 创建或恢复成果展示贴
     * 如果存在已删除的成果展示贴，则恢复它；否则创建新的
     *
     * @param result 成果展示贴信息
     * @return true-成功，false-失败
     */
    boolean createOrRestore(ActivityResult result);

    /**
     * 保存上传的图片文件
     *
     * @param file       图片文件
     * @param filename   文件名
     * @param activityId 活动ID，用于分目录存储
     * @return 图片访问URL
     */
    String saveImage(MultipartFile file, String filename, Long activityId);

    /**
     * 根据活动ID删除成果展示贴（逻辑删除）
     * 用于活动删除或下架时的清理操作
     *
     * @param activityId 活动ID
     * @return 是否删除成功
     */
    boolean removeByActivityId(Long activityId);

    /**
     * 根据活动ID下架成果展示贴（状态更新为-2已下架）
     * 用于活动下架时的同步操作
     *
     * @param activityId 活动ID
     * @return 是否下架成功
     */
    boolean takeDownByActivityId(Long activityId);
}
