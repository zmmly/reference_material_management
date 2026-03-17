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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCheckService {

    private final StockCheckMapper stockCheckMapper;
    private final StockCheckItemMapper stockCheckItemMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final UserMapper userMapper;

    public PageResult<StockCheck> list(Integer current, Integer size, Integer status) {
        Page<StockCheck> page = new Page<>(current, size);

        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, StockCheck::getStatus, status)
               .orderByDesc(StockCheck::getCreateTime);

        Page<StockCheck> result = stockCheckMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillRelations);

        PageResult<StockCheck> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public StockCheck getById(Long id) {
        StockCheck stockCheck = stockCheckMapper.selectById(id);
        if (stockCheck != null) {
            fillRelations(stockCheck);
        }
        return stockCheck;
    }

    public List<StockCheckItem> getItems(Long checkId) {
        List<StockCheckItem> items = stockCheckItemMapper.selectList(
            new LambdaQueryWrapper<StockCheckItem>().eq(StockCheckItem::getCheckId, checkId)
        );
        items.forEach(this::fillItemRelations);
        return items;
    }

    @Transactional
    public StockCheck create(StockCheck stockCheck, Long creatorId) {
        // 生成盘点单号
        String checkNo = "PD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        + String.format("%04d", stockCheckMapper.selectCount(null) + 1);
        stockCheck.setCheckNo(checkNo);
        stockCheck.setStatus(0);
        stockCheck.setCreatorId(creatorId);
        stockCheck.setTotalCount(0);
        stockCheck.setCheckedCount(0);
        stockCheck.setDifferenceCount(0);
        stockCheckMapper.insert(stockCheck);

        // 根据范围创建盘点明细
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getStatus, 1);

        if ("CATEGORY".equals(stockCheck.getScope()) && stockCheck.getScopeValue() != null) {
            // 按分类盘点
            List<Long> materialIds = materialMapper.selectList(
                new LambdaQueryWrapper<ReferenceMaterial>()
                    .eq(ReferenceMaterial::getCategoryId, Long.parseLong(stockCheck.getScopeValue()))
            ).stream().map(ReferenceMaterial::getId).toList();
            wrapper.in(Stock::getMaterialId, materialIds);
        } else if ("LOCATION".equals(stockCheck.getScope()) && stockCheck.getScopeValue() != null) {
            wrapper.eq(Stock::getLocationId, Long.parseLong(stockCheck.getScopeValue()));
        }

        List<Stock> stocks = stockMapper.selectList(wrapper);
        int total = 0;

        for (Stock stock : stocks) {
            StockCheckItem item = new StockCheckItem();
            item.setCheckId(stockCheck.getId());
            item.setStockId(stock.getId());
            item.setMaterialId(stock.getMaterialId());
            item.setSystemQuantity(stock.getQuantity());
            item.setStatus(0);
            stockCheckItemMapper.insert(item);
            total++;
        }

        stockCheck.setTotalCount(total);
        stockCheckMapper.updateById(stockCheck);

        return stockCheck;
    }

    @Transactional
    public void checkItem(Long itemId, BigDecimal actualQuantity, String differenceReason, Long checkerId) {
        StockCheckItem item = stockCheckItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("盘点明细不存在");
        }
        if (item.getStatus() != 0) {
            throw new BusinessException("该明细已盘点");
        }

        item.setActualQuantity(actualQuantity);
        item.setDifference(actualQuantity.subtract(item.getSystemQuantity()));
        item.setDifferenceReason(differenceReason);
        item.setStatus(item.getDifference().compareTo(BigDecimal.ZERO) == 0 ? 1 : 2);
        item.setCheckerId(checkerId);
        item.setCheckTime(LocalDateTime.now());
        stockCheckItemMapper.updateById(item);

        // 更新盘点任务统计
        StockCheck stockCheck = stockCheckMapper.selectById(item.getCheckId());
        stockCheck.setCheckedCount(stockCheck.getCheckedCount() + 1);
        if (item.getStatus() == 2) {
            stockCheck.setDifferenceCount(stockCheck.getDifferenceCount() + 1);
        }
        stockCheckMapper.updateById(stockCheck);
    }

    @Transactional
    public void complete(Long id) {
        StockCheck stockCheck = stockCheckMapper.selectById(id);
        if (stockCheck == null) {
            throw new BusinessException("盘点任务不存在");
        }
        if (stockCheck.getCheckedCount() < stockCheck.getTotalCount()) {
            throw new BusinessException("还有未盘点的项目");
        }

        stockCheck.setStatus(1);
        stockCheck.setCompleteTime(LocalDateTime.now());
        stockCheckMapper.updateById(stockCheck);
    }

    @Transactional
    public void adjust(Long itemId, String reason, Long operatorId) {
        StockCheckItem item = stockCheckItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("盘点明细不存在");
        }
        if (item.getStatus() != 2) {
            throw new BusinessException("只有有差异的项目才能调整");
        }

        // 调整库存
        Stock stock = stockMapper.selectById(item.getStockId());
        if (stock != null) {
            stock.setQuantity(item.getActualQuantity());
            stockMapper.updateById(stock);
        }

        // 更新明细状态
        item.setStatus(1);
        item.setDifferenceReason(item.getDifferenceReason() + " [已调整:" + reason + "]");
        stockCheckItemMapper.updateById(item);

        // 更新盘点任务差异计数
        StockCheck stockCheck = stockCheckMapper.selectById(item.getCheckId());
        stockCheck.setDifferenceCount(stockCheck.getDifferenceCount() - 1);
        stockCheckMapper.updateById(stockCheck);
    }

    private void fillRelations(StockCheck stockCheck) {
        if (stockCheck.getCreatorId() != null) {
            User user = userMapper.selectById(stockCheck.getCreatorId());
            if (user != null) {
                stockCheck.setCreatorName(user.getRealName());
            }
        }
    }

    private void fillItemRelations(StockCheckItem item) {
        if (item.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(item.getMaterialId());
            if (material != null) {
                item.setMaterialName(material.getName());
            }
        }
        if (item.getStockId() != null) {
            Stock stock = stockMapper.selectById(item.getStockId());
            if (stock != null) {
                item.setInternalCode(stock.getInternalCode());
                item.setBatchNo(stock.getBatchNo());
                if (stock.getLocationId() != null) {
                    Location location = locationMapper.selectById(stock.getLocationId());
                    if (location != null) {
                        item.setLocationName(location.getName());
                    }
                }
            }
        }
        if (item.getCheckerId() != null) {
            User user = userMapper.selectById(item.getCheckerId());
            if (user != null) {
                item.setCheckerName(user.getRealName());
            }
        }
    }
}
