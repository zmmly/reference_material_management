package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;

    public PageResult<Purchase> list(Integer current, Integer size, Integer status,
                                      String purchaseNo, String materialName, Long applicantId) {
        Page<Purchase> page = new Page<>(current, size);

        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Purchase::getStatus, status)
               .eq(applicantId != null, Purchase::getApplicantId, applicantId)
               .like(StringUtils.hasText(purchaseNo), Purchase::getPurchaseNo, purchaseNo)
               .orderByDesc(Purchase::getApplyTime);

        // materialName 关联查询：先找匹配的标准物质ID
        if (StringUtils.hasText(materialName)) {
            List<ReferenceMaterial> materials = materialMapper.selectList(
                    new LambdaQueryWrapper<ReferenceMaterial>().like(ReferenceMaterial::getName, materialName));
            if (materials.isEmpty()) {
                PageResult<Purchase> pageResult = new PageResult<>();
                pageResult.setRecords(new ArrayList<>());
                pageResult.setTotal(0L);
                pageResult.setSize((long) size);
                pageResult.setCurrent((long) current);
                pageResult.setPages(0L);
                return pageResult;
            }
            List<Long> materialIds = materials.stream().map(ReferenceMaterial::getId).toList();
            wrapper.in(Purchase::getMaterialId, materialIds);
        }

        Page<Purchase> result = purchaseMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillRelations);

        PageResult<Purchase> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public Purchase getById(Long id) {
        Purchase purchase = purchaseMapper.selectById(id);
        if (purchase != null) {
            fillRelations(purchase);
        }
        return purchase;
    }

    @Transactional
    public void apply(Purchase purchase, Long applicantId) {
        ReferenceMaterial material = materialMapper.selectById(purchase.getMaterialId());
        if (material == null) {
            throw new BusinessException("标准物质不存在");
        }

        // 自动计算金额（采购数量 * 预估单价）
        if (purchase.getQuantity() != null && purchase.getEstimatedPrice() != null) {
            purchase.setTotalAmount(purchase.getQuantity().multiply(purchase.getEstimatedPrice()));
        }

        // 设置默认单位
        if (!org.springframework.util.StringUtils.hasText(purchase.getUnit())) {
            purchase.setUnit("支");
        }

        // 生成采购申请单号
        if (purchase.getPurchaseNo() == null || purchase.getPurchaseNo().isEmpty()) {
            purchase.setPurchaseNo(generatePurchaseNo());
        }

        purchase.setApplicantId(applicantId);
        purchase.setApplyTime(LocalDateTime.now());
        purchase.setStatus(0);
        purchaseMapper.insert(purchase);
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

    @Transactional
    public void approve(Long id, Long approverId, boolean approved, String rejectReason) {
        Purchase purchase = purchaseMapper.selectById(id);
        if (purchase == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (purchase.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        if (approved) {
            purchase.setStatus(1);
        } else {
            if (!StringUtils.hasText(rejectReason)) {
                throw new BusinessException("请填写拒绝原因");
            }
            purchase.setStatus(2);
            purchase.setRejectReason(rejectReason);
        }

        purchase.setApproverId(approverId);
        purchase.setApproveTime(LocalDateTime.now());
        purchaseMapper.updateById(purchase);
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        Purchase purchase = purchaseMapper.selectById(id);
        if (purchase == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (purchase.getStatus() != 0) {
            throw new BusinessException("只能撤回待审批的申请");
        }
        if (!purchase.getApplicantId().equals(userId)) {
            throw new BusinessException("只能撤回自己的申请");
        }

        purchase.setStatus(3);
        purchaseMapper.updateById(purchase);
    }

    private void fillRelations(Purchase purchase) {
        if (purchase.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(purchase.getMaterialId());
            if (material != null) {
                purchase.setMaterialName(material.getName());
                purchase.setMaterialCode(material.getCode());
            }
        }
        if (purchase.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(purchase.getSupplierId());
            if (supplier != null) {
                purchase.setSupplierName(supplier.getName());
            }
        }
        if (purchase.getApplicantId() != null) {
            User user = userMapper.selectById(purchase.getApplicantId());
            if (user != null) {
                purchase.setApplicantName(user.getRealName());
            }
        }
        if (purchase.getApproverId() != null) {
            User user = userMapper.selectById(purchase.getApproverId());
            if (user != null) {
                purchase.setApproverName(user.getRealName());
            }
        }
    }

    /**
     * 查询导出数据（不分页）
     */
    public List<Purchase> listForExport(String purchaseNo, String materialName, Integer status) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();

        if (purchaseNo != null && !purchaseNo.isBlank()) {
            wrapper.like(Purchase::getPurchaseNo, purchaseNo);
        }

        if (status != null) {
            wrapper.eq(Purchase::getStatus, status);
        }

        // materialName 关联查询需要先找匹配的标准物质ID
        if (materialName != null && !materialName.isBlank()) {
            List<ReferenceMaterial> materials = materialMapper.selectList(
                    new LambdaQueryWrapper<ReferenceMaterial>().like(ReferenceMaterial::getName, materialName));
            if (materials.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> materialIds = materials.stream().map(ReferenceMaterial::getId).toList();
            wrapper.in(Purchase::getMaterialId, materialIds);
        }

        wrapper.orderByDesc(Purchase::getApplyTime);
        List<Purchase> list = purchaseMapper.selectList(wrapper);
        list.forEach(this::fillRelations);
        return list;
    }
}
