package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BackupMapper extends BaseMapper<BackupRecord> {
}
