package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Purchase;
import com.rmm.service.PurchaseService;
import com.rmm.service.PurchaseAcceptanceService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {

        List<Purchase> list = purchaseService.listForExport(purchaseNo, materialName, status);

        // 转换为导出DTO
        List<PurchaseExportDTO> exportList = list.stream().map(p -> {
            PurchaseExportDTO dto = new PurchaseExportDTO();
            dto.setPurchaseNo(p.getPurchaseNo());
            dto.setApplicantName(p.getApplicantName());
            dto.setMaterialCode(p.getMaterialCode());
            dto.setMaterialName(p.getMaterialName());
            dto.setSpecification(p.getSpecification());
            dto.setBatchNumber(p.getBatchNumber());
            dto.setQuantity(p.getQuantity() != null ? p.getQuantity().intValue() : 0);
            dto.setUnit(p.getUnit());
            dto.setEstimatedPrice(p.getEstimatedPrice());
            dto.setTotalAmount(p.getTotalAmount());
            dto.setSupplierName(p.getSupplierName());
            dto.setEstimatedArrivalDate(p.getEstimatedArrivalDate() != null ? p.getEstimatedArrivalDate().toString() : "");
            dto.setStatus(statusToText(p.getStatus()));
            dto.setApplyTime(p.getApplyTime() != null ? p.getApplyTime().toString() : "");
            dto.setReason(p.getReason());
            dto.setRejectReason(p.getRejectReason());
            return dto;
        }).collect(Collectors.toList());

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("采购申请记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写入Excel
        EasyExcel.write(response.getOutputStream(), PurchaseExportDTO.class)
                .sheet("采购申请记录")
                .doWrite(exportList);
    }

    private String statusToText(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已撤回";
            case 4 -> "待验收";
            case 6 -> "验收通过";
            case 7 -> "验收拒绝";
            default -> "未知";
        };
    }

    @Data
    @ColumnWidth(20)
    public static class PurchaseExportDTO {
        @ExcelProperty("采购单号")
        @ColumnWidth(25)
        private String purchaseNo;

        @ExcelProperty("申请人")
        private String applicantName;

        @ExcelProperty("编号")
        @ColumnWidth(25)
        private String materialCode;

        @ExcelProperty("名称")
        @ColumnWidth(25)
        private String materialName;

        @ExcelProperty("规格")
        private String specification;

        @ExcelProperty("批号")
        private String batchNumber;

        @ExcelProperty("数量")
        private Integer quantity;

        @ExcelProperty("单位")
        @ColumnWidth(10)
        private String unit;

        @ExcelProperty("单价")
        private java.math.BigDecimal estimatedPrice;

        @ExcelProperty("金额")
        private java.math.BigDecimal totalAmount;

        @ExcelProperty("供应商")
        private String supplierName;

        @ExcelProperty("预计到货日期")
        @ColumnWidth(15)
        private String estimatedArrivalDate;

        @ExcelProperty("状态")
        @ColumnWidth(12)
        private String status;

        @ExcelProperty("申请时间")
        @ColumnWidth(25)
        private String applyTime;

        @ExcelProperty("采购原因")
        @ColumnWidth(30)
        private String reason;

        @ExcelProperty("拒绝原因")
        @ColumnWidth(30)
        private String rejectReason;
    }
}
