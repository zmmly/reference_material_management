package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.PageResult;
import com.rmm.entity.OperationLog;
import com.rmm.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLog> list(Integer current, Integer size, String username, String module, String action) {
        Page<OperationLog> page = new Page<>(current, size);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), OperationLog::getUsername, username)
               .eq(StringUtils.hasText(module), OperationLog::getModule, module)
               .like(StringUtils.hasText(action), OperationLog::getAction, action)
               .orderByDesc(OperationLog::getCreateTime);

        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);

        PageResult<OperationLog> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public void create(OperationLog log) {
        operationLogMapper.insert(log);
    }
}
