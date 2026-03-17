package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockInService {

    private final StockInMapper stockInMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final UserMapper userMapper;

    public PageResult<StockIn> list(Integer current, Integer size, String keyword, String reason, String startDate, String endDate) {
        Page<StockIn> page = new Page<>(current, size);

        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(reason), StockIn::getReason, reason)
               .ge(StringUtils.hasText(startDate), StockIn::getCreateTime, startDate + " 00:00:00")
               .le(StringUtils.hasText(endDate), StockIn::getCreateTime, endDate + " 23:59:59")
               .orderByDesc(StockIn::getCreateTime);

        Page<StockIn> result = stockInMapper.selectPage(page, wrapper);

        result.getRecords().forEach(this::fillRelations);

        PageResult<StockIn> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Transactional
    public void create(StockIn stockIn, Long operatorId) {
        ReferenceMaterial material = materialMapper.selectById(stockIn.getMaterialId());
        if (material == null) {
            throw new BusinessException("标准物质不存在");
        }

        stockIn.setOperatorId(operatorId);
        stockInMapper.insert(stockIn);

        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
            .eq(Stock::getMaterialId, stockIn.getMaterialId())
            .eq(Stock::getBatchNo, stockIn.getBatchNo())
            .eq(Stock::getLocationId, stockIn.getLocationId())
        );

        if (stock == null) {
            stock = new Stock();
            stock.setMaterialId(stockIn.getMaterialId());
            stock.setBatchNo(stockIn.getBatchNo());
            stock.setInternalCode(stockIn.getInternalCode());
            stock.setExpiryDate(stockIn.getExpiryDate());
            stock.setQuantity(stockIn.getQuantity());
            stock.setLocationId(stockIn.getLocationId());
            stock.setStatus(1);
            stockMapper.insert(stock);

            stockIn.setStockId(stock.getId());
            stockInMapper.updateById(stockIn);
        } else {
            stock.setQuantity(stock.getQuantity().add(stockIn.getQuantity()));
            if (stockIn.getExpiryDate() != null) {
                stock.setExpiryDate(stockIn.getExpiryDate());
            }
            stockMapper.updateById(stock);

            stockIn.setStockId(stock.getId());
            stockInMapper.updateById(stockIn);
        }
    }

    private void fillRelations(StockIn stockIn) {
        if (stockIn.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stockIn.getMaterialId());
            if (material != null) {
                stockIn.setMaterialName(material.getName());
            }
        }
        if (stockIn.getLocationId() != null) {
            Location location = locationMapper.selectById(stockIn.getLocationId());
            if (location != null) {
                stockIn.setLocationName(location.getName());
            }
        }
        if (stockIn.getOperatorId() != null) {
            User user = userMapper.selectById(stockIn.getOperatorId());
            if (user != null) {
                stockIn.setOperatorName(user.getRealName());
            }
        }
    }
}
