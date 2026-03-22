package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reference_material")
public class ReferenceMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 标准物质编号 */
    private String code;
    /** 标准物质名称 */
    private String name;
    /** 英文名称 */
    private String englishName;
    /** CAS号 */
    private String casNumber;
    /** 规格 */
    private String specification;
    /** 纯度/浓度 */
    private String purityConcentration;
    /** 基质 */
    private String matrix;
    /** 包装形式 */
    private String packageForm;
    /** 分类ID */
    private Long categoryId;
    /** 供应商ID */
    private Long supplierId;
    /** 状态 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String supplierName;
}
