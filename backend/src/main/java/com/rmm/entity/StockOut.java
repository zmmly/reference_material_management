package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_out")
public class StockOut {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockId;
    private Long materialId;
    private String internalCode;
    private String batchNo;
    private BigDecimal quantity;
    private String reason;
    private String purpose;
    private Long applicantId;
    private LocalDateTime applyTime;
    private Long approverId;
    private LocalDateTime approveTime;
    private Integer status;
    private String rejectReason;
    private String remarks;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String casNumber;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String approverName;
}
