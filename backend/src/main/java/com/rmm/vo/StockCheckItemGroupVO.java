package com.rmm.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点明细按批号分组的VO
 */
@Data
public class StockCheckItemGroupVO {
    // 分组标识: batchNo_locationName
    private String groupKey;

    // 标准物质ID
    private Long materialId;

    // 标准物质名称
    private String materialName;

    // 批号
    private String batchNo;

    // 存放位置
    private String locationName;

    // 内部编码列表(合并显示)
    private String internalCodes;

    // 包含的明细ID列表(用于提交盘点结果)
    private List<Long> itemIds;

    // 明细数量(多少个明细项)
    private Integer itemCount;

    // 系统数量(合计)
    private BigDecimal systemQuantity;

    // 实盘数量(合计)
    private BigDecimal actualQuantity;

    // 差异(合计)
    private BigDecimal difference;

    // 差异说明
    private String differenceReason;

    // 状态: 0-未盘点, 1-已盘点(无差异), 2-已盘点(有差异)
    private Integer status;

    // 盘点人
    private String checkerName;

    // 盘点时间
    private String checkTime;
}
