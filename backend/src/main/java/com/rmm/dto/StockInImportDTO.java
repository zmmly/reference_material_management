package com.rmm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 入库导入 Excel 行数据 DTO
 */
@Data
@ColumnWidth(20)
public class StockInImportDTO {

    @ExcelProperty("标准物质编码*")
    private String materialCode;

    @ExcelProperty("标准物质名称*")
    private String materialName;

    @ExcelProperty("CAS编码")
    private String casNumber;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("批号*")
    private String batchNo;

    @ExcelProperty("入库数量*")
    private Integer quantity;

    @ExcelProperty("有效期")
    private String expiryDate;

    @ExcelProperty("存放位置*")
    private String locationName;

    @ExcelProperty("入库原因*")
    private String reason;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remarks;
}
