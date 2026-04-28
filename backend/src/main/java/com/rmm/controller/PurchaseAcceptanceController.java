package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.PurchaseAcceptance;
import com.rmm.dto.AcceptanceSubmitDTO;
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
@RequestMapping("/api/purchase-acceptance")
@RequiredArgsConstructor
public class PurchaseAcceptanceController {

    private final PurchaseAcceptanceService acceptanceService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<PurchaseAcceptance>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) String purchaseNo,
            @RequestParam(required = false) String materialName) {
        return Result.success(acceptanceService.list(current, size, status, result, purchaseNo, materialName));
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String purchaseNo,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) Integer result,
            HttpServletResponse response) throws IOException {

        List<PurchaseAcceptance> list = acceptanceService.listForExport(purchaseNo, materialName, result);

        List<AcceptanceExportDTO> exportList = list.stream().map(a -> {
            AcceptanceExportDTO dto = new AcceptanceExportDTO();
            dto.setPurchaseNo(a.getPurchaseNo());
            dto.setMaterialName(a.getMaterialName());
            dto.setSpecification(a.getSpecification());
            dto.setBatchNumber(a.getBatchNumber());
            dto.setQuantity(a.getQuantity() != null ? a.getQuantity().intValue() : 0);
            dto.setUnit(a.getUnit());
            dto.setSupplierName(a.getSupplierName());
            dto.setAcceptanceResultText(a.getAcceptanceResultText());
            dto.setActualQuantity(a.getActualQuantity() != null ? a.getActualQuantity().doubleValue() : null);
            dto.setAcceptanceUserName(a.getAcceptanceUserName());
            dto.setAcceptanceDate(a.getAcceptanceDate() != null ? a.getAcceptanceDate().toString() : "");
            dto.setAcceptanceRemark(a.getAcceptanceRemark());
            return dto;
        }).collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("采购验收记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), AcceptanceExportDTO.class)
                .sheet("采购验收记录")
                .doWrite(exportList);
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
            dto.getBatchNumber(),
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

    @Data
    @ColumnWidth(20)
    public static class AcceptanceExportDTO {
        @ExcelProperty("采购单号")
        @ColumnWidth(25)
        private String purchaseNo;

        @ExcelProperty("标准物质")
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

        @ExcelProperty("供应商")
        private String supplierName;

        @ExcelProperty("验收状态")
        @ColumnWidth(12)
        private String acceptanceResultText;

        @ExcelProperty("实际到货数量")
        @ColumnWidth(15)
        private Double actualQuantity;

        @ExcelProperty("验收人")
        private String acceptanceUserName;

        @ExcelProperty("验收日期")
        @ColumnWidth(25)
        private String acceptanceDate;

        @ExcelProperty("验收备注")
        @ColumnWidth(30)
        private String acceptanceRemark;
    }
}
