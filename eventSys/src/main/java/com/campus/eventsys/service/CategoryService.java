package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.Category;

import java.util.List;

/**
 * 活动分类服务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有启用的分类（前端展示用）
     */
    List<Category> getActiveCategories();

    /**
     * 获取所有分类（包括禁用的，用于管理员管理）
     */
    List<Category> getAllCategories();

    /**
     * 根据ID获取分类详情
     */
    Category getCategoryById(Long id);

    /**
     * 创建分类
     */
    boolean createCategory(Category category);

    /**
     * 更新分类
     */
    boolean updateCategory(Category category);

    /**
     * 删除分类（逻辑删除）
     */
    boolean deleteCategory(Long id);

    /**
     * 增加分类活动数量
     */
    void incrementActivityCount(Long categoryId);

    /**
     * 减少分类活动数量
     */
    void decrementActivityCount(Long categoryId);
}
