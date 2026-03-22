package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.StockOut;
import com.rmm.service.StockOutService;
import com.rmm.util.JwtUtil;
import com.rmm.util.OperationLogUtil;
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
    private final OperationLogUtil operationLogUtil;

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
        String username = jwtUtil.getUsername(token);
        stockOutService.apply(stockOut, userId);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "出库",
            "库存出库", "申请出库: " + stockOut.getMaterialName());

        return Result.success();
    }

    @PostMapping("/batch")
    public Result<Void> batchApply(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
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

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "出库",
            "批量出库", "批量申请出库，数量: " + stockIds.size());

        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String rejectReason,
                                HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        stockOutService.approve(id, userId, approved, rejectReason);

        // 记录操作日志
        String action = approved ? "通过" : "拒绝";
        String detail = approved ? "出库审核通过" : "出库审核拒绝: " + (rejectReason != null ? rejectReason : "");
        operationLogUtil.log(request, userId, username, "stock", "审核",
            "出库申请", detail);

        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        stockOutService.cancel(id, userId);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "取消",
            "出库申请", "取消出库申请ID: " + id);

        return Result.success();
    }
}
