package com.rmm.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.dto.StockInImportConfirmDTO;
import com.rmm.dto.StockInImportDTO;
import com.rmm.dto.StockInImportPreviewVO;
import com.rmm.entity.Location;
import com.rmm.entity.Metadata;
import com.rmm.entity.StockIn;
import com.rmm.entity.Supplier;
import com.rmm.mapper.LocationMapper;
import com.rmm.mapper.MetadataMapper;
import com.rmm.mapper.SupplierMapper;
import com.rmm.service.StockInService;
import com.rmm.util.JwtUtil;
import com.rmm.util.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final LocationMapper locationMapper;
    private final MetadataMapper metadataMapper;
    private final SupplierMapper supplierMapper;

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

        // 使用 Apache POI 创建带下拉框的模板
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 创建主工作表
            XSSFSheet mainSheet = workbook.createSheet("入库导入");

            // 创建隐藏的参考数据工作表
            XSSFSheet refSheet = workbook.createSheet("参考数据");
            workbook.setSheetHidden(workbook.getSheetIndex(refSheet), true);

            // ===== 准备参考数据 =====
            // 获取所有启用的位置
            List<Location> locations = locationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Location>()
                    .eq(Location::getStatus, 1));
            // 获取入库原因元数据
            List<Metadata> reasons = metadataMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Metadata>()
                    .eq(Metadata::getType, "STOCK_IN_REASON")
                    .eq(Metadata::getStatus, 1)
                    .orderByAsc(Metadata::getSortOrder));
            // 获取所有供应商
            List<Supplier> suppliers = supplierMapper.selectList(null);

            // 写入位置参考数据（A列，从第2行开始）
            for (int i = 0; i < locations.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = refSheet.getRow(i + 1);
                if (row == null) {
                    row = refSheet.createRow(i + 1);
                }
                row.createCell(0).setCellValue(locations.get(i).getName());
            }

            // 写入入库原因参考数据（B列，从第2行开始）
            for (int i = 0; i < reasons.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = refSheet.getRow(i + 1);
                if (row == null) {
                    row = refSheet.createRow(i + 1);
                }
                row.createCell(1).setCellValue(reasons.get(i).getName());
            }

            // 写入供应商参考数据（C列，从第2行开始）
            for (int i = 0; i < suppliers.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = refSheet.getRow(i + 1);
                if (row == null) {
                    row = refSheet.createRow(i + 1);
                }
                row.createCell(2).setCellValue(suppliers.get(i).getName());
            }

            // ===== 写入主工作表标题行 =====
            org.apache.poi.ss.usermodel.Row headerRow = mainSheet.createRow(0);
            String[] headers = {"标准物质编码*", "标准物质名称*", "CAS编码", "供应商", "批号*", "入库数量*", "有效期", "存放位置*", "入库原因*", "备注"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                mainSheet.setColumnWidth(i, 20 * 256);
            }

            // 创建文本格式（用于有效期列，防止 Excel 自动转换日期）
            org.apache.poi.ss.usermodel.CellStyle textStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
            textStyle.setDataFormat(format.getFormat("@"));  // "@" 表示文本格式
            // 设置整列为文本格式
            mainSheet.setDefaultColumnStyle(6, textStyle);

            // ===== 写入示例数据行 =====
            org.apache.poi.ss.usermodel.Row sampleRow = mainSheet.createRow(1);
            sampleRow.createCell(0).setCellValue("RM001");  // 标准物质编码
            sampleRow.createCell(1).setCellValue("示例标准物质");  // 标准物质名称
            sampleRow.createCell(2).setCellValue("1234-56-7");  // CAS编码
            sampleRow.createCell(3).setCellValue(suppliers.isEmpty() ? "" : suppliers.get(0).getName());  // 供应商
            sampleRow.createCell(4).setCellValue("BATCH20260325");  // 批号
            sampleRow.createCell(5).setCellValue(5);  // 入库数量
            org.apache.poi.ss.usermodel.Cell expiryCell = sampleRow.createCell(6);
            expiryCell.setCellStyle(textStyle);  // 应用文本格式
            expiryCell.setCellValue("2026-12-31");  // 有效期

            String firstLocation = locations.isEmpty() ? "" : locations.get(0).getName();
            String firstReason = reasons.isEmpty() ? "" : reasons.get(0).getName();
            sampleRow.createCell(7).setCellValue(firstLocation);   // 存放位置
            sampleRow.createCell(8).setCellValue(firstReason);     // 入库原因
            sampleRow.createCell(9).setCellValue("示例备注");       // 备注

            // ===== 设置下拉框（数据验证）=====
            XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(mainSheet);

            // 存放位置下拉框（H列，第2-1001行）
            int lastLocationRow = locations.isEmpty() ? 2 : locations.size() + 1;
            String locationRange = String.format("'参考数据'!$A$2:$A$%d", lastLocationRow);
            DataValidation locationDv = dvHelper.createValidation(
                dvHelper.createFormulaListConstraint(locationRange),
                new CellRangeAddressList(1, 1000, 7, 7)
            );
            locationDv.setShowErrorBox(true);
            locationDv.setErrorStyle(DataValidation.ErrorStyle.STOP);
            locationDv.createErrorBox("输入错误", "请从下拉列表中选择有效的存放位置");
            mainSheet.addValidationData(locationDv);

            // 入库原因下拉框（I列，第2-1001行）
            int lastReasonRow = reasons.isEmpty() ? 2 : reasons.size() + 1;
            String reasonRange = String.format("'参考数据'!$B$2:$B$%d", lastReasonRow);
            DataValidation reasonDv = dvHelper.createValidation(
                dvHelper.createFormulaListConstraint(reasonRange),
                new CellRangeAddressList(1, 1000, 8, 8)
            );
            reasonDv.setShowErrorBox(true);
            reasonDv.setErrorStyle(DataValidation.ErrorStyle.STOP);
            reasonDv.createErrorBox("输入错误", "请从下拉列表中选择有效的入库原因");
            mainSheet.addValidationData(reasonDv);

            // 供应商下拉框（D列，第2-1001行）
            int lastSupplierRow = suppliers.isEmpty() ? 2 : suppliers.size() + 1;
            String supplierRange = String.format("'参考数据'!$C$2:$C$%d", lastSupplierRow);
            DataValidation supplierDv = dvHelper.createValidation(
                dvHelper.createFormulaListConstraint(supplierRange),
                new CellRangeAddressList(1, 1000, 3, 3)  // D列
            );
            supplierDv.setShowErrorBox(true);
            supplierDv.setErrorStyle(DataValidation.ErrorStyle.STOP);
            supplierDv.createErrorBox("输入错误", "请从下拉列表中选择有效的供应商");
            mainSheet.addValidationData(supplierDv);

            // ===== 冻结首行 =====
            mainSheet.createFreezePane(0, 1);

            // 写入响应
            workbook.write(response.getOutputStream());
        }
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
