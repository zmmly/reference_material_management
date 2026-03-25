package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 盘点分组表（按批号+位置分组）
 */
@Data
@TableName("stock_check_group")
public class StockCheckGroup {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checkId;
    private Long materialId;
    private String batchNo;
    private Long locationId;
    private String locationName;
    private String internalCodes;
    private Integer itemCount;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private String differenceReason;
    private Integer status;
    private Long checkerId;
    private LocalDateTime checkTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String checkerName;
}
