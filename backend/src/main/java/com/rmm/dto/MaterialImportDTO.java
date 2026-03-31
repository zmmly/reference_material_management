package com.rmm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 标准物质导入 Excel 行数据 DTO
 */
@Data
@ColumnWidth(20)
public class MaterialImportDTO {

    @ExcelProperty("编号*")
    private String code;

    @ExcelProperty("名称*")
    private String name;

    @ExcelProperty("英文名称")
    private String englishName;

    @ExcelProperty("CAS号")
    private String casNumber;

    @ExcelProperty("分类*")
    private String categoryName;

    @ExcelProperty("规格*")
    private String specification;

    @ExcelProperty("基质")
    private String matrix;

    @ExcelProperty("包装形式")
    private String packageForm;

    @ExcelProperty("供应商")
    private String supplierName;
}
