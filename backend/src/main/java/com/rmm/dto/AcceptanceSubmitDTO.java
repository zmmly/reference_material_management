package com.rmm.dto;

import lombok.Data;

@Data
public class AcceptanceSubmitDTO {
    private Integer packageIntact;     // 外包装是否完好
    private Integer labelComplete;      // 标签是否完整
    private Integer hasDamage;          // 有无破损
    private java.math.BigDecimal actualQuantity; // 实际到货数量
    private java.time.LocalDate expiryDate;     // 有效期
    private Long locationId;                    // 存放位置ID
    private Integer result;              // 验收结果
    private String remark;               // 备注
}
