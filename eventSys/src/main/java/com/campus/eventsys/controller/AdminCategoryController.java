package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Category;
import com.campus.eventsys.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-活动分类管理控制器
 * 提供活动分类的创建、更新、删除及全量查询功能（管理员权限）
 *
 * 与 CategoryController 的职责划分：
 * - CategoryController (/categories)：公开查询接口（列表、详情）
 * - AdminCategoryController (/admin/categories)：管理员管理接口（CRUD）
 */
@Tag(name = "管理员-分类管理", description = "管理员使用的活动分类管理接口，包含创建、更新、删除及全量查询功能。需要ADMIN角色权限\n\n" +
        "与公开查询接口的区别：\n" +
        "- /categories (CategoryController): 公开查询，仅返回启用的分类\n" +
        "- /admin/categories (本接口): 管理员管理，返回所有分类（含禁用）")
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类（包括禁用的）
     */
    @Operation(summary = "获取所有分类",
               description = "获取所有分类（包括禁用的），用于管理员管理，按排序号升序排列。\n\n" +
                           "与公开接口的区别：公开接口仅返回 status=1(启用) 的分类，此接口返回全部")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/list")
    public Result<List<Category>> list() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    /**
     * 创建分类
     */
    @Operation(summary = "创建分类", description = "创建新的活动分类。\n\n" +
                "请求体为分类对象，字段包括：name(分类名)、icon(图标)、sortOrder(排序号)、status(状态: 0-禁用/1-启用)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/create")
    public Result<Void> create(
            @Parameter(description = "分类信息", required = true) @RequestBody Category category) {
        boolean success = categoryService.createCategory(category);
        return success ? Result.success("创建成功", null) : Result.error("创建失败");
    }

    /**
     * 更新分类
     */
    @Operation(summary = "更新分类", description = "更新活动分类信息。\n\n" +
                "可通过设置 status=0 来禁用分类，禁用后公开接口将不再返回该分类")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @Parameter(description = "分类ID", required = true) @PathVariable Long id,
            @Parameter(description = "分类信息", required = true) @RequestBody Category category) {
        Category existCategory = categoryService.getById(id);
        if (existCategory == null) {
            return Result.error("分类不存在");
        }
        category.setId(id);
        boolean success = categoryService.updateCategory(category);
        return success ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    /**
     * 删除分类（逻辑删除）
     */
    @Operation(summary = "删除分类", description = "删除活动分类（逻辑删除）。\n\n" +
                "注意：删除分类前请确认该分类下没有活动，否则可能影响数据统计")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(description = "分类ID", required = true) @PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        boolean success = categoryService.deleteCategory(id);
        return success ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
