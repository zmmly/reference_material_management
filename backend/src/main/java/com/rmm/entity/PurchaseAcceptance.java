package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_acceptance")
public class PurchaseAcceptance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long purchaseId;
    private String purchaseNo;
    private Long materialId;
    private String materialName;
    private String materialCode;
    private String specification;
    private String batchNumber;
    private BigDecimal quantity;
    private BigDecimal actualQuantity;
    private String unit;
    private Long supplierId;
    private java.time.LocalDate expiryDate;
    private Long locationId;

    @TableField(exist = false)
    private String locationName;
    private String supplierName;
    private BigDecimal estimatedPrice;
    private BigDecimal totalAmount;

    // 验收信息
    private Integer packageIntact;
    private Integer labelComplete;
    private Integer hasDamage;
    private Integer acceptanceResult;
    private String acceptanceRemark;
    private LocalDateTime acceptanceDate;
    private Long acceptanceUserId;
    private String acceptanceUserName;

    // 入库信息
    private Long stockInId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 状态枚举
    public static final int STATUS_PENDING = 0;  // 待验收
    public static final int STATUS_PASSED = 1;    // 验收通过
    public static final int STATUS_REJECTED = 2;   // 验收拒绝

    @TableField(exist = false)
    private String acceptanceResultText;
}
