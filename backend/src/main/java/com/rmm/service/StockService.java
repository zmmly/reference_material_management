package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final StockOutMapper stockOutMapper;

    public PageResult<Stock> list(Integer current, Integer size, String keyword, Long locationId, Integer status) {
        Page<Stock> page = new Page<>(current, size);

        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(locationId != null, Stock::getLocationId, locationId)
               .eq(status != null, Stock::getStatus, status)
               .orderByDesc(Stock::getUpdateTime);

        Page<Stock> result = stockMapper.selectPage(page, wrapper);

        // 获取所有库存ID
        List<Long> stockIds = result.getRecords().stream()
                .map(Stock::getId)
                .toList();

        // 查询有待审批出库申请的库存ID集合
        Set<Long> pendingStockIds = getPendingStockIds(stockIds);

        result.getRecords().forEach(stock -> {
            fillRelations(stock);
            stock.setHasPendingOut(pendingStockIds.contains(stock.getId()));
        });

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

    /**
     * 获取有待审批出库申请的库存ID集合
     */
    private Set<Long> getPendingStockIds(List<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return Set.of();
        }
        List<StockOut> pendingOuts = stockOutMapper.selectList(
            new LambdaQueryWrapper<StockOut>()
                .in(StockOut::getStockId, stockIds)
                .eq(StockOut::getStatus, 0)
        );
        return pendingOuts.stream()
                .map(StockOut::getStockId)
                .collect(Collectors.toSet());
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
