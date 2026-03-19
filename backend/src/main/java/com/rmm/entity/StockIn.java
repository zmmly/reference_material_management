package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_in")
public class StockIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockId;
    private Long materialId;
    private String batchNo;
    private String internalCode;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private Long locationId;
    private String reason;
    private Long supplierId;
    private BigDecimal price;
    private String remarks;
    private String productCertificate;
    private Long operatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String locationName;
    @TableField(exist = false)
    private String operatorName;
    @TableField(exist = false)
    private String supplierName;
}
