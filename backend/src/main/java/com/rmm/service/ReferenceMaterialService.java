package com.rmm.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.dto.MaterialImportConfirmDTO;
import com.rmm.dto.MaterialImportDTO;
import com.rmm.dto.MaterialImportPreviewVO;
import com.rmm.entity.Category;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.entity.Supplier;
import com.rmm.mapper.CategoryMapper;
import com.rmm.mapper.ReferenceMaterialMapper;
import com.rmm.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceMaterialService {

    private final ReferenceMaterialMapper materialMapper;
    private final CategoryMapper categoryMapper;
    private final SupplierMapper supplierMapper;

    public PageResult<ReferenceMaterial> list(Integer current, Integer size, String name, Long categoryId, Integer status) {
        Page<ReferenceMaterial> page = new Page<>(current, size);
        LambdaQueryWrapper<ReferenceMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), ReferenceMaterial::getName, name)
               .eq(categoryId != null, ReferenceMaterial::getCategoryId, categoryId)
               .eq(status != null, ReferenceMaterial::getStatus, status)
               .orderByDesc(ReferenceMaterial::getCreateTime);

        Page<ReferenceMaterial> result = materialMapper.selectPage(page, wrapper);

        // 填充分类名称和供应商名称
        result.getRecords().forEach(material -> {
            fillCategoryName(material);
            fillSupplierName(material);
        });

        PageResult<ReferenceMaterial> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public List<ReferenceMaterial> listAll() {
        List<ReferenceMaterial> list = materialMapper.selectList(
            new LambdaQueryWrapper<ReferenceMaterial>()
                .eq(ReferenceMaterial::getStatus, 1)
                .orderByAsc(ReferenceMaterial::getName)
        );
        list.forEach(material -> {
            fillCategoryName(material);
            fillSupplierName(material);
        });
        return list;
    }

    public ReferenceMaterial getById(Long id) {
        ReferenceMaterial material = materialMapper.selectById(id);
        if (material != null) {
            fillCategoryName(material);
            fillSupplierName(material);
        }
        return material;
    }

    public void create(ReferenceMaterial material) {
        if (materialMapper.selectCount(new LambdaQueryWrapper<ReferenceMaterial>()
                .eq(ReferenceMaterial::getCode, material.getCode())) > 0) {
            throw new BusinessException("编号已存在");
        }
        material.setStatus(1);
        materialMapper.insert(material);
    }

    public void update(ReferenceMaterial material) {
        ReferenceMaterial existing = materialMapper.selectById(material.getId());
        if (existing == null) {
            throw new BusinessException("标准物质不存在");
        }
        if (!existing.getCode().equals(material.getCode())) {
            if (materialMapper.selectCount(new LambdaQueryWrapper<ReferenceMaterial>()
                    .eq(ReferenceMaterial::getCode, material.getCode())) > 0) {
                throw new BusinessException("编号已存在");
            }
        }
        materialMapper.updateById(material);
    }

    public void delete(Long id) {
        materialMapper.deleteById(id);
    }

    private void fillCategoryName(ReferenceMaterial material) {
        if (material.getCategoryId() != null) {
            Category category = categoryMapper.selectById(material.getCategoryId());
            if (category != null) {
                material.setCategoryName(category.getName());
            }
        }
    }

    private void fillSupplierName(ReferenceMaterial material) {
        if (material.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(material.getSupplierId());
            if (supplier != null) {
                material.setSupplierName(supplier.getName());
            }
        }
    }

    /**
     * 预览导入数据
     */
    public MaterialImportPreviewVO previewImport(MultipartFile file) throws IOException {
        List<MaterialImportDTO> rows = EasyExcel.read(file.getInputStream())
                .head(MaterialImportDTO.class)
                .sheet()
                .doReadSync();

        // 预加载分类名称映射
        Map<String, Category> categoryNameMap = loadCategoryNameMap();
        // 预加载供应商名称映射
        Map<String, Supplier> supplierNameMap = loadSupplierNameMap();
        // 预加载已存在的编号
        Map<String, Boolean> existingCodes = loadExistingCodes();

        List<MaterialImportPreviewVO.PreviewItem> items = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            MaterialImportDTO row = rows.get(i);
            // 跳过空行
            if (isEmptyRow(row)) {
                continue;
            }

            MaterialImportPreviewVO.PreviewItem item = validateRow(row, i + 2, categoryNameMap, supplierNameMap, existingCodes);
            items.add(item);

            if (item.getValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        MaterialImportPreviewVO result = new MaterialImportPreviewVO();
        result.setItems(items);
        result.setTotalCount(items.size());
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        return result;
    }

    /**
     * 判断是否为空行
     */
    private boolean isEmptyRow(MaterialImportDTO row) {
        return (row.getCode() == null || row.getCode().isBlank())
            && (row.getName() == null || row.getName().isBlank());
    }

    /**
     * 加载分类名称映射
     */
    private Map<String, Category> loadCategoryNameMap() {
        Map<String, Category> map = new HashMap<>();
        List<Category> categories = categoryMapper.selectList(null);
        for (Category c : categories) {
            if (c.getName() != null) {
                map.put(c.getName(), c);
            }
        }
        return map;
    }

    /**
     * 加载供应商名称映射
     */
    private Map<String, Supplier> loadSupplierNameMap() {
        Map<String, Supplier> map = new HashMap<>();
        List<Supplier> suppliers = supplierMapper.selectList(null);
        for (Supplier s : suppliers) {
            if (s.getName() != null) {
                map.put(s.getName(), s);
            }
        }
        return map;
    }

    /**
     * 加载已存在的编号
     */
    private Map<String, Boolean> loadExistingCodes() {
        Map<String, Boolean> map = new HashMap<>();
        List<ReferenceMaterial> materials = materialMapper.selectList(null);
        for (ReferenceMaterial m : materials) {
            if (m.getCode() != null) {
                map.put(m.getCode(), true);
            }
        }
        return map;
    }

    /**
     * 校验单行数据
     */
    private MaterialImportPreviewVO.PreviewItem validateRow(
            MaterialImportDTO row, int rowNum,
            Map<String, Category> categoryNameMap,
            Map<String, Supplier> supplierNameMap,
            Map<String, Boolean> existingCodes) {

        MaterialImportPreviewVO.PreviewItem item = new MaterialImportPreviewVO.PreviewItem();
        item.setRowNum(rowNum);
        item.setCode(row.getCode());
        item.setName(row.getName());
        item.setCasNumber(row.getCasNumber());
        item.setCategoryName(row.getCategoryName());
        item.setSpecification(row.getSpecification());
        item.setMatrix(row.getMatrix());
        item.setPackageForm(row.getPackageForm());
        item.setSupplierName(row.getSupplierName());

        List<String> errors = new ArrayList<>();

        // 校验编号
        if (row.getCode() == null || row.getCode().isBlank()) {
            errors.add("编号不能为空");
        } else if (existingCodes.containsKey(row.getCode())) {
            errors.add("编号已存在");
        }

        // 校验名称
        if (row.getName() == null || row.getName().isBlank()) {
            errors.add("名称不能为空");
        }

        // 校验分类
        if (row.getCategoryName() == null || row.getCategoryName().isBlank()) {
            errors.add("分类不能为空");
        } else {
            Category category = categoryNameMap.get(row.getCategoryName());
            if (category == null) {
                errors.add("分类不存在");
            } else {
                item.setCategoryId(category.getId());
            }
        }

        // 校验规格
        if (row.getSpecification() == null || row.getSpecification().isBlank()) {
            errors.add("规格不能为空");
        }

        // 校验供应商（可选）
        if (row.getSupplierName() != null && !row.getSupplierName().isBlank()) {
            Supplier supplier = supplierNameMap.get(row.getSupplierName());
            if (supplier == null) {
                errors.add("供应商不存在");
            } else {
                item.setSupplierId(supplier.getId());
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
    public int confirmImport(MaterialImportConfirmDTO dto) {
        int successCount = 0;
        for (MaterialImportConfirmDTO.ImportItem item : dto.getItems()) {
            ReferenceMaterial material = new ReferenceMaterial();
            material.setCode(item.getCode());
            material.setName(item.getName());
            material.setCasNumber(item.getCasNumber());
            material.setCategoryId(item.getCategoryId());
            material.setSpecification(item.getSpecification());
            material.setMatrix(item.getMatrix());
            material.setPackageForm(item.getPackageForm());
            material.setSupplierId(item.getSupplierId());
            material.setStatus(1);
            materialMapper.insert(material);
            successCount++;
        }
        return successCount;
    }
}
