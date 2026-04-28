package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Certificate;
import com.rmm.service.CertificateService;
import com.rmm.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "证书管理", description = "标准物质证书的增删改查")
@RestController
@RequestMapping("/api/basic/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "分页查询证书")
    @GetMapping
    public Result<PageResult<Certificate>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String batchNo) {
        return Result.success(certificateService.list(current, size, keyword, materialId, batchNo));
    }

    @Operation(summary = "根据ID查询证书")
    @GetMapping("/{id}")
    public Result<Certificate> getById(@PathVariable Long id) {
        return Result.success(certificateService.getById(id));
    }

    @Operation(summary = "根据物质和批号查询证书")
    @GetMapping("/query")
    public Result<Certificate> query(@RequestParam Long materialId, @RequestParam String batchNo) {
        return Result.success(certificateService.query(materialId, batchNo));
    }

    @Operation(summary = "新增证书")
    @PostMapping
    public Result<Void> create(@RequestBody Certificate certificate,
                                @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        certificate.setUploaderId(userId);
        certificateService.create(certificate);
        return Result.success();
    }

    @Operation(summary = "更新证书")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Certificate certificate) {
        certificateService.update(id, certificate);
        return Result.success();
    }

    @Operation(summary = "删除证书")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        certificateService.delete(id);
        return Result.success();
    }
}
