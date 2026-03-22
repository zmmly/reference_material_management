package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock")
public class Stock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String batchNo;
    private String internalCode;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private Long locationId;
    private Integer status;
    private LocalDateTime lastOutTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String casNumber;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String locationName;
    @TableField(exist = false)
    private Boolean hasPendingOut;
    @TableField(exist = false)
    private Boolean hasApprovedOut;
}
