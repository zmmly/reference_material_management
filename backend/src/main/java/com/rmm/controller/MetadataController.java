package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.entity.Metadata;
import com.rmm.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping
    public Result<List<Metadata>> listAll() {
        return Result.success(metadataService.listAll());
    }

    @GetMapping("/type/{type}")
    public Result<List<Metadata>> listByType(@PathVariable String type) {
        return Result.success(metadataService.listByType(type));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Metadata metadata) {
        metadataService.create(metadata);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Metadata metadata) {
        metadata.setId(id);
        metadataService.update(metadata);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        metadataService.delete(id);
        return Result.success();
    }
}
