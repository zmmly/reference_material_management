package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase")
public class Purchase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String specification;
    private String batchNumber;
    private String unit;
    private BigDecimal quantity;
    private Long supplierId;
    private BigDecimal estimatedPrice;
    private BigDecimal totalAmount;
    private LocalDate estimatedArrivalDate;
    private String reason;
    private Long applicantId;
    private LocalDateTime applyTime;
    private Long approverId;
    private LocalDateTime approveTime;
    private Integer status;
    private String rejectReason;
    private String remarks;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String approverName;
}
