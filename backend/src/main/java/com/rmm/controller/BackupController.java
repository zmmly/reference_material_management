package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.BackupRecord;
import com.rmm.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统备份", description = "数据库备份管理")
@RestController
@RequestMapping("/api/system/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "获取备份列表", description = "分页获取备份记录列表")
    @GetMapping
    public Result<PageResult<BackupRecord>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(backupService.list(current, size));
    }

    @Operation(summary = "创建备份", description = "手动创建数据库备份")
    @PostMapping
    public Result<BackupRecord> createBackup() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BackupRecord record = backupService.createBackup(userId, "管理员");
        return Result.success(record);
    }

    @Operation(summary = "下载备份", description = "下载指定的备份文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long id) {
        java.nio.file.Path filePath = backupService.getBackupFilePath(id);
        BackupRecord record = backupService.getById(id);

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + record.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "删除备份", description = "删除指定的备份记录和文件")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBackup(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return Result.success();
    }
}
