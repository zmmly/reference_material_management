package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.StockCheck;
import com.rmm.entity.StockCheckItem;
import com.rmm.service.StockCheckService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-check")
@RequiredArgsConstructor
public class StockCheckController {

    private final StockCheckService stockCheckService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<StockCheck>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(stockCheckService.list(current, size, status));
    }

    @GetMapping("/{id}")
    public Result<StockCheck> getById(@PathVariable Long id) {
        return Result.success(stockCheckService.getById(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<StockCheckItem>> getItems(@PathVariable Long id) {
        return Result.success(stockCheckService.getItems(id));
    }

    @PostMapping
    public Result<StockCheck> create(@RequestBody StockCheck stockCheck, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(stockCheckService.create(stockCheck, userId));
    }

    @PutMapping("/{checkId}/items/{itemId}")
    public Result<Void> checkItem(@PathVariable Long checkId,
                                  @PathVariable Long itemId,
                                  @RequestParam java.math.BigDecimal actualQuantity,
                                  @RequestParam(required = false) String remarks,
                                  HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockCheckService.checkItem(checkId, itemId, actualQuantity, remarks, userId);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        stockCheckService.complete(id);
        return Result.success();
    }

    @PutMapping("/items/{itemId}/adjust")
    public Result<Void> adjust(@PathVariable Long itemId,
                               @RequestParam String reason,
                               HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockCheckService.adjust(itemId, reason, userId);
        return Result.success();
    }
}
