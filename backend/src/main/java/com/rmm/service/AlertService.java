package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertConfigMapper alertConfigMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final UserMapper userMapper;

    public AlertConfig getConfig(String type) {
        return alertConfigMapper.selectOne(
            new LambdaQueryWrapper<AlertConfig>().eq(AlertConfig::getType, type)
        );
    }

    public List<AlertConfig> getAllConfigs() {
        return alertConfigMapper.selectList(null);
    }

    public void updateConfig(String type, Integer threshold, Integer enabled) {
        AlertConfig config = getConfig(type);
        if (config != null) {
            config.setThreshold(threshold);
            config.setEnabled(enabled);
            alertConfigMapper.updateById(config);
        }
    }

    public List<AlertRecord> getAlerts(Integer status, String type) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, AlertRecord::getStatus, status)
               .eq(type != null && !type.isEmpty(), AlertRecord::getType, type)
               .orderByDesc(AlertRecord::getCreateTime);

        List<AlertRecord> records = alertRecordMapper.selectList(wrapper);
        fillRelationsBatch(records);
        return records;
    }

    public PageResult<AlertRecord> getAlertsPage(Integer current, Integer size, Integer status, String type) {
        Page<AlertRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, AlertRecord::getStatus, status)
               .eq(type != null && !type.isEmpty(), AlertRecord::getType, type)
               .orderByDesc(AlertRecord::getCreateTime);

        Page<AlertRecord> result = alertRecordMapper.selectPage(page, wrapper);
        fillRelationsBatch(result.getRecords());

        PageResult<AlertRecord> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    private void fillRelationsBatch(List<AlertRecord> records) {
        if (records.isEmpty()) return;

        // 批量加载关联数据
        List<Long> materialIds = records.stream().map(AlertRecord::getMaterialId).filter(Objects::nonNull).distinct().toList();
        List<Long> stockIds = records.stream().map(AlertRecord::getStockId).filter(Objects::nonNull).distinct().toList();
        List<Long> handlerIds = records.stream().map(AlertRecord::getHandlerId).filter(Objects::nonNull).distinct().toList();

        Map<Long, ReferenceMaterial> materialMap = materialIds.isEmpty() ? Map.of() :
            materialMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(ReferenceMaterial::getId, m -> m));
        Map<Long, Stock> stockMap = stockIds.isEmpty() ? Map.of() :
            stockMapper.selectBatchIds(stockIds).stream().collect(Collectors.toMap(Stock::getId, s -> s));
        Map<Long, User> handlerMap = handlerIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(handlerIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 收集需要查询位置的 locationId
        List<Long> locationIds = stockMap.values().stream()
            .map(Stock::getLocationId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Location> locationMap = locationIds.isEmpty() ? Map.of() :
            locationMapper.selectBatchIds(locationIds).stream().collect(Collectors.toMap(Location::getId, l -> l));

        for (AlertRecord record : records) {
            // 填充物质名称
            if (record.getMaterialId() != null) {
                ReferenceMaterial material = materialMap.get(record.getMaterialId());
                if (material != null) {
                    record.setMaterialName(material.getName());
                }
            }

            // 库存不足预警
            if ("STOCK_LOW".equals(record.getType())) {
                if (record.getInternalCodes() == null || record.getInternalCodes().isEmpty()) {
                    List<Stock> stocks = stockMapper.selectList(
                        new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getMaterialId, record.getMaterialId())
                            .gt(Stock::getQuantity, BigDecimal.ZERO)
                            .eq(Stock::getStatus, 1)
                    );
                    String codes = stocks.stream()
                        .map(Stock::getInternalCode)
                        .filter(Objects::nonNull)
                        .filter(code -> !code.isEmpty())
                        .collect(Collectors.joining(", "));
                    record.setInternalCodes(codes);
                }
                // 查询库存不足预警涉及的所有位置
                List<Stock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getMaterialId, record.getMaterialId())
                        .gt(Stock::getQuantity, BigDecimal.ZERO)
                        .eq(Stock::getStatus, 1)
                );
                String locations = stocks.stream()
                    .map(Stock::getLocationId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(locationId -> {
                        Location location = locationMap.get(locationId);
                        return location != null ? location.getName() : "";
                    })
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.joining(", "));
                record.setLocationName(locations);
            } else if (record.getStockId() != null) {
                Stock stock = stockMap.get(record.getStockId());
                if (stock != null) {
                    record.setInternalCode(stock.getInternalCode());
                    if (stock.getLocationId() != null) {
                        Location location = locationMap.get(stock.getLocationId());
                        if (location != null) {
                            record.setLocationName(location.getName());
                        }
                    }
                }
            }

            // 填充处理人
            if (record.getHandlerId() != null) {
                User user = handlerMap.get(record.getHandlerId());
                if (user != null) {
                    record.setHandlerName(user.getRealName());
                }
            }
        }
    }

    public AlertStats getStats() {
        AlertStats stats = new AlertStats();
        stats.setTotal(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, 0)
        ).intValue());
        stats.setExpiryWarning(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .eq(AlertRecord::getType, "EXPIRY_WARNING")
        ).intValue());
        stats.setExpiryCritical(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .eq(AlertRecord::getType, "EXPIRY_CRITICAL")
        ).intValue());
        stats.setExpiryOverdue(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .eq(AlertRecord::getType, "EXPIRY_OVERDUE")
        ).intValue());
        stats.setStockLow(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .eq(AlertRecord::getType, "STOCK_LOW")
        ).intValue());
        stats.setUnused(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .eq(AlertRecord::getType, "UNUSED")
        ).intValue());
        return stats;
    }

    @Transactional
    public void handleAlert(Long id, Long handlerId, String remark) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) return;
        record.setStatus(1);
        record.setHandlerId(handlerId);
        record.setHandleTime(LocalDateTime.now());
        record.setHandleRemark(remark);
        alertRecordMapper.updateById(record);
    }

    @Transactional
    public void ignoreAlert(Long id) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) return;
        record.setStatus(2);
        alertRecordMapper.updateById(record);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkAlerts() {
        log.info("开始检查预警...");
        checkExpiryAlerts();
        checkStockLowAlerts();
        checkUnusedAlerts();
        log.info("预警检查完成");
    }

    private void checkExpiryAlerts() {
        AlertConfig warningConfig = getConfig("EXPIRY_WARNING");
        AlertConfig criticalConfig = getConfig("EXPIRY_CRITICAL");

        if (warningConfig == null || warningConfig.getEnabled() != 1) return;

        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(warningConfig.getThreshold());
        int criticalThreshold = criticalConfig != null ? criticalConfig.getThreshold() : 7;

        // 查询已过期的库存
        List<Stock> expiredStocks = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .isNotNull(Stock::getExpiryDate)
                .le(Stock::getExpiryDate, today)
        );

        for (Stock stock : expiredStocks) {
            long days = ChronoUnit.DAYS.between(stock.getExpiryDate(), today);
            createAlertIfNotExists("EXPIRY_OVERDUE", stock, stock.getMaterialId(),
                String.format("【%s】已过期%d天", getMaterialName(stock.getMaterialId()), days), 3);
        }

        // 查询即将过期的库存（未过期但在预警期内）
        List<Stock> warningStocks = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .isNotNull(Stock::getExpiryDate)
                .gt(Stock::getExpiryDate, today)
                .le(Stock::getExpiryDate, warningDate)
        );

        for (Stock stock : warningStocks) {
            long days = ChronoUnit.DAYS.between(today, stock.getExpiryDate());
            String alertType = days <= criticalThreshold ? "EXPIRY_CRITICAL" : "EXPIRY_WARNING";
            int level = alertType.equals("EXPIRY_CRITICAL") ? 3 : 2;

            createAlertIfNotExists(alertType, stock, stock.getMaterialId(),
                String.format("【%s】将在%d天后过期", getMaterialName(stock.getMaterialId()), days), level);
        }
    }

    private void checkStockLowAlerts() {
        AlertConfig config = getConfig("STOCK_LOW");
        if (config == null || config.getEnabled() != 1) return;

        // 按标准物质汇总在库数量
        List<Map<String, Object>> lowStockMaterials = stockMapper.selectMaps(
            new QueryWrapper<Stock>()
                .select("material_id", "COUNT(*) as total_count")
                .gt("quantity", BigDecimal.ZERO)
                .eq("status", 1)  // 在库状态
                .groupBy("material_id")
                .having("COUNT(*) <= {0}", config.getThreshold())
        );

        for (Map<String, Object> item : lowStockMaterials) {
            Long materialId = ((Number) item.get("material_id")).longValue();
            Long totalCount = ((Number) item.get("total_count")).longValue();

            // 查询该物质所有在库的内部编码
            List<Stock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getMaterialId, materialId)
                    .gt(Stock::getQuantity, BigDecimal.ZERO)
                    .eq(Stock::getStatus, 1)
            );
            // 过滤掉 null 和空字符串的内部编码
            String internalCodes = stocks.stream()
                .map(Stock::getInternalCode)
                .filter(Objects::nonNull)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.joining(", "));

            createStockLowAlertIfNotExists(materialId, totalCount, config.getThreshold(), internalCodes);
        }
    }

    private void createStockLowAlertIfNotExists(Long materialId, Long totalCount,
                                                 Integer threshold, String internalCodes) {
        Long existing = alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getType, "STOCK_LOW")
                .eq(AlertRecord::getMaterialId, materialId)
                .eq(AlertRecord::getStatus, 0)
        );
        if (existing > 0) return;

        AlertRecord record = new AlertRecord();
        record.setType("STOCK_LOW");
        record.setStockId(null);
        record.setMaterialId(materialId);
        record.setInternalCodes(internalCodes);
        record.setContent(String.format("【%s】库存不足，当前库存: %d 件（阈值: %d 件）",
            getMaterialName(materialId), totalCount, threshold));
        record.setLevel(2);
        record.setStatus(0);
        alertRecordMapper.insert(record);
    }

    private void checkUnusedAlerts() {
        AlertConfig config = getConfig("UNUSED_MONTHS");
        if (config == null || config.getEnabled() != 1) return;

        LocalDateTime cutoffTime = LocalDateTime.now().minusMonths(config.getThreshold());

        List<Stock> stocks = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .gt(Stock::getQuantity, BigDecimal.ZERO)
                .and(w -> w.lt(Stock::getLastOutTime, cutoffTime)
                           .or()
                           .isNull(Stock::getLastOutTime))
        );

        for (Stock stock : stocks) {
            String internalCode = stock.getInternalCode() != null ? stock.getInternalCode() : "未知编号";
            createAlertIfNotExists("UNUSED", stock, stock.getMaterialId(),
                String.format("【%s-%s】已超过%d个月未使用", getMaterialName(stock.getMaterialId()), internalCode, config.getThreshold()), 1);
        }
    }

    private void createAlertIfNotExists(String type, Stock stock, Long materialId, String content, int level) {
        Long existing = alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getType, type)
                .eq(AlertRecord::getStockId, stock.getId())
                .eq(AlertRecord::getStatus, 0)
        );
        if (existing > 0) return;

        AlertRecord record = new AlertRecord();
        record.setType(type);
        record.setStockId(stock.getId());
        record.setMaterialId(materialId);
        record.setContent(content);
        record.setLevel(level);
        record.setStatus(0);
        alertRecordMapper.insert(record);
    }

    private String getMaterialName(Long materialId) {
        if (materialId == null) return "未知";
        ReferenceMaterial material = materialMapper.selectById(materialId);
        return material != null ? material.getName() : "未知";
    }

    private void fillRelations(AlertRecord record) {
        if (record.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(record.getMaterialId());
            if (material != null) {
                record.setMaterialName(material.getName());
            }
        }

        // 对于库存预警，使用数据库中存储的 internalCodes
        // 对于其他预警类型，从 stock 表查询单个 internalCode
        if ("STOCK_LOW".equals(record.getType())) {
            // internalCodes 已从数据库加载，无需额外处理
            // 如果需要兼容旧数据（internalCodes 为空），可按 materialId 查询
            if (record.getInternalCodes() == null || record.getInternalCodes().isEmpty()) {
                // 兼容处理：查询该物质所有在库的内部编号
                List<Stock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getMaterialId, record.getMaterialId())
                        .gt(Stock::getQuantity, BigDecimal.ZERO)
                        .eq(Stock::getStatus, 1)
                );
                String codes = stocks.stream()
                    .map(Stock::getInternalCode)
                    .filter(Objects::nonNull)
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.joining(", "));
                record.setInternalCodes(codes);
            }

            // 查询库存不足预警涉及的所有位置
            List<Stock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getMaterialId, record.getMaterialId())
                    .gt(Stock::getQuantity, BigDecimal.ZERO)
                    .eq(Stock::getStatus, 1)
            );
            String locations = stocks.stream()
                .map(Stock::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .map(locationId -> {
                    Location location = locationMapper.selectById(locationId);
                    return location != null ? location.getName() : "";
                })
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining(", "));
            record.setLocationName(locations);

        } else if (record.getStockId() != null) {
            Stock stock = stockMapper.selectById(record.getStockId());
            if (stock != null) {
                record.setInternalCode(stock.getInternalCode());

                // 查询单个库存的位置信息
                if (stock.getLocationId() != null) {
                    Location location = locationMapper.selectById(stock.getLocationId());
                    if (location != null) {
                        record.setLocationName(location.getName());
                    }
                }
            }
        }

        if (record.getHandlerId() != null) {
            User user = userMapper.selectById(record.getHandlerId());
            if (user != null) {
                record.setHandlerName(user.getRealName());
            }
        }
    }

    @lombok.Data
    public static class AlertStats {
        private int total;
        private int expiryWarning;
        private int expiryCritical;
        private int expiryOverdue;
        private int stockLow;
        private int unused;
    }
}
