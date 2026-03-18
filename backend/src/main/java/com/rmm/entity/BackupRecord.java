package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("backup_record")
public class BackupRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String filename;
    private Long fileSize;
    private LocalDateTime backupTime;
    private Long operatorId;
    private String operatorName;
}
