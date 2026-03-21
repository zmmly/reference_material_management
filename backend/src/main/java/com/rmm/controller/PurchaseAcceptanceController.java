package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.PurchaseAcceptance;
import com.rmm.dto.AcceptanceSubmitDTO;
import com.rmm.service.PurchaseAcceptanceService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-acceptance")
@RequiredArgsConstructor
public class PurchaseAcceptanceController {

    private final PurchaseAcceptanceService acceptanceService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<PurchaseAcceptance>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(acceptanceService.list(current, size, status));
    }

    @GetMapping("/{id}")
    public Result<PurchaseAcceptance> getById(@PathVariable Long id) {
        return Result.success(acceptanceService.getById(id));
    }

    @PostMapping("/{id}/start")
    public Result<Void> startAcceptance(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        acceptanceService.startAcceptance(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submitAcceptance(@PathVariable Long id,
                                         @RequestBody AcceptanceSubmitDTO dto,
                                         HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        acceptanceService.submitAcceptance(id, userId,
            dto.getPackageIntact(),
            dto.getLabelComplete(),
            dto.getHasDamage(),
            dto.getActualQuantity(),
            dto.getExpiryDate(),
            dto.getLocationId(),
            dto.getResult(),
            dto.getRemark());
        return Result.success();
    }
}
