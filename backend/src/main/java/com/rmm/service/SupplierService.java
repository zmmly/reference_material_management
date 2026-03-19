package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.PageResult;
import com.rmm.entity.Supplier;
import com.rmm.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public PageResult<Supplier> list(Integer current, Integer size, String keyword, Integer status) {
        Page<Supplier> page = new Page<>(current, size);

        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Supplier::getName, keyword)
               .eq(status != null, Supplier::getStatus, status)
               .orderByDesc(Supplier::getCreateTime);

        Page<Supplier> result = supplierMapper.selectPage(page, wrapper);

        PageResult<Supplier> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public List<Supplier> listAll() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, 1)
               .orderByAsc(Supplier::getName);
        return supplierMapper.selectList(wrapper);
    }

    public Supplier getById(Long id) {
        return supplierMapper.selectById(id);
    }

    public void create(Supplier supplier) {
        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }
        supplierMapper.insert(supplier);
    }

    public void update(Long id, Supplier supplier) {
        supplier.setId(id);
        supplierMapper.updateById(supplier);
    }

    public void delete(Long id) {
        supplierMapper.deleteById(id);
    }
}
