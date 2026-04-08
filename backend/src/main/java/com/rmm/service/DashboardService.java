package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final PurchaseMapper purchaseMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 物质统计
        stats.put("totalMaterials", materialMapper.selectCount(
            new LambdaQueryWrapper<ReferenceMaterial>().eq(ReferenceMaterial::getStatus, 1)
        ));

        // 库存统计
        stats.put("totalStock", stockMapper.selectCount(
            new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1)
        ));

        // 库存总量
        BigDecimal totalQuantity = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1)
        ).stream()
         .map(Stock::getQuantity)
         .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalQuantity", totalQuantity);

        // 入库统计
        stats.put("totalStockIn", stockInMapper.selectCount(null));

        // 出库统计
        stats.put("totalStockOut", stockOutMapper.selectCount(
            new LambdaQueryWrapper<StockOut>().eq(StockOut::getStatus, 1)
        ));

        // 采购待审批
        stats.put("pendingPurchase", purchaseMapper.selectCount(
            new LambdaQueryWrapper<Purchase>().eq(Purchase::getStatus, 0)
        ));

        // 预警数量
        stats.put("alertCount", alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, 0)
        ));

        return stats;
    }

    public Map<String, Object> getExpiryStats() {
        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(30);

        // 即将过期(30天内)
        result.put("expiringSoon", stockMapper.selectCount(
            new LambdaQueryWrapper<Stock>()
                .eq(Stock::getStatus, 1)
                .isNotNull(Stock::getExpiryDate)
                .gt(Stock::getExpiryDate, today)
                .le(Stock::getExpiryDate, warningDate)
        ));

        // 已过期
        result.put("expired", stockMapper.selectCount(
            new LambdaQueryWrapper<Stock>()
                .eq(Stock::getStatus, 1)
                .isNotNull(Stock::getExpiryDate)
                .lt(Stock::getExpiryDate, today)
        ));

        return result;
    }

    public Map<String, Object> getTodoItems(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 判断是否为管理员/经理
        User user = userMapper.selectById(userId);
        Role role = user != null && user.getRoleId() != null ? roleMapper.selectById(user.getRoleId()) : null;
        String roleCode = role != null ? role.getCode() : "";
        boolean isAdmin = "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode);

        // 待审批采购申请数量：管理员看全部，普通用户只看自己申请的
        LambdaQueryWrapper<Purchase> pendingWrapper = new LambdaQueryWrapper<Purchase>()
            .eq(Purchase::getStatus, 0);
        if (!isAdmin) {
            pendingWrapper.eq(Purchase::getApplicantId, userId);
        }
        long pendingPurchaseCount = purchaseMapper.selectCount(pendingWrapper);
        result.put("pendingPurchaseCount", (int) pendingPurchaseCount);

        // 已通过待确认数量：管理员看全部，普通用户只看自己申请的
        LambdaQueryWrapper<Purchase> approvedWrapper = new LambdaQueryWrapper<Purchase>()
            .eq(Purchase::getStatus, 1);
        if (!isAdmin) {
            approvedWrapper.eq(Purchase::getApplicantId, userId);
        }
        long approvedPurchaseCount = purchaseMapper.selectCount(approvedWrapper);
        result.put("approvedPurchaseCount", (int) approvedPurchaseCount);

        // 待处理预警数量（status=0）
        long alertCount = alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getStatus, 0)
        );
        result.put("alertCount", (int) alertCount);

        return result;
    }
}
