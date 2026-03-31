package com.rmm.dto;

import lombok.Data;
import java.util.List;

/**
 * 标准物质导入预览响应 VO
 */
@Data
public class MaterialImportPreviewVO {

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

        /** 编号 */
        private String code;

        /** 名称 */
        private String name;

        /** 英文名称 */
        private String englishName;

        /** CAS号 */
        private String casNumber;

        /** 分类ID（校验通过后填充） */
        private Long categoryId;

        /** 分类名称 */
        private String categoryName;

        /** 规格 */
        private String specification;

        /** 基质 */
        private String matrix;

        /** 包装形式 */
        private String packageForm;

        /** 供应商ID（校验通过后填充） */
        private Long supplierId;

        /** 供应商名称 */
        private String supplierName;

        /** 是否有效 */
        private Boolean valid;

        /** 错误信息列表 */
        private List<String> errors;
    }
}
