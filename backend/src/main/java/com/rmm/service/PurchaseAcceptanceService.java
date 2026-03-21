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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseAcceptanceService {

    private final PurchaseAcceptanceMapper acceptanceMapper;
    private final PurchaseMapper purchaseMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;
    private final StockInMapper stockInMapper;
    private final StockMapper stockMapper;
    private final LocationMapper locationMapper;

    public PageResult<PurchaseAcceptance> list(Integer current, Integer size, Integer status) {
        Page<PurchaseAcceptance> page = new Page<>(current, size);

        LambdaQueryWrapper<PurchaseAcceptance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, PurchaseAcceptance::getAcceptanceResult, status)
               .orderByDesc(PurchaseAcceptance::getCreateTime);

        Page<PurchaseAcceptance> result = acceptanceMapper.selectPage(page, wrapper);

        // 填充状态文本和位置名称
        result.getRecords().forEach(acceptance -> {
            fillStatusText(acceptance);
            fillLocationInfo(acceptance);
        });

        PageResult<PurchaseAcceptance> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public PurchaseAcceptance getById(Long id) {
        PurchaseAcceptance acceptance = acceptanceMapper.selectById(id);
        if (acceptance != null) {
            fillStatusText(acceptance);
            fillLocationInfo(acceptance);
        }
        return acceptance;
    }

    /**
     * 生成采购申请单号
     * 规则：CG + 年月日 + 4位流水号
     * 示例：CG202603220001
     */
    private String generatePurchaseNo() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询今日最大的单号
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Purchase::getPurchaseNo, "CG" + today)
               .orderByDesc(Purchase::getPurchaseNo)
               .last("LIMIT 1");

        Purchase lastPurchase = purchaseMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastPurchase != null && lastPurchase.getPurchaseNo() != null) {
            String lastNo = lastPurchase.getPurchaseNo();
            // 从 CG202603220001 提取 0001
            String lastSequence = lastNo.substring(12);
            sequence = Integer.parseInt(lastSequence) + 1;
        }

        return String.format("CG%s%04d", today, sequence);
    }

    /**
     * 确认到货，生成验收申请单
     */
    @Transactional
    public PurchaseAcceptance createAcceptance(Long purchaseId) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (purchase.getStatus() != 1) {
            throw new BusinessException("只能为已通过的采购申请创建验收单");
        }

        // 检查是否已有验收记录
        LambdaQueryWrapper<PurchaseAcceptance> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PurchaseAcceptance::getPurchaseId, purchaseId);
        if (acceptanceMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("该采购已生成验收单");
        }

        // 生成采购申请单号（如果还没有）
        if (purchase.getPurchaseNo() == null || purchase.getPurchaseNo().isEmpty()) {
            String purchaseNo = generatePurchaseNo();
            purchase.setPurchaseNo(purchaseNo);
            purchaseMapper.updateById(purchase);
        }

        // 创建验收记录
        PurchaseAcceptance acceptance = new PurchaseAcceptance();
        acceptance.setPurchaseId(purchaseId);
        acceptance.setPurchaseNo(purchase.getPurchaseNo());
        acceptance.setMaterialId(purchase.getMaterialId());
        acceptance.setSpecification(purchase.getSpecification());
        acceptance.setBatchNumber(purchase.getBatchNumber());
        acceptance.setQuantity(purchase.getQuantity());
        acceptance.setUnit(purchase.getUnit());
        acceptance.setSupplierId(purchase.getSupplierId());
        acceptance.setEstimatedPrice(purchase.getEstimatedPrice());
        acceptance.setTotalAmount(purchase.getTotalAmount());
        acceptance.setAcceptanceResult(PurchaseAcceptance.STATUS_PENDING);

        // 填充关联信息
        fillMaterialInfo(acceptance);
        fillSupplierInfo(acceptance);
        fillLocationInfo(acceptance);

        acceptanceMapper.insert(acceptance);

        // 更新采购单状态为待验收
        purchase.setStatus(4);  // 状态改为待验收
        purchaseMapper.updateById(purchase);

        return acceptance;
    }

    /**
     * 开始验收
     */
    @Transactional
    public void startAcceptance(Long acceptanceId, Long userId) {
        PurchaseAcceptance acceptance = acceptanceMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new BusinessException("验收单不存在");
        }
        if (acceptance.getAcceptanceResult() != PurchaseAcceptance.STATUS_PENDING) {
            throw new BusinessException("只能开始待验收的订单");
        }

        // 设置验收人
        User user = userMapper.selectById(userId);
        if (user != null) {
            acceptance.setAcceptanceUserId(userId);
            acceptance.setAcceptanceUserName(user.getRealName());
        }

        acceptance.setAcceptanceDate(LocalDateTime.now());
        acceptanceMapper.updateById(acceptance);
    }

    /**
     * 提交验收
     */
    @Transactional
    public void submitAcceptance(Long acceptanceId, Long userId, Integer packageIntact,
                                Integer labelComplete, Integer hasDamage, java.math.BigDecimal actualQuantity,
                                java.time.LocalDate expiryDate, Long locationId,
                                Integer result, String remark) {
        PurchaseAcceptance acceptance = acceptanceMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new BusinessException("验收单不存在");
        }
        if (acceptance.getAcceptanceResult() != PurchaseAcceptance.STATUS_PENDING) {
            throw new BusinessException("该验收单已处理");
        }

        // 更新验收信息
        acceptance.setPackageIntact(packageIntact);
        acceptance.setLabelComplete(labelComplete);
        acceptance.setHasDamage(hasDamage);
        acceptance.setActualQuantity(actualQuantity);
        acceptance.setExpiryDate(expiryDate);
        acceptance.setLocationId(locationId);
        acceptance.setAcceptanceResult(result);
        acceptance.setAcceptanceRemark(remark);
        acceptance.setAcceptanceDate(LocalDateTime.now());

        // 设置验收人
        User user = userMapper.selectById(userId);
        if (user != null) {
            acceptance.setAcceptanceUserId(userId);
            acceptance.setAcceptanceUserName(user.getRealName());
        }

        acceptanceMapper.updateById(acceptance);

        // 更新采购单状态
        Purchase purchase = purchaseMapper.selectById(acceptance.getPurchaseId());
        if (purchase != null) {
            if (result == PurchaseAcceptance.STATUS_PASSED) {
                purchase.setStatus(6);  // 验收通过

                // 自动生成入库申请
                StockIn stockIn = createStockIn(acceptance, purchase);
                acceptance.setStockInId(stockIn.getId());
                acceptanceMapper.updateById(acceptance);

            } else {
                purchase.setStatus(7);  // 验收拒绝
            }
            purchaseMapper.updateById(purchase);
        }
    }

    /**
     * 创建入库申请
     */
    private StockIn createStockIn(PurchaseAcceptance acceptance, Purchase purchase) {
        StockIn stockIn = new StockIn();
        stockIn.setMaterialId(purchase.getMaterialId());
        stockIn.setBatchNo(purchase.getBatchNumber());

        // 使用实际到货数量，如果没有填写则使用采购数量
        java.math.BigDecimal quantity = acceptance.getActualQuantity() != null
            ? acceptance.getActualQuantity()
            : purchase.getQuantity();
        stockIn.setQuantity(quantity);

        // 设置有效期和存放位置
        stockIn.setExpiryDate(acceptance.getExpiryDate());
        stockIn.setLocationId(acceptance.getLocationId());

        stockIn.setReason("采购单号：" + purchase.getPurchaseNo() + "，验收通过");
        stockIn.setOperatorId(acceptance.getAcceptanceUserId());
        stockIn.setOperatorName(acceptance.getAcceptanceUserName());
        stockIn.setSupplierId(purchase.getSupplierId());

        // 生成内部编号
        String batchNo = purchase.getBatchNumber();
        int maxSequence = getMaxSequenceForBatch(batchNo);
        int qty = quantity.intValue();

        // 生成内部编号范围
        String firstCode = generateInternalCode(batchNo, maxSequence + 1);
        String lastCode = generateInternalCode(batchNo, maxSequence + qty);
        String internalCodeRange = qty == 1 ? firstCode : firstCode + " ~ " + lastCode;

        stockIn.setInternalCode(internalCodeRange);

        // 插入入库记录
        stockInMapper.insert(stockIn);

        // 为每个物品创建独立的库存记录
        for (int i = 1; i <= qty; i++) {
            int sequence = maxSequence + i;
            String internalCode = generateInternalCode(batchNo, sequence);

            Stock stock = new Stock();
            stock.setMaterialId(purchase.getMaterialId());
            stock.setBatchNo(batchNo);
            stock.setInternalCode(internalCode);
            stock.setExpiryDate(acceptance.getExpiryDate());
            stock.setLocationId(acceptance.getLocationId());
            stock.setQuantity(java.math.BigDecimal.ONE);
            stock.setStatus(1);
            stockMapper.insert(stock);
        }

        return stockIn;
    }

    /**
     * 获取指定批号的最大序列号（从stock表）
     */
    private int getMaxSequenceForBatch(String batchNo) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Stock> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.likeRight(Stock::getInternalCode, batchNo + "-")
               .orderByDesc(Stock::getInternalCode)
               .last("LIMIT 1");

        Stock lastStock = stockMapper.selectOne(wrapper);
        if (lastStock == null || lastStock.getInternalCode() == null) {
            return 0;
        }

        // 解析序列号，格式: batchNo-NNN
        String internalCode = lastStock.getInternalCode();
        int lastDashIndex = internalCode.lastIndexOf("-");
        if (lastDashIndex == -1) {
            return 0;
        }

        try {
            String sequenceStr = internalCode.substring(lastDashIndex + 1);
            return Integer.parseInt(sequenceStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 生成内部编号
     * 格式: 批号-NNN (3位序列号)
     */
    private String generateInternalCode(String batchNo, int sequence) {
        return String.format("%s-%03d", batchNo.toUpperCase(), sequence);
    }

    private void fillMaterialInfo(PurchaseAcceptance acceptance) {
        if (acceptance.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(acceptance.getMaterialId());
            if (material != null) {
                acceptance.setMaterialName(material.getName());
                acceptance.setMaterialCode(material.getCode());
            }
        }
    }

    private void fillSupplierInfo(PurchaseAcceptance acceptance) {
        if (acceptance.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(acceptance.getSupplierId());
            if (supplier != null) {
                acceptance.setSupplierName(supplier.getName());
            }
        }
    }

    private void fillLocationInfo(PurchaseAcceptance acceptance) {
        if (acceptance.getLocationId() != null) {
            Location location = locationMapper.selectById(acceptance.getLocationId());
            if (location != null) {
                acceptance.setLocationName(location.getName());
            }
        }
    }

    private void fillStatusText(PurchaseAcceptance acceptance) {
        Integer result = acceptance.getAcceptanceResult();
        if (result == null) {
            acceptance.setAcceptanceResultText("待验收");
        } else if (result == PurchaseAcceptance.STATUS_PENDING) {
            acceptance.setAcceptanceResultText("待验收");
        } else if (result == PurchaseAcceptance.STATUS_PASSED) {
            acceptance.setAcceptanceResultText("验收通过");
        } else if (result == PurchaseAcceptance.STATUS_REJECTED) {
            acceptance.setAcceptanceResultText("验收拒绝");
        } else {
            acceptance.setAcceptanceResultText("未知");
        }
    }
}
