package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.PageResult;
import com.rmm.entity.Location;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.entity.Stock;
import com.rmm.mapper.LocationMapper;
import com.rmm.mapper.ReferenceMaterialMapper;
import com.rmm.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;

    public PageResult<Stock> list(Integer current, Integer size, String keyword, Long locationId, Integer status) {
        Page<Stock> page = new Page<>(current, size);

        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(locationId != null, Stock::getLocationId, locationId)
               .eq(status != null, Stock::getStatus, status)
               .orderByDesc(Stock::getUpdateTime);

        Page<Stock> result = stockMapper.selectPage(page, wrapper);

        // 填充关联信息
        result.getRecords().forEach(this::fillRelations);

        // 关键字过滤
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase();
            List<Stock> filtered = result.getRecords().stream()
                .filter(s -> (s.getMaterialName() != null && s.getMaterialName().toLowerCase().contains(kw))
                          || (s.getInternalCode() != null && s.getInternalCode().toLowerCase().contains(kw))
                          || (s.getBatchNo() != null && s.getBatchNo().toLowerCase().contains(kw)))
                .toList();
            result.setRecords(filtered);
        }

        PageResult<Stock> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public List<Stock> listAll() {
        List<Stock> list = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1)
        );
        list.forEach(this::fillRelations);
        return list;
    }

    public Stock getById(Long id) {
        Stock stock = stockMapper.selectById(id);
        if (stock != null) {
            fillRelations(stock);
        }
        return stock;
    }

    public void updateStatus() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusMonths(1);

        // 更新即将过期状态
        stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .between(Stock::getExpiryDate, today, warningDate)
                .ne(Stock::getStatus, 3))
            .forEach(stock -> {
                stock.setStatus(2);
                stockMapper.updateById(stock);
            });

        // 更新已过期状态
        stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .lt(Stock::getExpiryDate, today)
                .ne(Stock::getStatus, 3))
            .forEach(stock -> {
                stock.setStatus(3);
                stockMapper.updateById(stock);
            });
    }

    private void fillRelations(Stock stock) {
        if (stock.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stock.getMaterialId());
            if (material != null) {
                stock.setMaterialName(material.getName());
                stock.setMaterialCode(material.getCode());
            }
        }
        if (stock.getLocationId() != null) {
            Location location = locationMapper.selectById(stock.getLocationId());
            if (location != null) {
                stock.setLocationName(location.getName());
            }
        }
    }
}
