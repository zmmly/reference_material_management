package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.service.ReferenceMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class ReferenceMaterialController {

    private final ReferenceMaterialService materialService;

    @GetMapping
    public Result<PageResult<ReferenceMaterial>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return Result.success(materialService.list(current, size, name, categoryId, status));
    }

    @GetMapping("/all")
    public Result<List<ReferenceMaterial>> listAll() {
        return Result.success(materialService.listAll());
    }

    @GetMapping("/{id}")
    public Result<ReferenceMaterial> getById(@PathVariable Long id) {
        return Result.success(materialService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ReferenceMaterial material) {
        materialService.create(material);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReferenceMaterial material) {
        material.setId(id);
        materialService.update(material);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success();
    }
}
