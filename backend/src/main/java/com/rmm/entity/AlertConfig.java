package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_config")
public class AlertConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String name;
    private Integer threshold;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
