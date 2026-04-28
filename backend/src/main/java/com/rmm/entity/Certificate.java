package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("certificate")
public class Certificate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String batchNo;
    private String filePath;
    private String fileName;
    private Long uploaderId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 非持久化字段
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String uploaderName;
}
