package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_check")
public class StockCheck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkNo;
    private LocalDate checkDate;
    private String scope;
    private String scopeValue;
    private Integer status;
    private Integer totalCount;
    private Integer checkedCount;
    private Integer differenceCount;
    private Long creatorId;
    private Long checkerId;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private String remarks;

    @TableField(exist = false)
    private String creatorName;

    @TableField(exist = false)
    private String checkerName;

    // 分组统计字段（非持久化）
    @TableField(exist = false)
    private Integer groupCount;

    @TableField(exist = false)
    private Integer checkedGroupCount;
}
