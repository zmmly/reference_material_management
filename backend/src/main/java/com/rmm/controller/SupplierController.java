package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Supplier;
import com.rmm.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "供应商管理", description = "供应商信息的增删改查")
@RestController
@RequestMapping("/api/basic/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "分页查询供应商")
    @GetMapping
    public Result<PageResult<Supplier>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(supplierService.list(current, size, keyword, status));
    }

    @Operation(summary = "获取所有启用的供应商")
    @GetMapping("/all")
    public Result<List<Supplier>> listAll() {
        return Result.success(supplierService.listAll());
    }

    @Operation(summary = "根据ID查询供应商")
    @GetMapping("/{id}")
    public Result<Supplier> getById(@PathVariable Long id) {
        return Result.success(supplierService.getById(id));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<Void> create(@RequestBody Supplier supplier) {
        supplierService.create(supplier);
        return Result.success();
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplierService.update(id, supplier);
        return Result.success();
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.success();
    }
}
