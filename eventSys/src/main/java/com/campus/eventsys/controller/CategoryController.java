package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Category;
import com.campus.eventsys.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动分类公开查询控制器
 * 提供活动分类的公开查询接口
 *
 * 管理员分类管理接口已迁至 AdminCategoryController (/admin/categories)
 */
@Tag(name = "活动分类", description = "活动分类公开查询接口（无需登录）。管理员管理请使用 /admin/categories 接口")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有启用的分类列表（公开接口，前端展示用）
     */
    @Operation(summary = "获取启用的分类列表", description = "获取所有启用的活动分类列表，按排序号升序排列。\n\n" +
                "返回数据中每个分类包含：id、name(分类名)、icon(图标)、sortOrder(排序号)、activityCount(活动数量)、status(状态: 1-启用)\n" +
                "仅返回 status=1(启用) 的分类")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public Result<List<Category>> list() {
        List<Category> categories = categoryService.getActiveCategories();
        return Result.success(categories);
    }

    /**
     * 获取分类详情（公开接口）
     */
    @Operation(summary = "获取分类详情", description = "根据分类ID获取分类详细信息。\n\n" +
                "返回数据包含：id、name、icon、sortOrder、activityCount、status、createTime等")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @GetMapping("/detail/{id}")
    public Result<Category> detail(
            @Parameter(description = "分类ID", required = true) @PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        return Result.success(category);
    }
}
