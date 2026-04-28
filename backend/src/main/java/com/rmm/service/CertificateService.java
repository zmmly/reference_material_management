package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.Certificate;
import com.rmm.entity.ReferenceMaterial;
import com.rmm.entity.User;
import com.rmm.mapper.CertificateMapper;
import com.rmm.mapper.ReferenceMaterialMapper;
import com.rmm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateMapper certificateMapper;
    private final ReferenceMaterialMapper materialMapper;
    private final UserMapper userMapper;

    public PageResult<Certificate> list(Integer current, Integer size, String keyword,
                                         Long materialId, String batchNo) {
        Page<Certificate> page = new Page<>(current, size);

        LambdaQueryWrapper<Certificate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, Certificate::getMaterialId, materialId)
               .like(StringUtils.hasText(batchNo), Certificate::getBatchNo, batchNo)
               .orderByDesc(Certificate::getCreateTime);

        if (StringUtils.hasText(keyword)) {
            wrapper.inSql(Certificate::getMaterialId,
                "SELECT id FROM reference_material WHERE name LIKE '%" + keyword + "%'");
        }

        Page<Certificate> result = certificateMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillRelations);

        PageResult<Certificate> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public Certificate getById(Long id) {
        Certificate cert = certificateMapper.selectById(id);
        if (cert != null) {
            fillRelations(cert);
        }
        return cert;
    }

    public Certificate query(Long materialId, String batchNo) {
        LambdaQueryWrapper<Certificate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Certificate::getMaterialId, materialId)
               .eq(Certificate::getBatchNo, batchNo);
        return certificateMapper.selectOne(wrapper);
    }

    public void create(Certificate certificate) {
        if (certificate.getMaterialId() == null) {
            throw new BusinessException("标准物质不能为空");
        }
        if (!StringUtils.hasText(certificate.getBatchNo())) {
            throw new BusinessException("批号不能为空");
        }
        if (!StringUtils.hasText(certificate.getFilePath())) {
            throw new BusinessException("请上传证书文件");
        }

        // 校验唯一性
        Certificate existing = query(certificate.getMaterialId(), certificate.getBatchNo());
        if (existing != null) {
            throw new BusinessException("该标准物质和批号已有证书记录");
        }

        certificateMapper.insert(certificate);
    }

    public void update(Long id, Certificate certificate) {
        Certificate existing = certificateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("证书记录不存在");
        }

        // 如果修改了 materialId 或 batchNo，需要校验唯一性
        Long newMaterialId = certificate.getMaterialId() != null ? certificate.getMaterialId() : existing.getMaterialId();
        String newBatchNo = StringUtils.hasText(certificate.getBatchNo()) ? certificate.getBatchNo() : existing.getBatchNo();

        if (!newMaterialId.equals(existing.getMaterialId()) || !newBatchNo.equals(existing.getBatchNo())) {
            Certificate conflict = query(newMaterialId, newBatchNo);
            if (conflict != null && !conflict.getId().equals(id)) {
                throw new BusinessException("该标准物质和批号已有证书记录");
            }
        }

        certificate.setId(id);
        certificateMapper.updateById(certificate);
    }

    public void delete(Long id) {
        certificateMapper.deleteById(id);
    }

    private void fillRelations(Certificate cert) {
        if (cert.getMaterialId() != null) {
            ReferenceMaterial material = materialMapper.selectById(cert.getMaterialId());
            if (material != null) {
                cert.setMaterialName(material.getName());
            }
        }
        if (cert.getUploaderId() != null) {
            User user = userMapper.selectById(cert.getUploaderId());
            if (user != null) {
                cert.setUploaderName(user.getRealName());
            }
        }
    }
}
