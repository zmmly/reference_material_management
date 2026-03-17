package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.Category;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.mapper.CategoryMapper;
import com.rmm.mapper.ReferenceMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferenceMaterialService {

    private final ReferenceMaterialMapper materialMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<ReferenceMaterial> list(Integer current, Integer size, String name, Long categoryId, Integer status) {
        Page<ReferenceMaterial> page = new Page<>(current, size);
        LambdaQueryWrapper<ReferenceMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), ReferenceMaterial::getName, name)
               .eq(categoryId != null, ReferenceMaterial::getCategoryId, categoryId)
               .eq(status != null, ReferenceMaterial::getStatus, status)
               .orderByDesc(ReferenceMaterial::getCreateTime);

        Page<ReferenceMaterial> result = materialMapper.selectPage(page, wrapper);

        // 填充分类名称
        result.getRecords().forEach(this::fillCategoryName);

        PageResult<ReferenceMaterial> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public List<ReferenceMaterial> listAll() {
        List<ReferenceMaterial> list = materialMapper.selectList(
            new LambdaQueryWrapper<ReferenceMaterial>()
                .eq(ReferenceMaterial::getStatus, 1)
                .orderByAsc(ReferenceMaterial::getName)
        );
        list.forEach(this::fillCategoryName);
        return list;
    }

    public ReferenceMaterial getById(Long id) {
        ReferenceMaterial material = materialMapper.selectById(id);
        if (material != null) {
            fillCategoryName(material);
        }
        return material;
    }

    public void create(ReferenceMaterial material) {
        if (materialMapper.selectCount(new LambdaQueryWrapper<ReferenceMaterial>()
                .eq(ReferenceMaterial::getCode, material.getCode())) > 0) {
            throw new BusinessException("编号已存在");
        }
        material.setStatus(1);
        materialMapper.insert(material);
    }

    public void update(ReferenceMaterial material) {
        ReferenceMaterial existing = materialMapper.selectById(material.getId());
        if (existing == null) {
            throw new BusinessException("标准物质不存在");
        }
        if (!existing.getCode().equals(material.getCode())) {
            if (materialMapper.selectCount(new LambdaQueryWrapper<ReferenceMaterial>()
                    .eq(ReferenceMaterial::getCode, material.getCode())) > 0) {
                throw new BusinessException("编号已存在");
            }
        }
        materialMapper.updateById(material);
    }

    public void delete(Long id) {
        materialMapper.deleteById(id);
    }

    private void fillCategoryName(ReferenceMaterial material) {
        if (material.getCategoryId() != null) {
            Category category = categoryMapper.selectById(material.getCategoryId());
            if (category != null) {
                material.setCategoryName(category.getName());
            }
        }
    }
}
