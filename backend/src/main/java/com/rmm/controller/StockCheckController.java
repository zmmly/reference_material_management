package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.StockCheck;
import com.rmm.entity.StockCheckGroup;
import com.rmm.service.StockCheckService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}/groups")
    public Result<List<StockCheckGroup>> getGroups(@PathVariable Long id) {
        return Result.success(stockCheckService.getGroups(id));
    }

    @PostMapping
    public Result<StockCheck> create(@RequestBody StockCheck stockCheck, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(stockCheckService.create(stockCheck, userId));
    }

    @PutMapping("/{id}/check")
    public Result<Void> checkGroup(@PathVariable Long id,
                                   @RequestBody Map<String, Object> params,
                                   HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        Long groupId = ((Number) params.get("groupId")).longValue();
        BigDecimal actualQuantity = new BigDecimal(params.get("actualQuantity").toString());
        String remarks = (String) params.get("remarks");
        stockCheckService.checkGroup(groupId, actualQuantity, remarks, userId);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        stockCheckService.complete(id);
        return Result.success();
    }

    @PutMapping("/groups/{groupId}/adjust")
    public Result<Void> adjust(@PathVariable Long groupId,
                               @RequestParam String reason,
                               HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockCheckService.adjust(groupId, reason, userId);
        return Result.success();
    }
}
