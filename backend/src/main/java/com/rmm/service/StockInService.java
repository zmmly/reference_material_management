package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.*;
import com.rmm.mapper.*;
import com.rmm.dto.StockInDeleteCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.excel.EasyExcel;
import com.rmm.dto.StockInImportConfirmDTO;
import com.rmm.dto.StockInImportDTO;
import com.rmm.dto.StockInImportPreviewVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInService {

    private final StockInMapper stockInMapper;
    private final StockMapper stockMapper;
    private final StockOutMapper stockOutMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final LocationMapper locationMapper;
    private final UserMapper userMapper;
    private final SupplierMapper supplierMapper;

    /** 入库原因文字到编码的映射 */
    private static final Map<String, String> REASON_TEXT_TO_CODE = Map.of(
        "新购入", "PURCHASE",
        "盘盈", "SURPLUS",
        "归还", "RETURN",
        "调拨入", "TRANSFER_IN",
        "其他", "OTHER"
    );

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
            stock.setPurityConcentration(stockIn.getPurityConcentration());
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

    /**
     * 更新入库记录（仅更新有效期、存放位置、证书等可编辑字段）
     */
    @Transactional
    public void update(Long id, StockIn stockIn) {
        StockIn existing = stockInMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("入库记录不存在");
        }

        // 只更新允许编辑的字段
        existing.setExpiryDate(stockIn.getExpiryDate());
        existing.setLocationId(stockIn.getLocationId());
        existing.setProductCertificate(stockIn.getProductCertificate());

        stockInMapper.updateById(existing);

        // 同步更新对应的库存记录
        if (existing.getStockId() != null) {
            Stock stock = stockMapper.selectById(existing.getStockId());
            if (stock != null) {
                stock.setExpiryDate(stockIn.getExpiryDate());
                stock.setLocationId(stockIn.getLocationId());
                stockMapper.updateById(stock);
            }
        }
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

    /**
     * 预览导入数据
     */
    public StockInImportPreviewVO previewImport(MultipartFile file) throws IOException {
        // 解析 Excel
        List<StockInImportDTO> rows = EasyExcel.read(file.getInputStream())
                .head(StockInImportDTO.class)
                .sheet()
                .doReadSync();

        // 预加载标准物质编码映射
        Map<String, ReferenceMaterial> materialCodeMap = loadMaterialCodeMap();
        // 预加载位置名称映射
        Map<String, Location> locationNameMap = loadLocationNameMap();

        List<StockInImportPreviewVO.PreviewItem> items = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            StockInImportDTO row = rows.get(i);
            // 跳过示例数据行或空行
            if (i == 0 && isSampleDataRow(row)) {
                continue;
            }
            // 跳过完全空的行
            if (isEmptyRow(row)) {
                continue;
            }

            StockInImportPreviewVO.PreviewItem item = validateRow(row, i + 2, materialCodeMap, locationNameMap);
            items.add(item);

            if (item.getValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        StockInImportPreviewVO result = new StockInImportPreviewVO();
        result.setItems(items);
        result.setTotalCount(items.size());
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        return result;
    }

    /**
     * 判断是否为示例数据行
     */
    private boolean isSampleDataRow(StockInImportDTO row) {
        return row.getMaterialCode() == null && row.getBatchNo() == null;
    }

    /**
     * 判断是否为空行
     */
    private boolean isEmptyRow(StockInImportDTO row) {
        return (row.getMaterialCode() == null || row.getMaterialCode().isBlank())
            && (row.getBatchNo() == null || row.getBatchNo().isBlank())
            && row.getQuantity() == null
            && (row.getLocationName() == null || row.getLocationName().isBlank());
    }

    /**
     * 加载标准物质编码映射
     */
    private Map<String, ReferenceMaterial> loadMaterialCodeMap() {
        Map<String, ReferenceMaterial> map = new HashMap<>();
        List<ReferenceMaterial> materials = materialMapper.selectList(null);
        for (ReferenceMaterial m : materials) {
            if (m.getCode() != null) {
                map.put(m.getCode(), m);
            }
        }
        return map;
    }

    /**
     * 加载位置名称映射
     */
    private Map<String, Location> loadLocationNameMap() {
        Map<String, Location> map = new HashMap<>();
        List<Location> locations = locationMapper.selectList(null);
        for (Location l : locations) {
            if (l.getName() != null) {
                map.put(l.getName(), l);
            }
        }
        return map;
    }

    /**
     * 校验单行数据
     */
    private StockInImportPreviewVO.PreviewItem validateRow(
            StockInImportDTO row, int rowNum,
            Map<String, ReferenceMaterial> materialCodeMap,
            Map<String, Location> locationNameMap) {

        StockInImportPreviewVO.PreviewItem item = new StockInImportPreviewVO.PreviewItem();
        item.setRowNum(rowNum);
        item.setMaterialCode(row.getMaterialCode());
        item.setMaterialName(row.getMaterialName());
        item.setBatchNo(row.getBatchNo());
        item.setPurityConcentration(row.getPurityConcentration());
        item.setQuantity(row.getQuantity());
        item.setLocationName(row.getLocationName());
        item.setReasonText(row.getReason());
        item.setRemarks(row.getRemarks());

        List<String> errors = new ArrayList<>();

        // 校验标准物质编码
        if (row.getMaterialCode() == null || row.getMaterialCode().isBlank()) {
            errors.add("标准物质编码不能为空");
        } else {
            ReferenceMaterial material = materialCodeMap.get(row.getMaterialCode());
            if (material == null) {
                errors.add("标准物质编码不存在");
            } else {
                item.setMaterialId(material.getId());
            }
        }

        // 校验标准物质名称
        if (row.getMaterialName() == null || row.getMaterialName().isBlank()) {
            errors.add("标准物质名称不能为空");
        }

        // 校验批号
        if (row.getBatchNo() == null || row.getBatchNo().isBlank()) {
            errors.add("批号不能为空");
        }

        // 校验入库数量
        if (row.getQuantity() == null) {
            errors.add("入库数量不能为空");
        } else if (row.getQuantity() < 1) {
            errors.add("入库数量必须大于0");
        }

        // 校验纯度/浓度
        if (row.getPurityConcentration() == null || row.getPurityConcentration().isBlank()) {
            errors.add("纯度/浓度不能为空");
        }

        // 校验有效期格式（支持 YYYY-MM-DD 和 YYYY/MM/DD）
        if (row.getExpiryDate() != null && !row.getExpiryDate().isBlank()) {
            try {
                String dateStr = row.getExpiryDate().replace("/", "-");
                item.setExpiryDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                errors.add("有效期格式错误，应为 YYYY-MM-DD 或 YYYY/MM/DD");
            }
        }

        // 校验存放位置
        if (row.getLocationName() == null || row.getLocationName().isBlank()) {
            errors.add("存放位置不能为空");
        } else {
            Location location = locationNameMap.get(row.getLocationName());
            if (location == null) {
                errors.add("存放位置不存在");
            } else {
                item.setLocationId(location.getId());
            }
        }

        // 校验入库原因
        if (row.getReason() == null || row.getReason().isBlank()) {
            errors.add("入库原因不能为空");
        } else {
            String code = REASON_TEXT_TO_CODE.get(row.getReason());
            if (code == null) {
                errors.add("入库原因必须是：新购入/盘盈/归还/调拨入/其他");
            } else {
                item.setReasonCode(code);
            }
        }

        item.setValid(errors.isEmpty());
        item.setErrors(errors);
        return item;
    }

    /**
     * 确认批量导入
     */
    @Transactional
    public int confirmImport(StockInImportConfirmDTO dto, Long operatorId) {
        int successCount = 0;
        for (StockInImportConfirmDTO.ImportItem item : dto.getItems()) {
            // 构建 StockIn 对象
            StockIn stockIn = new StockIn();
            stockIn.setMaterialId(item.getMaterialId());
            stockIn.setBatchNo(item.getBatchNo());
            stockIn.setPurityConcentration(item.getPurityConcentration());
            stockIn.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            stockIn.setExpiryDate(item.getExpiryDate());
            stockIn.setLocationId(item.getLocationId());
            stockIn.setReason(item.getReason());
            stockIn.setRemarks(item.getRemarks());

            // 调用内部方法创建入库记录
            createStockInRecord(stockIn, operatorId);
            successCount++;
        }
        return successCount;
    }

    /**
     * 创建入库记录（复用 create 方法核心逻辑）
     */
    private void createStockInRecord(StockIn stockIn, Long operatorId) {
        int quantity = stockIn.getQuantity() != null ? stockIn.getQuantity().intValue() : 1;

        // 获取该批号的最大序列号
        int maxSequence = getMaxSequence(stockIn.getBatchNo());

        // 生成内部编号范围
        String firstCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + 1);
        String lastCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + quantity);
        String internalCodeRange = quantity == 1 ? firstCode : firstCode + " ~ " + lastCode;

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
            stock.setQuantity(BigDecimal.ONE);
            stock.setLocationId(stockIn.getLocationId());
            stock.setStatus(1);
            stockMapper.insert(stock);
        }

        stockInMapper.insert(stockIn);
    }

    /**
     * 检查入库记录是否可以删除
     * 返回检查结果，包含是否可删除、不可删除原因、相关出库记录信息
     */
    public StockInDeleteCheckVO checkCanDelete(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw new BusinessException("入库记录不存在");
        }

        StockInDeleteCheckVO result = new StockInDeleteCheckVO();
        result.setStockInId(id);
        result.setCanDelete(true);

        // 解析内部编号范围
        List<String> internalCodes = parseInternalCodeRange(stockIn.getInternalCode());
        if (internalCodes.isEmpty()) {
            result.setCanDelete(false);
            result.setReason("无法解析内部编号");
            return result;
        }

        // 查找所有关联的库存记录
        LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.in(Stock::getInternalCode, internalCodes);
        List<Stock> stocks = stockMapper.selectList(stockWrapper);

        List<StockInDeleteCheckVO.StockOutInfo> blockedItems = new ArrayList<>();

        for (Stock stock : stocks) {
            // 检查是否有待审批或已批准的出库申请
            LambdaQueryWrapper<StockOut> outWrapper = new LambdaQueryWrapper<>();
            outWrapper.eq(StockOut::getStockId, stock.getId())
                     .in(StockOut::getStatus, 0, 1); // 0=待审批, 1=已批准
            List<StockOut> stockOuts = stockOutMapper.selectList(outWrapper);

            for (StockOut out : stockOuts) {
                StockInDeleteCheckVO.StockOutInfo info = new StockInDeleteCheckVO.StockOutInfo();
                info.setInternalCode(stock.getInternalCode());
                info.setStatus(out.getStatus() == 0 ? "待审批" : "已批准");
                info.setReason(out.getReason());
                info.setApplyTime(out.getApplyTime());
                blockedItems.add(info);
            }

            // 检查库存状态是否已出库
            if (stock.getStatus() != null && stock.getStatus() == 0) {
                StockInDeleteCheckVO.StockOutInfo info = new StockInDeleteCheckVO.StockOutInfo();
                info.setInternalCode(stock.getInternalCode());
                info.setStatus("已出库");
                info.setReason("该物质已完成出库");
                blockedItems.add(info);
            }
        }

        if (!blockedItems.isEmpty()) {
            result.setCanDelete(false);
            result.setReason("存在关联的出库记录，无法删除");
            result.setBlockedItems(blockedItems);
        }

        return result;
    }

    /**
     * 删除入库记录及关联的库存记录
     */
    @Transactional
    public void delete(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw new BusinessException("入库记录不存在");
        }

        // 先检查是否可以删除
        StockInDeleteCheckVO check = checkCanDelete(id);
        if (!check.getCanDelete()) {
            throw new BusinessException(check.getReason());
        }

        // 解析内部编号范围
        List<String> internalCodes = parseInternalCodeRange(stockIn.getInternalCode());

        // 删除关联的库存记录
        if (!internalCodes.isEmpty()) {
            LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.in(Stock::getInternalCode, internalCodes);
            stockMapper.delete(stockWrapper);
        }

        // 删除入库记录
        stockInMapper.deleteById(id);
    }

    /**
     * 解析内部编号范围
     * 支持格式: "BATCH001-001" 或 "BATCH001-001 ~ BATCH001-003"
     */
    private List<String> parseInternalCodeRange(String internalCode) {
        List<String> codes = new ArrayList<>();
        if (internalCode == null || internalCode.isBlank()) {
            return codes;
        }

        if (internalCode.contains("~")) {
            // 范围格式: BATCH001-001 ~ BATCH001-003
            String[] parts = internalCode.split("~");
            if (parts.length != 2) {
                return codes;
            }

            String startCode = parts[0].trim();
            String endCode = parts[1].trim();

            // 提取序列号
            int lastDashStart = startCode.lastIndexOf("-");
            int lastDashEnd = endCode.lastIndexOf("-");

            if (lastDashStart == -1 || lastDashEnd == -1) {
                codes.add(startCode);
                return codes;
            }

            String prefix = startCode.substring(0, lastDashStart + 1);
            try {
                int startSeq = Integer.parseInt(startCode.substring(lastDashStart + 1));
                int endSeq = Integer.parseInt(endCode.substring(lastDashEnd + 1));

                for (int i = startSeq; i <= endSeq; i++) {
                    codes.add(prefix + String.format("%03d", i));
                }
            } catch (NumberFormatException e) {
                log.error("无法解析内部编号序列号: {}", internalCode);
                codes.add(startCode);
            }
        } else {
            // 单个编号
            codes.add(internalCode.trim());
        }

        return codes;
    }
}
