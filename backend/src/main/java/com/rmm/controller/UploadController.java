package com.rmm.controller;

import com.rmm.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Tag(name = "文件上传", description = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    // 新增：允许的文件扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS =
        Set.of(".jpg", ".jpeg", ".png", ".pdf", ".doc", ".docx", ".xls", ".xlsx");

    @Value("${upload.path:uploads}")
    private String configuredUploadPath;

    private String uploadPath;

    @PostConstruct
    public void init() {
        // 使用配置的上传路径
        uploadPath = configuredUploadPath;

        // 如果是相对路径，转换为绝对路径（相对于项目根目录）
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            // 使用系统临时目录或用户主目录作为基准
            String basePath = System.getProperty("user.home");
            uploadPath = basePath + File.separator + uploadPath;
            dir = new File(uploadPath);
        }

        // 创建目录
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Created upload directory: {}", uploadPath);
            } else {
                log.warn("Failed to create upload directory: {}", uploadPath);
            }
        }

        log.info("Upload path configured: {}", uploadPath);
    }

    @Operation(summary = "上传文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "type", defaultValue = "certificate") String type) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 新增：验证文件类型
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                return Result.error("不支持的文件类型，仅支持 JPG、PNG、PDF、DOC、XLS 等格式");
            }

            // 生成新文件名：日期/UUID.扩展名
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 创建目录
            File dir = new File(uploadPath + "/" + type + "/" + datePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            File destFile = new File(dir, newFilename);
            file.transferTo(destFile.getAbsoluteFile());

            // 返回相对路径
            String relativePath = "/" + type + "/" + datePath + "/" + newFilename;
            log.info("File uploaded: {} -> {}", originalFilename, destFile.getAbsolutePath());

            return Result.success(relativePath);
        } catch (IOException e) {
            log.error("File upload failed", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @Operation(summary = "预览文件")
    @GetMapping("/preview")
    public void preview(@RequestParam String path, HttpServletResponse response) throws IOException {
        if (path == null || path.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "文件路径不能为空");
            return;
        }

        // 新增：路径安全验证，防止路径遍历攻击
        // 去掉开头的 "/"，确保是相对路径
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        Path uploadPathObj = Paths.get(uploadPath).normalize();
        Path resolvedPath = uploadPathObj.resolve(relativePath).normalize();

        if (!resolvedPath.startsWith(uploadPathObj)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法路径");
            return;
        }

        File file = resolvedPath.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" +
                URLEncoder.encode(file.getName(), StandardCharsets.UTF_8));
        response.setContentLengthLong(file.length());

        try (OutputStream os = response.getOutputStream()) {
            Files.copy(file.toPath(), os);
            os.flush();
        }
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    public Result<Void> delete(@RequestParam String path) {
        if (path == null || path.isEmpty()) {
            return Result.error("文件路径不能为空");
        }

        try {
            // 新增：路径安全验证，防止路径遍历攻击
            // 去掉开头的 "/"，确保是相对路径
            String relativePath = path.startsWith("/") ? path.substring(1) : path;
            Path uploadPathObj = Paths.get(uploadPath).normalize();
            Path resolvedPath = uploadPathObj.resolve(relativePath).normalize();

            if (!resolvedPath.startsWith(uploadPathObj)) {
                return Result.error("非法路径");
            }

            File file = resolvedPath.toFile();
            if (file.exists() && file.isFile()) {
                file.delete();
                log.info("File deleted: {}", path);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("File delete failed", e);
            return Result.error("文件删除失败");
        }
    }
}
