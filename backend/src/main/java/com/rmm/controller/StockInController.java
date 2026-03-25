package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.dto.StockInImportConfirmDTO;
import com.rmm.dto.StockInImportDTO;
import com.rmm.dto.StockInImportPreviewVO;
import com.rmm.entity.StockIn;
import com.rmm.service.StockInService;
import com.rmm.util.JwtUtil;
import com.rmm.util.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stock-in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;
    private final JwtUtil jwtUtil;
    private final OperationLogUtil operationLogUtil;

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
        String username = jwtUtil.getUsername(token);
        stockInService.create(stockIn, userId);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "入库",
            "库存入库", "入库登记: " + stockIn.getMaterialName());

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

    @GetMapping("/template")
    @Operation(summary = "下载入库导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("入库导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 创建模板数据（标题行 + 示例行）
        List<StockInImportDTO> templateData = new ArrayList<>();

        // 示例数据行
        StockInImportDTO sample = new StockInImportDTO();
        sample.setMaterialCode("RM001");
        sample.setBatchNo("BATCH20260325");
        sample.setQuantity(5);
        sample.setExpiryDate("2026-12-31");
        sample.setLocationName("仓库A");
        sample.setReason("新购入");
        sample.setRemarks("示例备注");
        templateData.add(sample);

        // 写入 Excel
        EasyExcel.write(response.getOutputStream(), StockInImportDTO.class)
                .sheet("入库导入")
                .doWrite(templateData);
    }

    @PostMapping("/import/preview")
    @Operation(summary = "预览入库导入数据")
    public Result<StockInImportPreviewVO> previewImport(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件格式
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return Result.error("仅支持 .xlsx 格式的 Excel 文件");
            }

            StockInImportPreviewVO preview = stockInService.previewImport(file);
            return Result.success(preview);
        } catch (IOException e) {
            log.error("解析导入文件失败", e);
            return Result.error("解析文件失败：" + e.getMessage());
        }
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "确认入库导入")
    public Result<Map<String, Object>> confirmImport(
            @RequestBody StockInImportConfirmDTO dto,
            HttpServletRequest request) {
        // 验证数据
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.error("导入数据不能为空");
        }

        // 获取当前用户
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);

        // 执行导入
        int successCount = stockInService.confirmImport(dto, userId);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "入库",
            "批量入库导入", "成功导入 " + successCount + " 条入库记录");

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("message", "成功导入 " + successCount + " 条入库记录");
        return Result.success(result);
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
