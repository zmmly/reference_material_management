package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final StockMapper stockMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final CategoryMapper categoryMapper;
    private final LocationMapper locationMapper;

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> result = new HashMap<>();

        // 库存总数
        Long totalStock = stockMapper.selectCount(
            new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1)
        );
        result.put("totalStock", totalStock);

        // 标准物质种类数
        Long totalMaterials = materialMapper.selectCount(
            new LambdaQueryWrapper<ReferenceMaterial>().eq(ReferenceMaterial::getStatus, 1)
        );
        result.put("totalMaterials", totalMaterials);

        // 本月入库数量
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long monthIn = stockInMapper.selectCount(
            new LambdaQueryWrapper<StockIn>().ge(StockIn::getCreateTime, monthStart)
        );
        result.put("monthIn", monthIn);

        // 本月出库数量
        Long monthOut = stockOutMapper.selectCount(
            new LambdaQueryWrapper<StockOut>()
                .ge(StockOut::getApplyTime, monthStart)
                .eq(StockOut::getStatus, 1)
        );
        result.put("monthOut", monthOut);

        return result;
    }

    public List<Map<String, Object>> getCategoryStats() {
        List<Category> categories = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>().eq(Category::getParentId, 0)
        );

        return categories.stream().map(cat -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat.getName());

            // 统计该分类下的库存
            List<ReferenceMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<ReferenceMaterial>().eq(ReferenceMaterial::getCategoryId, cat.getId())
            );
            List<Long> materialIds = materials.stream().map(ReferenceMaterial::getId).toList();

            long count = 0;
            BigDecimal totalQty = BigDecimal.ZERO;
            if (!materialIds.isEmpty()) {
                count = stockMapper.selectCount(
                    new LambdaQueryWrapper<Stock>()
                        .in(Stock::getMaterialId, materialIds)
                        .eq(Stock::getStatus, 1)
                );
                // 这里简化处理，实际应该用SQL聚合
            }

            item.put("count", count);
            item.put("quantity", totalQty);
            return item;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLocationStats() {
        List<Location> locations = locationMapper.selectList(null);

        return locations.stream().map(loc -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", loc.getName());
            item.put("code", loc.getCode());

            long count = stockMapper.selectCount(
                new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getLocationId, loc.getId())
                    .eq(Stock::getStatus, 1)
            );
            item.put("count", count);
            return item;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getInOutTrend(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // 入库趋势
        List<StockIn> stockIns = stockInMapper.selectList(
            new LambdaQueryWrapper<StockIn>()
                .between(StockIn::getCreateTime, start, end)
                .orderByAsc(StockIn::getCreateTime)
        );

        // 出库趋势
        List<StockOut> stockOuts = stockOutMapper.selectList(
            new LambdaQueryWrapper<StockOut>()
                .between(StockOut::getApplyTime, start, end)
                .eq(StockOut::getStatus, 1)
                .orderByAsc(StockOut::getApplyTime)
        );

        // 按日期分组
        Map<String, Long> inTrend = stockIns.stream()
            .collect(Collectors.groupingBy(
                si -> si.getCreateTime().toLocalDate().toString(),
                Collectors.counting()
            ));

        Map<String, Long> outTrend = stockOuts.stream()
            .collect(Collectors.groupingBy(
                so -> so.getApplyTime().toLocalDate().toString(),
                Collectors.counting()
            ));

        result.put("inTrend", inTrend);
        result.put("outTrend", outTrend);

        return result;
    }

    public List<Map<String, Object>> getExpiryStats() {
        LocalDate today = LocalDate.now();
        LocalDate warning = today.plusMonths(1);
        LocalDate critical = today.plusDays(7);

        List<Stock> allStock = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .eq(Stock::getStatus, 1)  // 只统计在库的库存，排除已出库
                .isNotNull(Stock::getExpiryDate)
                .gt(Stock::getQuantity, BigDecimal.ZERO)
        );

        int normal = 0, warningCount = 0, criticalCount = 0, expired = 0;

        for (Stock stock : allStock) {
            if (stock.getExpiryDate().isBefore(today)) {
                expired++;
            } else if (stock.getExpiryDate().isBefore(critical)) {
                criticalCount++;
            } else if (stock.getExpiryDate().isBefore(warning)) {
                warningCount++;
            } else {
                normal++;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Object> normalItem = new HashMap<>();
        normalItem.put("name", "正常");
        normalItem.put("value", normal);
        result.add(normalItem);

        Map<String, Object> warningItem = new HashMap<>();
        warningItem.put("name", "即将过期(1个月内)");
        warningItem.put("value", warningCount);
        result.add(warningItem);

        Map<String, Object> criticalItem = new HashMap<>();
        criticalItem.put("name", "紧急(7天内)");
        criticalItem.put("value", criticalCount);
        result.add(criticalItem);

        Map<String, Object> expiredItem = new HashMap<>();
        expiredItem.put("name", "已过期");
        expiredItem.put("value", expired);
        result.add(expiredItem);

        return result;
    }
}
