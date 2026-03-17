package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_check_item")
public class StockCheckItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long checkId;
    private Long stockId;
    private Long materialId;
    private BigDecimal systemQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal difference;
    private String differenceReason;
    private Integer status;
    private Long checkerId;
    private LocalDateTime checkTime;
    private String remarks;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String internalCode;
    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String locationName;
    @TableField(exist = false)
    private String checkerName;
}
