package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Location;
import com.rmm.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public Result<PageResult<Location>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(locationService.list(current, size, keyword, status));
    }

    @GetMapping("/all")
    public Result<List<Location>> listAll() {
        return Result.success(locationService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Location> getById(@PathVariable Long id) {
        return Result.success(locationService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Location location) {
        locationService.create(location);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Location location) {
        location.setId(id);
        locationService.update(location);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return Result.success();
    }
}
