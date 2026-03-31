package com.rmm.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库记录删除检查结果 VO
 */
@Data
public class StockInDeleteCheckVO {

    /** 入库记录ID */
    private Long stockInId;

    /** 是否可以删除 */
    private Boolean canDelete;

    /** 不可删除原因 */
    private String reason;

    /** 阻止删除的出库记录列表 */
    private List<StockOutInfo> blockedItems;

    /**
     * 出库记录信息
     */
    @Data
    public static class StockOutInfo {
        /** 内部编号 */
        private String internalCode;

        /** 状态 */
        private String status;

        /** 出库原因 */
        private String reason;

        /** 申请时间 */
        private LocalDateTime applyTime;
    }
}
