package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Stock;
import com.rmm.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public Result<PageResult<Stock>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Integer status) {
        return Result.success(stockService.list(current, size, keyword, locationId, status));
    }

    @GetMapping("/all")
    public Result<List<Stock>> listAll() {
        return Result.success(stockService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Stock> getById(@PathVariable Long id) {
        return Result.success(stockService.getById(id));
    }
}
