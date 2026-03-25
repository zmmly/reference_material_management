package com.rmm.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 确认入库导入请求 DTO
 */
@Data
public class StockInImportConfirmDTO {

    /** 导入项列表 */
    private List<ImportItem> items;

    @Data
    public static class ImportItem {
        /** 标准物质ID */
        private Long materialId;

        /** 批号 */
        private String batchNo;

        /** 入库数量 */
        private Integer quantity;

        /** 有效期 */
        private LocalDate expiryDate;

        /** 存放位置ID */
        private Long locationId;

        /** 入库原因编码 */
        private String reason;

        /** 备注 */
        private String remarks;
    }
}
