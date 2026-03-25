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
    private final StockCheckGroupMapper stockCheckGroupMapper;
    private final StockCheckItemStockMapper stockCheckItemStockMapper;
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

    /**
     * 获取盘点分组列表
     */
    public List<StockCheckGroup> getGroups(Long checkId) {
        List<StockCheckGroup> groups = stockCheckGroupMapper.selectList(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, checkId)
                .orderByAsc(StockCheckGroup::getBatchNo)
        );
        groups.forEach(this::fillGroupRelations);
        return groups;
    }

    /**
     * 创建盘点任务（按批号+位置分组）
     */
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

        // 根据范围查询库存
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getStatus, 1);

        if ("CATEGORY".equals(stockCheck.getScope()) && stockCheck.getScopeValue() != null) {
            List<Long> materialIds = materialMapper.selectList(
                new LambdaQueryWrapper<ReferenceMaterial>()
                    .eq(ReferenceMaterial::getCategoryId, Long.parseLong(stockCheck.getScopeValue()))
            ).stream().map(ReferenceMaterial::getId).toList();
            wrapper.in(Stock::getMaterialId, materialIds);
        } else if ("LOCATION".equals(stockCheck.getScope()) && stockCheck.getScopeValue() != null) {
            wrapper.eq(Stock::getLocationId, Long.parseLong(stockCheck.getScopeValue()));
        }

        List<Stock> stocks = stockMapper.selectList(wrapper);

        // 按批号+位置分组
        Map<String, List<Stock>> grouped = stocks.stream()
            .collect(Collectors.groupingBy(stock -> {
                String batchNo = stock.getBatchNo() != null ? stock.getBatchNo() : "";
                String locationId = stock.getLocationId() != null ? stock.getLocationId().toString() : "";
                return batchNo + "_" + locationId;
            }));

        int groupCount = 0;
        for (Map.Entry<String, List<Stock>> entry : grouped.entrySet()) {
            List<Stock> groupStocks = entry.getValue();
            Stock first = groupStocks.get(0);

            // 创建分组记录
            StockCheckGroup group = new StockCheckGroup();
            group.setCheckId(stockCheck.getId());
            group.setMaterialId(first.getMaterialId());
            group.setBatchNo(first.getBatchNo());
            group.setLocationId(first.getLocationId());

            // 获取位置名称
            if (first.getLocationId() != null) {
                Location location = locationMapper.selectById(first.getLocationId());
                if (location != null) {
                    group.setLocationName(location.getName());
                }
            }

            // 合并内部编码
            String internalCodes = groupStocks.stream()
                .map(Stock::getInternalCode)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(", "));
            group.setInternalCodes(internalCodes);

            // 统计明细数量
            group.setItemCount(groupStocks.size());

            // 合计系统数量（转为整数）
            int systemQty = groupStocks.stream()
                .map(Stock::getQuantity)
                .filter(Objects::nonNull)
                .map(q -> q.intValue())
                .reduce(0, Integer::sum);
            group.setSystemQuantity(systemQty);

            group.setStatus(0);
            group.setCreateTime(LocalDateTime.now());
            stockCheckGroupMapper.insert(group);

            // 创建分组与库存的关联
            for (Stock stock : groupStocks) {
                StockCheckItemStock itemStock = new StockCheckItemStock();
                itemStock.setCheckId(stockCheck.getId());
                itemStock.setGroupId(group.getId());
                itemStock.setStockId(stock.getId());
                itemStock.setSystemQuantity(stock.getQuantity() != null ? stock.getQuantity().intValue() : 0);
                itemStock.setCreateTime(LocalDateTime.now());
                stockCheckItemStockMapper.insert(itemStock);
            }

            groupCount++;
        }

        // 更新盘点任务统计
        stockCheck.setTotalCount(groupCount);
        stockCheck.setGroupCount(groupCount);
        stockCheck.setCheckedGroupCount(0);
        stockCheckMapper.updateById(stockCheck);

        return stockCheck;
    }

    /**
     * 盘点分组
     */
    @Transactional
    public void checkGroup(Long groupId, Integer actualQuantity, String differenceReason, Long checkerId) {
        StockCheckGroup group = stockCheckGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("盘点分组不存在");
        }
        if (group.getStatus() > 0) {
            throw new BusinessException("该分组已盘点");
        }

        // 设置盘点结果
        group.setActualQuantity(actualQuantity);
        int systemQty = group.getSystemQuantity() != null ? group.getSystemQuantity() : 0;
        group.setDifference(actualQuantity - systemQty);
        group.setDifferenceReason(differenceReason);
        group.setStatus(group.getDifference() == 0 ? 1 : 2);
        group.setCheckerId(checkerId);
        group.setCheckTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        stockCheckGroupMapper.updateById(group);

        // 更新盘点任务统计
        updateCheckStats(group.getCheckId());
    }

    /**
     * 完成盘点
     */
    @Transactional
    public void complete(Long id, Long userId) {
        StockCheck stockCheck = stockCheckMapper.selectById(id);
        if (stockCheck == null) {
            throw new BusinessException("盘点任务不存在");
        }

        // 检查是否所有分组都已盘点
        Long uncheckedCount = stockCheckGroupMapper.selectCount(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, id)
                .eq(StockCheckGroup::getStatus, 0)
        );
        if (uncheckedCount > 0) {
            throw new BusinessException("还有未盘点的分组");
        }

        // 设置盘点人（如果还没设置）
        if (stockCheck.getCheckerId() == null) {
            stockCheck.setCheckerId(userId);
        }

        stockCheck.setStatus(1);
        stockCheck.setCompleteTime(LocalDateTime.now());
        stockCheckMapper.updateById(stockCheck);
    }

    /**
     * 调整库存
     */
    @Transactional
    public void adjust(Long groupId, String reason, Long operatorId) {
        StockCheckGroup group = stockCheckGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("盘点分组不存在");
        }
        if (group.getStatus() != 2) {
            throw new BusinessException("只有有差异的分组才能调整");
        }

        // 获取该分组关联的所有库存
        List<StockCheckItemStock> itemStocks = stockCheckItemStockMapper.selectList(
            new LambdaQueryWrapper<StockCheckItemStock>()
                .eq(StockCheckItemStock::getGroupId, groupId)
        );

        // 按比例调整每个库存的数量
        int totalSystemQty = group.getSystemQuantity() != null ? group.getSystemQuantity() : 0;
        int totalActualQty = group.getActualQuantity() != null ? group.getActualQuantity() : 0;

        for (StockCheckItemStock itemStock : itemStocks) {
            Stock stock = stockMapper.selectById(itemStock.getStockId());
            if (stock != null && totalSystemQty > 0) {
                // 按比例分配: stockActual = stockSystem * (totalActual / totalSystem)
                BigDecimal newQty = BigDecimal.valueOf(itemStock.getSystemQuantity())
                    .multiply(BigDecimal.valueOf(totalActualQty))
                    .divide(BigDecimal.valueOf(totalSystemQty), 0, RoundingMode.HALF_UP);
                stock.setQuantity(newQty);
                stock.setUpdateTime(LocalDateTime.now());
                stockMapper.updateById(stock);
            }
        }

        // 更新分组状态
        group.setStatus(1);
        group.setDifferenceReason(group.getDifferenceReason() + " [已调整:" + reason + "]");
        group.setUpdateTime(LocalDateTime.now());
        stockCheckGroupMapper.updateById(group);

        // 更新盘点任务统计
        updateCheckStats(group.getCheckId());
    }

    /**
     * 更新盘点任务统计
     */
    private void updateCheckStats(Long checkId) {
        StockCheck stockCheck = stockCheckMapper.selectById(checkId);

        // 统计已盘点分组数
        Long checkedCount = stockCheckGroupMapper.selectCount(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, checkId)
                .gt(StockCheckGroup::getStatus, 0)
        );
        stockCheck.setCheckedCount(checkedCount.intValue());
        stockCheck.setCheckedGroupCount(checkedCount.intValue());

        // 统计有差异的分组数
        Long diffCount = stockCheckGroupMapper.selectCount(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, checkId)
                .eq(StockCheckGroup::getStatus, 2)
        );
        stockCheck.setDifferenceCount(diffCount.intValue());

        stockCheckMapper.updateById(stockCheck);
    }

    private void fillRelations(StockCheck stockCheck) {
        if (stockCheck.getCreatorId() != null) {
            User user = userMapper.selectById(stockCheck.getCreatorId());
            if (user != null) {
                stockCheck.setCreatorName(user.getRealName());
            }
        }

        // 填充盘点人信息
        if (stockCheck.getCheckerId() != null) {
            User checker = userMapper.selectById(stockCheck.getCheckerId());
            if (checker != null) {
                stockCheck.setCheckerName(checker.getRealName());
            }
        }

        // 从分组表统计
        Long groupCount = stockCheckGroupMapper.selectCount(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, stockCheck.getId())
        );
        stockCheck.setGroupCount(groupCount.intValue());

        Long checkedGroupCount = stockCheckGroupMapper.selectCount(
            new LambdaQueryWrapper<StockCheckGroup>()
                .eq(StockCheckGroup::getCheckId, stockCheck.getId())
                .gt(StockCheckGroup::getStatus, 0)
        );
        stockCheck.setCheckedGroupCount(checkedGroupCount.intValue());
    }

    private void fillGroupRelations(StockCheckGroup group) {
        if (group.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(group.getMaterialId());
            if (material != null) {
                group.setMaterialCode(material.getCode());
                group.setMaterialName(material.getName());
            }
        }
        if (group.getCheckerId() != null) {
            User user = userMapper.selectById(group.getCheckerId());
            if (user != null) {
                group.setCheckerName(user.getRealName());
            }
        }
    }
}
