package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.entity.Category;
import com.rmm.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        return Result.success(categoryService.list());
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Category category) {
        categoryService.create(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
