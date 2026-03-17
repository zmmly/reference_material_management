package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.entity.Metadata;
import com.rmm.mapper.MetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final MetadataMapper metadataMapper;

    public List<Metadata> listByType(String type) {
        return metadataMapper.selectList(
            new LambdaQueryWrapper<Metadata>()
                .eq(Metadata::getType, type)
                .eq(Metadata::getStatus, 1)
                .orderByAsc(Metadata::getSortOrder)
        );
    }

    public List<Metadata> listAll() {
        return metadataMapper.selectList(
            new LambdaQueryWrapper<Metadata>()
                .eq(Metadata::getStatus, 1)
                .orderByAsc(Metadata::getType)
                .orderByAsc(Metadata::getSortOrder)
        );
    }

    public void create(Metadata metadata) {
        metadata.setStatus(1);
        metadataMapper.insert(metadata);
    }

    public void update(Metadata metadata) {
        metadataMapper.updateById(metadata);
    }

    public void delete(Long id) {
        metadataMapper.deleteById(id);
    }
}
