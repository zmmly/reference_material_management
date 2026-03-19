package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.StockOut;
import com.rmm.service.StockOutService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-out")
@RequiredArgsConstructor
public class StockOutController {

    private final StockOutService stockOutService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<StockOut>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long applicantId) {
        return Result.success(stockOutService.list(current, size, status, applicantId));
    }

    @GetMapping("/my")
    public Result<PageResult<StockOut>> listMy(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(stockOutService.list(current, size, status, userId));
    }

    @PostMapping
    public Result<Void> apply(@RequestBody StockOut stockOut, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockOutService.apply(stockOut, userId);
        return Result.success();
    }

    @PostMapping("/batch")
    public Result<Void> batchApply(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) params.get("stockIds");
        List<Long> stockIds = rawIds.stream()
                .map(obj -> {
                    if (obj instanceof Number) {
                        return ((Number) obj).longValue();
                    }
                    return Long.parseLong(obj.toString());
                })
                .toList();
        String reason = (String) params.get("reason");
        String purpose = (String) params.get("purpose");
        stockOutService.batchApply(stockIds, reason, purpose, userId);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String rejectReason,
                                HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockOutService.approve(id, userId, approved, rejectReason);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockOutService.cancel(id, userId);
        return Result.success();
    }
}
