package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点明细与库存关联表
 */
@Data
@TableName("stock_check_item_stock")
public class StockCheckItemStock {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checkId;
    private Long groupId;
    private Long stockId;
    private BigDecimal systemQuantity;
    private LocalDateTime createTime;
}
