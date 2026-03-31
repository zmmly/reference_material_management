package com.rmm.dto;

import lombok.Data;
import java.util.List;

/**
 * 确认标准物质导入请求 DTO
 */
@Data
public class MaterialImportConfirmDTO {

    /** 导入项列表 */
    private List<ImportItem> items;

    @Data
    public static class ImportItem {
        /** 编号 */
        private String code;

        /** 名称 */
        private String name;

        /** 英文名称 */
        private String englishName;

        /** CAS号 */
        private String casNumber;

        /** 分类ID */
        private Long categoryId;

        /** 规格 */
        private String specification;

        /** 基质 */
        private String matrix;

        /** 包装形式 */
        private String packageForm;

        /** 供应商ID */
        private Long supplierId;
    }
}
