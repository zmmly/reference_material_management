package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.BackupRecord;
import com.rmm.mapper.BackupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupMapper backupMapper;

    @Value("${backup.directory:backups}")
    private String backupDirectory;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    public PageResult<BackupRecord> list(Integer current, Integer size) {
        Page<BackupRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BackupRecord::getBackupTime);
        Page<BackupRecord> result = backupMapper.selectPage(page, wrapper);

        PageResult<BackupRecord> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public BackupRecord createBackup(Long operatorId, String operatorName) {
        Path backupDir = Paths.get(backupDirectory);
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new BusinessException("创建备份目录失败: " + e.getMessage());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "backup_" + timestamp + ".sql";
        Path backupFile = backupDir.resolve(filename);

        String database = extractDatabaseName(datasourceUrl);
        String mysqldumpPath = "C:\\\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                mysqldumpPath,
                "-h", "localhost",
                "-P", "3306",
                "-u", datasourceUsername,
                "-p" + datasourcePassword,
                database,
                "--single-transaction",
                "--routines",
                "--triggers"
            );
            pb.redirectOutput(backupFile.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = readStream(process.getErrorStream());
                throw new BusinessException("备份失败: " + error);
            }

            if (!Files.exists(backupFile)) {
                throw new BusinessException("备份文件创建失败");
            }

            long fileSize = Files.size(backupFile);

            BackupRecord record = new BackupRecord();
            record.setFilename(filename);
            record.setFileSize(fileSize);
            record.setBackupTime(LocalDateTime.now());
            record.setOperatorId(operatorId);
            record.setOperatorName(operatorName);
            backupMapper.insert(record);

            log.info("Database backup created: {}, size: {} bytes", filename, fileSize);
            return record;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("备份被中断");
        } catch (IOException e) {
            throw new BusinessException("备份失败: " + e.getMessage());
        }
    }

    public Path getBackupFilePath(Long id) {
        BackupRecord record = backupMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("备份记录不存在");
        }
        Path filePath = Paths.get(backupDirectory, record.getFilename());
        if (!Files.exists(filePath)) {
            throw new BusinessException("备份文件不存在");
        }
        return filePath;
    }

    public BackupRecord getById(Long id) {
        return backupMapper.selectById(id);
    }

    public void deleteBackup(Long id) {
        BackupRecord record = backupMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("备份记录不存在");
        }

        Path filePath = Paths.get(backupDirectory, record.getFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete backup file: {}", filePath, e);
        }

        backupMapper.deleteById(id);
        log.info("Backup deleted: {}", record.getFilename());
    }

    private String extractDatabaseName(String url) {
        // JDBC URL format: jdbc:mysql://host:port/database?params
        // Find the third '/' (after jdbc:mysql://)
        int firstSlash = url.indexOf('/');
        int secondSlash = url.indexOf('/', firstSlash + 1);
        int thirdSlash = url.indexOf('/', secondSlash + 1);

        int start = thirdSlash + 1;
        int end = url.indexOf('?', start);
        if (end == -1) {
            end = url.length();
        }
        return url.substring(start, end);
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
