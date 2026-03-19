package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.StockIn;
import com.rmm.service.StockInService;
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
@RequestMapping("/api/stock-in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<PageResult<StockIn>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String batchNo) {
        return Result.success(stockInService.list(current, size, keyword, reason, startDate, endDate, operatorId, materialName, batchNo));
    }

    @PostMapping
    public Result<Void> create(@RequestBody StockIn stockIn, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        stockInService.create(stockIn, userId);
        return Result.success();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String batchNo,
            HttpServletResponse response) throws IOException {

        List<StockIn> list = stockInService.listAll(keyword, reason, startDate, endDate, operatorId, materialName, batchNo);

        // 转换为导出DTO
        List<StockInExportDTO> exportList = list.stream().map(stockIn -> {
            StockInExportDTO dto = new StockInExportDTO();
            dto.setMaterialName(stockIn.getMaterialName());
            dto.setBatchNo(stockIn.getBatchNo());
            dto.setInternalCode(stockIn.getInternalCode());
            dto.setQuantity(stockIn.getQuantity() != null ? stockIn.getQuantity().intValue() : 0);
            dto.setSupplierName(stockIn.getSupplierName());
            dto.setExpiryDate(stockIn.getExpiryDate() != null ? stockIn.getExpiryDate().toString() : "");
            dto.setLocationName(stockIn.getLocationName());
            dto.setReason(reasonToText(stockIn.getReason()));
            dto.setOperatorName(stockIn.getOperatorName());
            dto.setCreateTime(stockIn.getCreateTime() != null ? stockIn.getCreateTime().toString() : "");
            dto.setRemarks(stockIn.getRemarks());
            return dto;
        }).collect(Collectors.toList());

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("入库记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写入Excel
        EasyExcel.write(response.getOutputStream(), StockInExportDTO.class)
                .sheet("入库记录")
                .doWrite(exportList);
    }

    private String reasonToText(String reason) {
        if (reason == null) return "";
        return switch (reason) {
            case "PURCHASE" -> "新购入";
            case "SURPLUS" -> "盘盈";
            case "RETURN" -> "归还";
            case "TRANSFER_IN" -> "调拨入";
            case "OTHER" -> "其他";
            default -> reason;
        };
    }

    @Data
    @ColumnWidth(20)
    public static class StockInExportDTO {
        @ExcelProperty("标准物质")
        private String materialName;

        @ExcelProperty("批号")
        private String batchNo;

        @ExcelProperty("内部编码")
        private String internalCode;

        @ExcelProperty("入库数量")
        private Integer quantity;

        @ExcelProperty("供应商")
        private String supplierName;

        @ExcelProperty("有效期")
        private String expiryDate;

        @ExcelProperty("存放位置")
        private String locationName;

        @ExcelProperty("入库原因")
        private String reason;

        @ExcelProperty("操作人")
        private String operatorName;

        @ExcelProperty("入库时间")
        @ColumnWidth(25)
        private String createTime;

        @ExcelProperty("备注")
        @ColumnWidth(30)
        private String remarks;
    }
}
