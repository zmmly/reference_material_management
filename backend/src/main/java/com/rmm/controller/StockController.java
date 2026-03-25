package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Stock;
import com.rmm.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "库存管理", description = "库存查询、盘点等接口")
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "分页查询库存", description = "根据条件分页查询库存列表")
    @GetMapping
    public Result<PageResult<Stock>> list(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词(物质名称/编码/批号)") @RequestParam(required = false) String keyword,
            @Parameter(description = "存放位置ID") @RequestParam(required = false) Long locationId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(stockService.list(current, size, keyword, locationId, status));
    }

    @Operation(summary = "查询所有库存", description = "获取所有有效库存列表")
    @GetMapping("/all")
    public Result<List<Stock>> listAll() {
        return Result.success(stockService.listAll());
    }

    @Operation(summary = "查询库存详情", description = "根据ID查询库存详细信息")
    @GetMapping("/{id}")
    public Result<Stock> getById(
            @Parameter(description = "库存ID", required = true) @PathVariable Long id) {
        return Result.success(stockService.getById(id));
    }

    @Operation(summary = "更新有效期", description = "修改库存的有效期（同标准物质同批号全部修改）")
    @PutMapping("/expiry-date")
    public Result<Integer> updateExpiryDate(
            @Parameter(description = "标准物质ID", required = true) @RequestParam Long materialId,
            @Parameter(description = "批号", required = true) @RequestParam String batchNo,
            @Parameter(description = "有效期", required = true) @RequestParam String expiryDate) {
        int count = stockService.updateExpiryDateByMaterialAndBatch(materialId, batchNo, expiryDate);
        return Result.success(count);
    }
}
