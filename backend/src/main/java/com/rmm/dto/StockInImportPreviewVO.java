package com.rmm.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库导入预览响应 VO
 */
@Data
public class StockInImportPreviewVO {

    /** 预览项列表 */
    private List<PreviewItem> items;

    /** 总数量 */
    private Integer totalCount;

    /** 有效数量 */
    private Integer validCount;

    /** 无效数量 */
    private Integer invalidCount;

    /**
     * 单行预览数据
     */
    @Data
    public static class PreviewItem {
        /** Excel 行号 */
        private Integer rowNum;

        /** 标准物质编码 */
        private String materialCode;

        /** 标准物质ID（校验通过后填充） */
        private Long materialId;

        /** 标准物质名称（校验通过后填充） */
        private String materialName;

        /** 批号 */
        private String batchNo;

        /** 入库数量 */
        private Integer quantity;

        /** 有效期 */
        private LocalDate expiryDate;

        /** 存放位置ID（校验通过后填充） */
        private Long locationId;

        /** 存放位置名称 */
        private String locationName;

        /** 入库原因（文字） */
        private String reasonText;

        /** 入库原因编码（校验通过后填充） */
        private String reasonCode;

        /** 备注 */
        private String remarks;

        /** 是否有效 */
        private Boolean valid;

        /** 错误信息列表 */
        private List<String> errors;
    }
}
