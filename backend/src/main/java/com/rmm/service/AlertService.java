package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        records.forEach(this::fillRelations);
        return records;
    }

    public AlertStats getStats() {
        AlertStats stats = new AlertStats();
        stats.setTotal(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, 0)
        ).intValue());
        stats.setExpiry(alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, 0)
                .likeRight(AlertRecord::getType, "EXPIRY")
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
        LocalDate criticalDate = criticalConfig != null ? today.plusDays(criticalConfig.getThreshold()) : today.plusDays(7);

        List<Stock> stocks = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .isNotNull(Stock::getExpiryDate)
                .gt(Stock::getExpiryDate, today)
                .le(Stock::getExpiryDate, warningDate)
        );

        for (Stock stock : stocks) {
            long days = ChronoUnit.DAYS.between(today, stock.getExpiryDate());
            String alertType = days <= (criticalConfig != null ? criticalConfig.getThreshold() : 7) ? "EXPIRY_CRITICAL" : "EXPIRY_WARNING";
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
            createAlertIfNotExists("UNUSED", stock, stock.getMaterialId(),
                String.format("【%s】已超过%d个月未使用", getMaterialName(stock.getMaterialId()), config.getThreshold()), 1);
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
                // 兼容处理：查询该物质所有在库的内部编码
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
        } else if (record.getStockId() != null) {
            Stock stock = stockMapper.selectById(record.getStockId());
            if (stock != null) {
                record.setInternalCode(stock.getInternalCode());
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
        private int expiry;
        private int stockLow;
        private int unused;
    }
}
