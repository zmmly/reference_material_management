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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockOutService {

    private final StockOutMapper stockOutMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final UserMapper userMapper;
    private final SupplierMapper supplierMapper;

    public PageResult<StockOut> list(Integer current, Integer size, Integer status, Long applicantId) {
        Page<StockOut> page = new Page<>(current, size);

        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, StockOut::getStatus, status)
               .eq(applicantId != null, StockOut::getApplicantId, applicantId)
               .orderByDesc(StockOut::getApplyTime);

        Page<StockOut> result = stockOutMapper.selectPage(page, wrapper);

        result.getRecords().forEach(this::fillRelations);

        PageResult<StockOut> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Transactional
    public void apply(StockOut stockOut, Long applicantId) {
        Stock stock = stockMapper.selectById(stockOut.getStockId());
        if (stock == null) {
            throw new BusinessException("库存不存在");
        }
        // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)
        if (stock.getStatus() == 0) {
            throw new BusinessException("该库存已出库");
        }

        // 检查是否已有待审批的出库申请
        Long pendingCount = stockOutMapper.selectCount(
            new LambdaQueryWrapper<StockOut>()
                .eq(StockOut::getStockId, stockOut.getStockId())
                .eq(StockOut::getStatus, 0)
        );
        if (pendingCount > 0) {
            throw new BusinessException("该库存已有待审批的出库申请，请先撤回之前的申请");
        }

        // 新设计：每条库存记录代表一个物品，数量固定为1
        stockOut.setMaterialId(stock.getMaterialId());
        stockOut.setQuantity(BigDecimal.ONE);  // 固定为1
        stockOut.setInternalCode(stock.getInternalCode());  // 记录内部编码
        stockOut.setBatchNo(stock.getBatchNo());  // 记录批号
        stockOut.setApplicantId(applicantId);
        stockOut.setStatus(0);
        stockOut.setApplyTime(LocalDateTime.now());
        stockOutMapper.insert(stockOut);
    }

    /**
     * 批量出库申请
     */
    @Transactional
    public void batchApply(List<Long> stockIds, String reason, String purpose, Long applicantId) {
        if (stockIds == null || stockIds.isEmpty()) {
            throw new BusinessException("请选择要出库的库存");
        }

        for (Long stockId : stockIds) {
            Stock stock = stockMapper.selectById(stockId);
            if (stock == null) {
                throw new BusinessException("库存不存在: " + stockId);
            }
            // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)
            if (stock.getStatus() == 0) {
                throw new BusinessException("库存已出库: " + stock.getInternalCode());
            }

            // 检查是否已有待审批的出库申请
            Long pendingCount = stockOutMapper.selectCount(
                new LambdaQueryWrapper<StockOut>()
                    .eq(StockOut::getStockId, stockId)
                    .eq(StockOut::getStatus, 0)
            );
            if (pendingCount > 0) {
                throw new BusinessException("库存 " + stock.getInternalCode() + " 已有待审批的出库申请，请先撤回");
            }

            StockOut stockOut = new StockOut();
            stockOut.setStockId(stockId);
            stockOut.setMaterialId(stock.getMaterialId());
            stockOut.setQuantity(BigDecimal.ONE);
            stockOut.setInternalCode(stock.getInternalCode());
            stockOut.setBatchNo(stock.getBatchNo());
            stockOut.setReason(reason);
            stockOut.setPurpose(purpose);
            stockOut.setApplicantId(applicantId);
            stockOut.setStatus(0);
            stockOut.setApplyTime(LocalDateTime.now());
            stockOutMapper.insert(stockOut);
        }
    }

    @Transactional
    public void approve(Long id, Long approverId, boolean approved, String rejectReason) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw new BusinessException("出库申请不存在");
        }
        if (stockOut.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        if (approved) {
            Stock stock = stockMapper.selectById(stockOut.getStockId());
            if (stock == null) {
                throw new BusinessException("库存不存在");
            }
            // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)
            if (stock.getStatus() == 0) {
                throw new BusinessException("该库存已出库");
            }

            // 标记库存为已出库
            stock.setStatus(0);
            stock.setLastOutTime(LocalDateTime.now());
            stockMapper.updateById(stock);

            stockOut.setStatus(1);
        } else {
            if (!StringUtils.hasText(rejectReason)) {
                throw new BusinessException("请填写拒绝原因");
            }
            stockOut.setStatus(2);
            stockOut.setRejectReason(rejectReason);
        }

        stockOut.setApproverId(approverId);
        stockOut.setApproveTime(LocalDateTime.now());
        stockOutMapper.updateById(stockOut);
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw new BusinessException("出库申请不存在");
        }
        if (stockOut.getStatus() != 0) {
            throw new BusinessException("只能撤回待审批的申请");
        }
        if (!stockOut.getApplicantId().equals(userId)) {
            throw new BusinessException("只能撤回自己的申请");
        }

        stockOut.setStatus(3);
        stockOutMapper.updateById(stockOut);
    }

    private void fillRelations(StockOut stockOut) {
        if (stockOut.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stockOut.getMaterialId());
            if (material != null) {
                stockOut.setMaterialName(material.getName());
                stockOut.setCasNumber(material.getCasNumber());

                // 填充供应商名称
                if (material.getSupplierId() != null) {
                    Supplier supplier = supplierMapper.selectById(material.getSupplierId());
                    if (supplier != null) {
                        stockOut.setSupplierName(supplier.getName());
                    }
                }
            }
        }
        if (stockOut.getApplicantId() != null) {
            User user = userMapper.selectById(stockOut.getApplicantId());
            if (user != null) {
                stockOut.setApplicantName(user.getRealName());
            }
        }
        if (stockOut.getApproverId() != null) {
            User user = userMapper.selectById(stockOut.getApproverId());
            if (user != null) {
                stockOut.setApproverName(user.getRealName());
            }
        }
    }
}
