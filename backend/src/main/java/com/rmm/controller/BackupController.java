package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.BackupRecord;
import com.rmm.service.BackupService;
import com.rmm.util.JwtUtil;
import com.rmm.util.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;
    private final OperationLogUtil operationLogUtil;

    @Operation(summary = "获取备份列表", description = "分页获取备份记录列表")
    @GetMapping
    public Result<PageResult<BackupRecord>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(backupService.list(current, size));
    }

    @Operation(summary = "创建备份", description = "手动创建数据库备份")
    @PostMapping
    public Result<BackupRecord> createBackup(HttpServletRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 获取用户名用于日志记录
        String username = "管理员";
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                username = jwtUtil.getUsername(token.substring(7));
            } catch (Exception e) {
                // 忽略异常
            }
        }

        BackupRecord record = backupService.createBackup(userId, username);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "backup", "备份",
            "系统备份", "创建系统备份: " + record.getFilename());

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
    public Result<Void> deleteBackup(@PathVariable Long id, HttpServletRequest request) {
        BackupRecord record = backupService.getById(id);
        backupService.deleteBackup(id);

        // 获取用户信息用于日志记录
        String username = "管理员";
        Long userId = null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String tokenValue = token.substring(7);
                userId = jwtUtil.getUserId(tokenValue);
                username = jwtUtil.getUsername(tokenValue);
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "backup", "删除",
            "系统备份", "删除系统备份: " + (record != null ? record.getFilename() : id));

        return Result.success();
    }
}
