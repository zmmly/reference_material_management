package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.dto.MaterialImportConfirmDTO;
import com.rmm.dto.MaterialImportPreviewVO;
import com.rmm.entity.Category;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.entity.Supplier;
import com.rmm.mapper.CategoryMapper;
import com.rmm.mapper.SupplierMapper;
import com.rmm.service.ReferenceMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Tag(name = "标准物质管理", description = "标准物质的增删改查接口")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class ReferenceMaterialController {

    private final ReferenceMaterialService materialService;
    private final CategoryMapper categoryMapper;
    private final SupplierMapper supplierMapper;

    @Operation(summary = "分页查询标准物质", description = "根据条件分页查询标准物质列表")
    @GetMapping
    public Result<PageResult<ReferenceMaterial>> list(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "物质名称") @RequestParam(required = false) String name,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(materialService.list(current, size, name, categoryId, status));
    }

    @Operation(summary = "查询所有标准物质", description = "获取所有启用的标准物质列表")
    @GetMapping("/all")
    public Result<List<ReferenceMaterial>> listAll() {
        return Result.success(materialService.listAll());
    }

    @Operation(summary = "查询标准物质详情", description = "根据ID查询标准物质详细信息")
    @GetMapping("/{id}")
    public Result<ReferenceMaterial> getById(
            @Parameter(description = "物质ID", required = true) @PathVariable Long id) {
        return Result.success(materialService.getById(id));
    }

    @Operation(summary = "创建标准物质", description = "新增一个标准物质")
    @PostMapping
    public Result<Void> create(@RequestBody ReferenceMaterial material) {
        materialService.create(material);
        return Result.success();
    }

    @Operation(summary = "更新标准物质", description = "更新标准物质信息")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReferenceMaterial material) {
        material.setId(id);
        materialService.update(material);
        return Result.success();
    }

    @Operation(summary = "删除标准物质", description = "根据ID删除标准物质")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success();
    }

    @GetMapping("/template")
    @Operation(summary = "下载标准物质导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("标准物质导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet mainSheet = workbook.createSheet("标准物质导入");
            XSSFSheet refSheet = workbook.createSheet("参考数据");
            workbook.setSheetHidden(workbook.getSheetIndex(refSheet), true);

            // 准备参考数据
            List<Category> categories = categoryMapper.selectList(null);
            List<Supplier> suppliers = supplierMapper.selectList(null);

            // 写入分类参考数据
            for (int i = 0; i < categories.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = refSheet.getRow(i + 1);
                if (row == null) row = refSheet.createRow(i + 1);
                row.createCell(0).setCellValue(categories.get(i).getName());
            }

            // 写入供应商参考数据
            for (int i = 0; i < suppliers.size(); i++) {
                org.apache.poi.ss.usermodel.Row row = refSheet.getRow(i + 1);
                if (row == null) row = refSheet.createRow(i + 1);
                row.createCell(1).setCellValue(suppliers.get(i).getName());
            }

            // 写入标题行
            org.apache.poi.ss.usermodel.Row headerRow = mainSheet.createRow(0);
            String[] headers = {"编号*", "名称*", "英文名称", "CAS号", "分类*", "规格*", "基质", "包装形式", "供应商"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                mainSheet.setColumnWidth(i, 20 * 256);
            }

            // 写入示例数据
            org.apache.poi.ss.usermodel.Row sampleRow = mainSheet.createRow(1);
            sampleRow.createCell(0).setCellValue("RM001");
            sampleRow.createCell(1).setCellValue("示例标准物质");
            sampleRow.createCell(2).setCellValue("Example Material");
            sampleRow.createCell(3).setCellValue("1234-56-7");
            sampleRow.createCell(4).setCellValue(categories.isEmpty() ? "" : categories.get(0).getName());
            sampleRow.createCell(5).setCellValue("100mg");
            sampleRow.createCell(6).setCellValue("水");
            sampleRow.createCell(7).setCellValue("瓶装");
            sampleRow.createCell(8).setCellValue(suppliers.isEmpty() ? "" : suppliers.get(0).getName());

            // 设置下拉框
            XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(mainSheet);

            // 分类下拉框（E列）
            int lastCategoryRow = categories.isEmpty() ? 2 : categories.size() + 1;
            String categoryRange = String.format("'参考数据'!$A$2:$A$%d", lastCategoryRow);
            var categoryDv = dvHelper.createValidation(
                dvHelper.createFormulaListConstraint(categoryRange),
                new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 4, 4)
            );
            categoryDv.setShowErrorBox(true);
            categoryDv.createErrorBox("输入错误", "请从下拉列表中选择有效的分类");
            mainSheet.addValidationData(categoryDv);

            // 供应商下拉框（I列）
            int lastSupplierRow = suppliers.isEmpty() ? 2 : suppliers.size() + 1;
            String supplierRange = String.format("'参考数据'!$B$2:$B$%d", lastSupplierRow);
            var supplierDv = dvHelper.createValidation(
                dvHelper.createFormulaListConstraint(supplierRange),
                new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 8, 8)
            );
            supplierDv.setShowErrorBox(true);
            supplierDv.createErrorBox("输入错误", "请从下拉列表中选择有效的供应商");
            mainSheet.addValidationData(supplierDv);

            mainSheet.createFreezePane(0, 1);
            workbook.write(response.getOutputStream());
        }
    }

    @PostMapping("/import/preview")
    @Operation(summary = "预览标准物质导入数据")
    public Result<MaterialImportPreviewVO> previewImport(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return Result.error("仅支持 .xlsx 格式的 Excel 文件");
            }
            MaterialImportPreviewVO preview = materialService.previewImport(file);
            return Result.success(preview);
        } catch (IOException e) {
            log.error("解析导入文件失败", e);
            return Result.error("解析文件失败：" + e.getMessage());
        }
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "确认标准物质导入")
    public Result<Integer> confirmImport(@RequestBody MaterialImportConfirmDTO dto) {
        int count = materialService.confirmImport(dto);
        return Result.success(count);
    }
}
