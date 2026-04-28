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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockOutService {

    private final StockOutMapper stockOutMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final UserMapper userMapper;
    private final SupplierMapper supplierMapper;
    private final StockInMapper stockInMapper;

    public PageResult<StockOut> list(Integer current, Integer size, Integer status, Long applicantId,
                                      String applicantName, String materialCode, String materialName) {
        Page<StockOut> page = new Page<>(current, size);

        // 按申请人姓名模糊查询，获取匹配的用户ID列表
        List<Long> applicantIds = null;
        if (StringUtils.hasText(applicantName)) {
            List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().like(User::getRealName, applicantName));
            applicantIds = users.stream().map(User::getId).collect(Collectors.toList());
            if (applicantIds.isEmpty()) {
                // 没有匹配的用户，直接返回空结果
                PageResult<StockOut> pageResult = new PageResult<>();
                pageResult.setRecords(Collections.emptyList());
                pageResult.setTotal(0L);
                pageResult.setSize((long) size);
                pageResult.setCurrent((long) current);
                pageResult.setPages(0L);
                return pageResult;
            }
        }

        // 按物质编号/名称模糊查询，获取匹配的物质ID列表
        List<Long> materialIds = null;
        if (StringUtils.hasText(materialCode) || StringUtils.hasText(materialName)) {
            LambdaQueryWrapper<ReferenceMaterial> matWrapper = new LambdaQueryWrapper<>();
            matWrapper.like(StringUtils.hasText(materialCode), ReferenceMaterial::getCode, materialCode)
                      .like(StringUtils.hasText(materialName), ReferenceMaterial::getName, materialName);
            List<ReferenceMaterial> materials = materialMapper.selectList(matWrapper);
            materialIds = materials.stream().map(ReferenceMaterial::getId).collect(Collectors.toList());
            if (materialIds.isEmpty()) {
                PageResult<StockOut> pageResult = new PageResult<>();
                pageResult.setRecords(Collections.emptyList());
                pageResult.setTotal(0L);
                pageResult.setSize((long) size);
                pageResult.setCurrent((long) current);
                pageResult.setPages(0L);
                return pageResult;
            }
        }

        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, StockOut::getStatus, status)
               .eq(applicantId != null, StockOut::getApplicantId, applicantId)
               .in(applicantIds != null, StockOut::getApplicantId, applicantIds)
               .in(materialIds != null, StockOut::getMaterialId, materialIds)
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
        // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)和借出(4)
        if (stock.getStatus() == 0 || stock.getStatus() == 4) {
            throw new BusinessException(stock.getStatus() == 4 ? "该库存已借出，请先归还" : "该库存已出库");
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
        if (stockOut.getNeedReturn() == null) {
            stockOut.setNeedReturn(false);
        }
        stockOutMapper.insert(stockOut);
    }

    /**
     * 批量出库申请
     */
    @Transactional
    public void batchApply(List<Long> stockIds, String reason, String purpose, Boolean needReturn, Long applicantId) {
        if (stockIds == null || stockIds.isEmpty()) {
            throw new BusinessException("请选择要出库的库存");
        }

        for (Long stockId : stockIds) {
            Stock stock = stockMapper.selectById(stockId);
            if (stock == null) {
                throw new BusinessException("库存不存在: " + stockId);
            }
            // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)和借出(4)
            if (stock.getStatus() == 0 || stock.getStatus() == 4) {
                throw new BusinessException("库存已出库或借出: " + stock.getInternalCode());
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
            stockOut.setNeedReturn(needReturn != null && needReturn);
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
            // 允许正常(1)、即将过期(2)、已过期(3)的库存出库，禁止已出库(0)和借出(4)
            if (stock.getStatus() == 0 || stock.getStatus() == 4) {
                throw new BusinessException("该库存已出库或借出");
            }

            // 标记库存为已出库或借出
            if (Boolean.TRUE.equals(stockOut.getNeedReturn())) {
                stock.setStatus(4);  // 借出/待归还
            } else {
                stock.setStatus(0);  // 已出库
            }
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

    /**
     * 更新出库申请（仅允许更新待审批状态的申请）
     */
    public void update(Long id, StockOut stockOut, Long userId) {
        StockOut existing = stockOutMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("出库申请不存在");
        }
        if (existing.getStatus() != 0) {
            throw new BusinessException("只能编辑待审批的申请");
        }
        if (!existing.getApplicantId().equals(userId)) {
            throw new BusinessException("只能编辑自己的申请");
        }

        // 只允许更新出库原因和用途说明
        existing.setReason(stockOut.getReason());
        existing.setPurpose(stockOut.getPurpose());
        stockOutMapper.updateById(existing);
    }

    /**
     * 删除出库申请（仅允许删除待审批状态的申请）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw new BusinessException("出库申请不存在");
        }
        if (stockOut.getStatus() != 0) {
            throw new BusinessException("只能删除待审批的申请");
        }
        if (!stockOut.getApplicantId().equals(userId)) {
            throw new BusinessException("只能删除自己的申请");
        }

        stockOutMapper.deleteById(id);
    }

    private void fillRelations(StockOut stockOut) {
        if (stockOut.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stockOut.getMaterialId());
            if (material != null) {
                stockOut.setMaterialCode(material.getCode());
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
        // 设置归还状态文本
        if (Boolean.TRUE.equals(stockOut.getNeedReturn())) {
            stockOut.setReturnStatusText(Boolean.TRUE.equals(stockOut.getReturned()) ? "已归还" : "待归还");
        } else {
            stockOut.setReturnStatusText("无需归还");
        }
    }

    /**
     * 获取待归还列表
     */
    public PageResult<StockOut> pendingReturns(Integer current, Integer size) {
        Page<StockOut> page = new Page<>(current, size);
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOut::getStatus, 1)
               .eq(StockOut::getNeedReturn, true)
               .eq(StockOut::getReturned, false)
               .orderByDesc(StockOut::getApproveTime);

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

    /**
     * 归还出库物品
     */
    @Transactional
    public void returnStock(Long stockOutId, Long operatorId) {
        StockOut stockOut = stockOutMapper.selectById(stockOutId);
        if (stockOut == null) {
            throw new BusinessException("出库记录不存在");
        }
        if (stockOut.getStatus() != 1) {
            throw new BusinessException("该出库申请未审批通过");
        }
        if (!Boolean.TRUE.equals(stockOut.getNeedReturn())) {
            throw new BusinessException("该出库申请不需要归还");
        }
        if (Boolean.TRUE.equals(stockOut.getReturned())) {
            throw new BusinessException("该物品已归还");
        }

        // 恢复库存记录
        Stock stock = stockMapper.selectById(stockOut.getStockId());
        if (stock == null) {
            throw new BusinessException("库存记录不存在");
        }
        stock.setStatus(1);
        stockMapper.updateById(stock);

        // 标记出库记录为已归还
        stockOut.setReturned(true);
        stockOutMapper.updateById(stockOut);

        // 创建入库记录（审计追踪）
        StockIn stockIn = new StockIn();
        stockIn.setStockOutId(stockOutId);
        stockIn.setStockId(stock.getId());
        stockIn.setMaterialId(stock.getMaterialId());
        stockIn.setBatchNo(stock.getBatchNo());
        stockIn.setInternalCode(stock.getInternalCode());
        stockIn.setPurityConcentration(stock.getPurityConcentration());
        stockIn.setExpiryDate(stock.getExpiryDate());
        stockIn.setQuantity(BigDecimal.ONE);
        stockIn.setLocationId(stock.getLocationId());
        stockIn.setReason("RETURN");
        stockIn.setOperatorId(operatorId);
        stockInMapper.insert(stockIn);
    }
}
