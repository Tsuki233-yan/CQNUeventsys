package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Category;
import com.campus.eventsys.mapper.CategoryMapper;
import com.campus.eventsys.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动分类服务实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getActiveCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .eq(Category::getDeleted, 0)
               .orderByAsc(Category::getSortOrder);
        return list(wrapper);
    }

    @Override
    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getDeleted, 0)
               .orderByAsc(Category::getSortOrder);
        return list(wrapper);
    }

    @Override
    public Category getCategoryById(Long id) {
        return getById(id);
    }

    @Override
    public boolean createCategory(Category category) {
        // 初始化活动数量为0
        category.setActivityCount(0);
        return save(category);
    }

    @Override
    public boolean updateCategory(Category category) {
        return updateById(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        return removeById(id);
    }

    @Override
    public void incrementActivityCount(Long categoryId) {
        Category category = getById(categoryId);
        if (category != null) {
            category.setActivityCount(category.getActivityCount() + 1);
            updateById(category);
        }
    }

    @Override
    public void decrementActivityCount(Long categoryId) {
        Category category = getById(categoryId);
        if (category != null && category.getActivityCount() > 0) {
            category.setActivityCount(category.getActivityCount() - 1);
            updateById(category);
        }
    }
}
