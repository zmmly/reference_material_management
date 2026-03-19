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

import com.rmm.vo.StockCheckItemGroupVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 获取按批号分组的盘点明细
     */
    public List<StockCheckItemGroupVO> getItemsGrouped(Long checkId) {
        List<StockCheckItem> items = getItems(checkId);

        // 按批号+位置分组
        Map<String, List<StockCheckItem>> grouped = items.stream()
            .collect(Collectors.groupingBy(item -> {
                String batchNo = item.getBatchNo() != null ? item.getBatchNo() : "";
                String locationName = item.getLocationName() != null ? item.getLocationName() : "";
                return batchNo + "_" + locationName;
            }));

        List<StockCheckItemGroupVO> result = new ArrayList<>();
        for (Map.Entry<String, List<StockCheckItem>> entry : grouped.entrySet()) {
            List<StockCheckItem> groupItems = entry.getValue();
            StockCheckItem first = groupItems.get(0);

            StockCheckItemGroupVO vo = new StockCheckItemGroupVO();
            vo.setGroupKey(entry.getKey());
            vo.setMaterialId(first.getMaterialId());
            vo.setMaterialName(first.getMaterialName());
            vo.setBatchNo(first.getBatchNo());
            vo.setLocationName(first.getLocationName());

            // 合并内部编码
            String internalCodes = groupItems.stream()
                .map(StockCheckItem::getInternalCode)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(", "));
            vo.setInternalCodes(internalCodes);

            // 收集item ID
            List<Long> itemIds = groupItems.stream()
                .map(StockCheckItem::getId)
                .collect(Collectors.toList());
            vo.setItemIds(itemIds);
            vo.setItemCount(groupItems.size());

            // 合计系统数量
            BigDecimal systemQty = groupItems.stream()
                .map(StockCheckItem::getSystemQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setSystemQuantity(systemQty);

            // 合计实盘数量(已盘点的)
            BigDecimal actualQty = groupItems.stream()
                .filter(i -> i.getActualQuantity() != null)
                .map(StockCheckItem::getActualQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setActualQuantity(actualQty.compareTo(BigDecimal.ZERO) == 0 ? null : actualQty);

            // 合计差异
            BigDecimal diff = groupItems.stream()
                .filter(i -> i.getDifference() != null)
                .map(StockCheckItem::getDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setDifference(diff.compareTo(BigDecimal.ZERO) == 0 ? null : diff);

            // 差异说明(取第一个有差异说明的)
            String diffReason = groupItems.stream()
                .filter(i -> i.getDifferenceReason() != null && !i.getDifferenceReason().isEmpty())
                .map(StockCheckItem::getDifferenceReason)
                .findFirst()
                .orElse(null);
            vo.setDifferenceReason(diffReason);

            // 状态: 只有全部已盘点才算已盘点
            boolean allChecked = groupItems.stream().allMatch(i -> i.getStatus() > 0);
            boolean hasDiff = groupItems.stream().anyMatch(i -> i.getStatus() == 2);
            if (!allChecked) {
                vo.setStatus(0);
            } else {
                vo.setStatus(hasDiff ? 2 : 1);
            }

            // 盘点人
            String checkerName = groupItems.stream()
                .filter(i -> i.getCheckerName() != null)
                .map(StockCheckItem::getCheckerName)
                .findFirst()
                .orElse(null);
            vo.setCheckerName(checkerName);

            // 盘点时间
            String checkTime = groupItems.stream()
                .filter(i -> i.getCheckTime() != null)
                .map(i -> i.getCheckTime().toString())
                .findFirst()
                .orElse(null);
            vo.setCheckTime(checkTime);

            result.add(vo);
        }

        // 按批号排序
        result.sort(Comparator.comparing(StockCheckItemGroupVO::getBatchNo,
            Comparator.nullsLast(String::compareTo)));

        return result;
    }

    /**
     * 批量盘点(按组盘点)
     */
    @Transactional
    public void checkBatch(List<Long> itemIds, BigDecimal actualQuantity, String differenceReason, Long checkerId) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessException("盘点明细不能为空");
        }

        // 获取所有明细
        List<StockCheckItem> items = stockCheckItemMapper.selectBatchIds(itemIds);
        if (items.isEmpty()) {
            throw new BusinessException("盘点明细不存在");
        }

        // 计算系统总数量
        BigDecimal systemQty = items.stream()
            .map(StockCheckItem::getSystemQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按比例分配实盘数量到每个明细
        for (StockCheckItem item : items) {
            if (item.getStatus() > 0) {
                continue; // 跳过已盘点的
            }

            // 按比例分配: itemActual = itemSystem * (actualQuantity / systemQty)
            BigDecimal itemActual;
            if (systemQty.compareTo(BigDecimal.ZERO) > 0 && item.getSystemQuantity() != null) {
                itemActual = item.getSystemQuantity()
                    .multiply(actualQuantity)
                    .divide(systemQty, 1, RoundingMode.HALF_UP);
            } else {
                itemActual = BigDecimal.ZERO;
            }

            // 设置盘点结果
            item.setActualQuantity(itemActual);
            item.setDifference(itemActual.subtract(item.getSystemQuantity() != null ? item.getSystemQuantity() : BigDecimal.ZERO));
            item.setDifferenceReason(differenceReason);
            item.setStatus(item.getDifference().compareTo(BigDecimal.ZERO) == 0 ? 1 : 2);
            item.setCheckerId(checkerId);
            item.setCheckTime(LocalDateTime.now());
            stockCheckItemMapper.updateById(item);
        }

        // 更新盘点任务统计
        Long checkId = items.get(0).getCheckId();
        StockCheck stockCheck = stockCheckMapper.selectById(checkId);

        // 重新计算已盘点数量
        Long checkedCount = stockCheckItemMapper.selectCount(
            new LambdaQueryWrapper<StockCheckItem>()
                .eq(StockCheckItem::getCheckId, checkId)
                .gt(StockCheckItem::getStatus, 0)
        );
        stockCheck.setCheckedCount(checkedCount.intValue());

        // 重新计算差异数量
        Long diffCount = stockCheckItemMapper.selectCount(
            new LambdaQueryWrapper<StockCheckItem>()
                .eq(StockCheckItem::getCheckId, checkId)
                .eq(StockCheckItem::getStatus, 2)
        );
        stockCheck.setDifferenceCount(diffCount.intValue());

        stockCheckMapper.updateById(stockCheck);
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
        // 计算分组统计
        fillGroupStats(stockCheck);
    }

    /**
     * 计算并填充分组统计数据
     */
    private void fillGroupStats(StockCheck stockCheck) {
        List<StockCheckItem> items = stockCheckItemMapper.selectList(
            new LambdaQueryWrapper<StockCheckItem>().eq(StockCheckItem::getCheckId, stockCheck.getId())
        );

        // 填充明细关联信息
        items.forEach(this::fillItemRelations);

        // 按批号+位置分组
        Map<String, List<StockCheckItem>> grouped = items.stream()
            .collect(Collectors.groupingBy(item -> {
                String batchNo = item.getBatchNo() != null ? item.getBatchNo() : "";
                String locationName = item.getLocationName() != null ? item.getLocationName() : "";
                return batchNo + "_" + locationName;
            }));

        // 计算分组总数
        stockCheck.setGroupCount(grouped.size());

        // 计算已盘点分组数（分组内所有明细都已盘点）
        int checkedGroupCount = 0;
        for (List<StockCheckItem> groupItems : grouped.values()) {
            boolean allChecked = groupItems.stream().allMatch(i -> i.getStatus() > 0);
            if (allChecked) {
                checkedGroupCount++;
            }
        }
        stockCheck.setCheckedGroupCount(checkedGroupCount);
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
