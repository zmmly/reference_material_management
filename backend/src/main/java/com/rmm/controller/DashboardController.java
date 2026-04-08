package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.service.DashboardService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(dashboardService.getStats());
    }

    @GetMapping("/todo-items")
    public Result<Map<String, Object>> getTodoItems(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(dashboardService.getTodoItems(userId));
    }

    @GetMapping("/expiry-stats")
    public Result<Map<String, Object>> getExpiryStats() {
        return Result.success(dashboardService.getExpiryStats());
    }
}
