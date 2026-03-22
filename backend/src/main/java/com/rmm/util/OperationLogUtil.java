package com.rmm.util;

import com.rmm.entity.OperationLog;
import com.rmm.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 操作日志工具类
 * 用于手动记录操作日志
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogUtil {

    private final OperationLogService operationLogService;

    /**
     * 记录操作日志
     */
    public void log(HttpServletRequest request, Long userId, String username,
                  String module, String action, String target, String detail) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setModule(module);
            log.setAction(action);
            log.setTarget(target);
            log.setDetail(detail);
            log.setIp(getClientIp(request));

            operationLogService.create(log);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
