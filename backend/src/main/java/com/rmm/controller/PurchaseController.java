package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Purchase;
import com.rmm.service.PurchaseService;
import com.rmm.service.PurchaseAcceptanceService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchaseAcceptanceService acceptanceService;
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
        acceptanceService.createAcceptance(id);
        return Result.success();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String purchaseNo,
            @RequestParam(required = false) String materialName,
            HttpServletResponse response) throws IOException {

        List<Purchase> list = purchaseService.listForExport(purchaseNo, materialName);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("采购记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写入Excel
        EasyExcel.write(response.getOutputStream(), Purchase.class)
                .sheet("采购记录")
                .doWrite(list);
    }
}
