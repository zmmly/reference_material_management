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
import java.time.LocalDate;
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
    private final SupplierMapper supplierMapper;

    public PageResult<Stock> list(Integer current, Integer size, String keyword, Long locationId, Integer status) {
        Page<Stock> page = new Page<>(current, size);

        // 注意：不在此处按 status 筛选，因为状态需要动态计算
        // 只排除已出库的记录（status=0）在非明确查询已出库时
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(locationId != null, Stock::getLocationId, locationId)
               .ne(status == null || status > 0, Stock::getStatus, 0)  // 默认排除已出库
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
            // 动态计算并更新状态（基于有效期）
            updateStatusByExpiryDate(stock);
            stock.setHasPendingOut(pendingStockIds.contains(stock.getId()));
            // 已出库：状态为0表示已出库
            stock.setHasApprovedOut(stock.getStatus() != null && stock.getStatus() == 0);
        });

        // 关键字筛选
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase();
            List<Stock> filtered = result.getRecords().stream()
                .filter(s -> (s.getMaterialName() != null && s.getMaterialName().toLowerCase().contains(kw))
                          || (s.getInternalCode() != null && s.getInternalCode().toLowerCase().contains(kw))
                          || (s.getBatchNo() != null && s.getBatchNo().toLowerCase().contains(kw)))
                .toList();
            result.setRecords(filtered);
        }

        // 按动态计算后的状态筛选
        if (status != null && status > 0) {
            List<Stock> filtered = result.getRecords().stream()
                .filter(s -> status.equals(s.getStatus()))
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
     * 根据有效期动态计算库存状态
     * 状态定义：1=正常, 2=即将过期(30天内), 3=已过期
     * 注意：已出库状态(status=0)保持不变
     */
    private void updateStatusByExpiryDate(Stock stock) {
        // 已出库状态保持不变
        if (stock.getStatus() != null && stock.getStatus() == 0) {
            return;
        }

        LocalDate expiryDate = stock.getExpiryDate();
        if (expiryDate == null) {
            stock.setStatus(1);  // 无有效期默认正常
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(30);  // 30天后为即将过期阈值

        if (expiryDate.isBefore(today)) {
            stock.setStatus(3);  // 已过期
        } else if (!expiryDate.isAfter(warningDate)) {
            stock.setStatus(2);  // 即将过期（有效期在今天和30天后之间，含30天后）
        } else {
            stock.setStatus(1);  // 正常
        }
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
        // 获取所有未出库的库存
        List<Stock> list = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>().ne(Stock::getStatus, 0)
        );
        list.forEach(stock -> {
            fillRelations(stock);
            updateStatusByExpiryDate(stock);
        });
        return list;
    }

    public Stock getById(Long id) {
        Stock stock = stockMapper.selectById(id);
        if (stock != null) {
            fillRelations(stock);
            updateStatusByExpiryDate(stock);
        }
        return stock;
    }

    private void fillRelations(Stock stock) {
        if (stock.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stock.getMaterialId());
            if (material != null) {
                stock.setMaterialName(material.getName());
                stock.setMaterialCode(material.getCode());
                stock.setCasNumber(material.getCasNumber());

                // 填充供应商名称
                if (material.getSupplierId() != null) {
                    Supplier supplier = supplierMapper.selectById(material.getSupplierId());
                    if (supplier != null) {
                        stock.setSupplierName(supplier.getName());
                    }
                }
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
