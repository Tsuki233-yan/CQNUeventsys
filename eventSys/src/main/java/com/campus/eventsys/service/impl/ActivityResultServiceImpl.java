package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.ActivityResult;
import com.campus.eventsys.mapper.ActivityResultMapper;
import com.campus.eventsys.service.ActivityResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 活动成果展示贴服务实现类
 */
@Slf4j
@Service
public class ActivityResultServiceImpl extends ServiceImpl<ActivityResultMapper, ActivityResult>
        implements ActivityResultService {

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    @Value("${file.access.url:/uploads/images/}")
    private String accessUrl;

    @Override
    public ActivityResult getByActivityId(Long activityId) {
        // 查询未删除且未下架的成果展示贴（status != -2）
        return baseMapper.selectOne(
                new LambdaQueryWrapper<ActivityResult>()
                        .eq(ActivityResult::getActivityId, activityId)
                        .eq(ActivityResult::getDeleted, 0)
                        .ne(ActivityResult::getStatus, -2)
        );
    }

    @Override
    public boolean existsByActivityId(Long activityId) {
        // 检查是否存在未删除且未下架的成果展示贴
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<ActivityResult>()
                        .eq(ActivityResult::getActivityId, activityId)
                        .eq(ActivityResult::getDeleted, 0)
                        .ne(ActivityResult::getStatus, -2)
        );
        return count > 0;
    }

    @Override
    public void incrementViewCount(Long id) {
        ActivityResult result = getById(id);
        if (result != null) {
            result.setViewCount(result.getViewCount() + 1);
            updateById(result);
        }
    }

    @Override
    public void incrementCommentCount(Long id) {
        ActivityResult result = getById(id);
        if (result != null) {
            result.setCommentCount(result.getCommentCount() + 1);
            updateById(result);
        }
    }

    @Override
    public void decrementCommentCount(Long id) {
        ActivityResult result = getById(id);
        if (result != null && result.getCommentCount() > 0) {
            result.setCommentCount(result.getCommentCount() - 1);
            updateById(result);
        }
    }

    @Override
    public boolean createOrRestore(ActivityResult result) {
        try {
            // 检查是否存在已删除的成果展示贴（删除后重新创建的场景）
            // 使用自定义SQL查询包含已删除的记录，绕过MyBatis Plus的逻辑删除过滤器
            ActivityResult existingResult = baseMapper.selectDeletedResult(result.getActivityId());

            if (existingResult != null) {
                // 恢复已删除的成果展示贴，更新内容
                existingResult.setDeleted(0);
                existingResult.setTitle(result.getTitle());
                existingResult.setContent(result.getContent());
                existingResult.setImages(result.getImages());
                existingResult.setCreatorId(result.getCreatorId());
                existingResult.setViewCount(0);
                existingResult.setCommentCount(0);
                existingResult.setStatus(result.getStatus() != null ? result.getStatus() : 1);
                baseMapper.updateById(existingResult);
                log.info("恢复已删除的成果展示贴: activityId={}", result.getActivityId());
            } else {
                // 创建新的成果展示贴
                baseMapper.insert(result);
            }
            return true;
        } catch (Exception e) {
            log.error("创建/恢复成果展示贴失败: activityId={}", result.getActivityId(), e);
            return false;
        }
    }

    @Override
    public String saveImage(MultipartFile file, String filename, Long activityId) {
        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建上传目录: {}", uploadDir);
            }

            // 按活动ID分目录存储，每个活动有独立的图片文件夹
            String activityDir = "activity_" + activityId;
            Path activityPath = uploadDir.resolve(activityDir);
            if (!Files.exists(activityPath)) {
                Files.createDirectories(activityPath);
                log.info("创建活动图片目录: {}", activityPath);
            }

            // 保存文件
            Path targetPath = activityPath.resolve(filename);
            file.transferTo(targetPath.toFile());

            // 返回访问URL
            String imageUrl = accessUrl + activityDir + "/" + filename;
            log.info("图片上传成功: activityId={}, url={}", activityId, imageUrl);

            return imageUrl;

        } catch (IOException e) {
            log.error("保存图片失败: activityId={}, filename={}", activityId, filename, e);
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeByActivityId(Long activityId) {
        try {
            // 查询该活动下的成果展示贴
            ActivityResult result = getByActivityId(activityId);
            if (result == null) {
                // 没有成果展示贴，视为成功
                return true;
            }

            // 逻辑删除成果展示贴
            boolean success = removeById(result.getId());
            if (success) {
                log.info("删除活动成果展示贴: activityId={}, resultId={}", activityId, result.getId());
            }
            return success;
        } catch (Exception e) {
            log.error("删除活动成果展示贴失败: activityId={}", activityId, e);
            return false;
        }
    }

    @Override
    public boolean takeDownByActivityId(Long activityId) {
        try {
            // 查询该活动下的成果展示贴
            ActivityResult result = getByActivityId(activityId);
            if (result == null) {
                // 没有成果展示贴，视为成功
                return true;
            }

            // 更新状态为已下架(-2)
            result.setStatus(-2);
            boolean success = updateById(result);
            if (success) {
                log.info("下架活动成果展示贴: activityId={}, resultId={}", activityId, result.getId());
            }
            return success;
        } catch (Exception e) {
            log.error("下架活动成果展示贴失败: activityId={}", activityId, e);
            return false;
        }
    }
}
