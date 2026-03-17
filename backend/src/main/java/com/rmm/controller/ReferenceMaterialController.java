package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.service.ReferenceMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "标准物质管理", description = "标准物质的增删改查接口")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class ReferenceMaterialController {

    private final ReferenceMaterialService materialService;

    @Operation(summary = "分页查询标准物质", description = "根据条件分页查询标准物质列表")
    @GetMapping
    public Result<PageResult<ReferenceMaterial>> list(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "物质名称") @RequestParam(required = false) String name,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(materialService.list(current, size, name, categoryId, status));
    }

    @Operation(summary = "查询所有标准物质", description = "获取所有启用的标准物质列表")
    @GetMapping("/all")
    public Result<List<ReferenceMaterial>> listAll() {
        return Result.success(materialService.listAll());
    }

    @Operation(summary = "查询标准物质详情", description = "根据ID查询标准物质详细信息")
    @GetMapping("/{id}")
    public Result<ReferenceMaterial> getById(
            @Parameter(description = "物质ID", required = true) @PathVariable Long id) {
        return Result.success(materialService.getById(id));
    }

    @Operation(summary = "创建标准物质", description = "新增一个标准物质")
    @PostMapping
    public Result<Void> create(@RequestBody ReferenceMaterial material) {
        materialService.create(material);
        return Result.success();
    }

    @Operation(summary = "更新标准物质", description = "更新标准物质信息")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReferenceMaterial material) {
        material.setId(id);
        materialService.update(material);
        return Result.success();
    }

    @Operation(summary = "删除标准物质", description = "根据ID删除标准物质")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success();
    }
}
