package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInService {

    private final StockInMapper stockInMapper;
    private final StockMapper stockMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final UserMapper userMapper;
    private final SupplierMapper supplierMapper;

    public PageResult<StockIn> list(Integer current, Integer size, String keyword, String reason,
                                      String startDate, String endDate, Long operatorId,
                                      String materialName, String batchNo) {
        Page<StockIn> page = new Page<>(current, size);

        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(reason), StockIn::getReason, reason)
               .ge(StringUtils.hasText(startDate), StockIn::getCreateTime, startDate + " 00:00:00")
               .le(StringUtils.hasText(endDate), StockIn::getCreateTime, endDate + " 23:59:59")
               .eq(operatorId != null, StockIn::getOperatorId, operatorId)
               .like(StringUtils.hasText(batchNo), StockIn::getBatchNo, batchNo)
               .orderByDesc(StockIn::getCreateTime);

        // 按标准物质名称查询需要关联查询
        if (StringUtils.hasText(materialName)) {
            wrapper.inSql(StockIn::getMaterialId,
                "SELECT id FROM reference_material WHERE name LIKE '%" + materialName + "%'");
        }

        Page<StockIn> result = stockInMapper.selectPage(page, wrapper);

        result.getRecords().forEach(this::fillRelations);

        PageResult<StockIn> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    /**
     * 获取所有入库记录（用于导出）
     */
    public List<StockIn> listAll(String keyword, String reason, String startDate, String endDate,
                                  Long operatorId, String materialName, String batchNo) {
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(reason), StockIn::getReason, reason)
               .ge(StringUtils.hasText(startDate), StockIn::getCreateTime, startDate + " 00:00:00")
               .le(StringUtils.hasText(endDate), StockIn::getCreateTime, endDate + " 23:59:59")
               .eq(operatorId != null, StockIn::getOperatorId, operatorId)
               .like(StringUtils.hasText(batchNo), StockIn::getBatchNo, batchNo)
               .orderByDesc(StockIn::getCreateTime);

        if (StringUtils.hasText(materialName)) {
            wrapper.inSql(StockIn::getMaterialId,
                "SELECT id FROM reference_material WHERE name LIKE '%" + materialName + "%'");
        }

        List<StockIn> list = stockInMapper.selectList(wrapper);
        list.forEach(this::fillRelations);
        return list;
    }

    /**
     * 获取指定批号的最大序列号
     */
    private int getMaxSequence(String batchNo) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
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

    @Transactional
    public void create(StockIn stockIn, Long operatorId) {
        ReferenceMaterial material = materialMapper.selectById(stockIn.getMaterialId());
        if (material == null) {
            throw new BusinessException("标准物质不存在");
        }

        // 验证批号
        if (!StringUtils.hasText(stockIn.getBatchNo())) {
            throw new BusinessException("批号不能为空");
        }

        // 获取数量
        int quantity = stockIn.getQuantity() != null ? stockIn.getQuantity().intValue() : 1;
        if (quantity <= 0) {
            throw new BusinessException("入库数量必须大于0");
        }

        // 获取该批号的最大序列号
        int maxSequence = getMaxSequence(stockIn.getBatchNo());
        log.info("Batch {} current max sequence: {}", stockIn.getBatchNo(), maxSequence);

        // 生成内部编号范围
        String firstCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + 1);
        String lastCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + quantity);
        String internalCodeRange = quantity == 1 ? firstCode : firstCode + " ~ " + lastCode;

        // 设置入库记录信息
        stockIn.setOperatorId(operatorId);
        stockIn.setInternalCode(internalCodeRange);

        // 为每个物品创建独立的库存记录
        for (int i = 1; i <= quantity; i++) {
            int sequence = maxSequence + i;
            String internalCode = generateInternalCode(stockIn.getBatchNo(), sequence);

            Stock stock = new Stock();
            stock.setMaterialId(stockIn.getMaterialId());
            stock.setBatchNo(stockIn.getBatchNo());
            stock.setInternalCode(internalCode);
            stock.setExpiryDate(stockIn.getExpiryDate());
            stock.setQuantity(BigDecimal.ONE);  // 每条记录数量为1
            stock.setLocationId(stockIn.getLocationId());
            stock.setStatus(1);
            stockMapper.insert(stock);

            log.info("Created stock record with internal code: {}", internalCode);
        }

        // 插入入库记录
        stockInMapper.insert(stockIn);
    }

    private void fillRelations(StockIn stockIn) {
        if (stockIn.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(stockIn.getMaterialId());
            if (material != null) {
                stockIn.setMaterialName(material.getName());
            }
        }
        if (stockIn.getLocationId() != null) {
            Location location = locationMapper.selectById(stockIn.getLocationId());
            if (location != null) {
                stockIn.setLocationName(location.getName());
            }
        }
        if (stockIn.getOperatorId() != null) {
            User user = userMapper.selectById(stockIn.getOperatorId());
            if (user != null) {
                stockIn.setOperatorName(user.getRealName());
            }
        }
        if (stockIn.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(stockIn.getSupplierId());
            if (supplier != null) {
                stockIn.setSupplierName(supplier.getName());
            }
        }
    }
}
