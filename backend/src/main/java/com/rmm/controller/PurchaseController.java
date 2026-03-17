package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Purchase;
import com.rmm.service.PurchaseService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<Purchase>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(purchaseService.list(current, size, status, userId));
    }

    @GetMapping("/all")
    public Result<PageResult<Purchase>> listAll(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(purchaseService.list(current, size, status, null));
    }

    @GetMapping("/{id}")
    public Result<Purchase> getById(@PathVariable Long id) {
        return Result.success(purchaseService.getById(id));
    }

    @PostMapping
    public Result<Void> apply(@RequestBody Purchase purchase, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        purchaseService.apply(purchase, userId);
        return Result.success();
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String rejectReason,
                                HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        purchaseService.approve(id, userId, approved, rejectReason);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        purchaseService.cancel(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/arrive")
    public Result<Void> markArrived(@PathVariable Long id) {
        purchaseService.markArrived(id);
        return Result.success();
    }
}
