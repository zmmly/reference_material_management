package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private Long stockId;
    private String internalCodes;  // 新增字段：存储所有关联库存的内部编号，逗号分隔
    private Long materialId;
    private String content;
    private Integer level;
    private Integer status;
    private Long handlerId;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String internalCode;
    @TableField(exist = false)
    private String handlerName;
}
