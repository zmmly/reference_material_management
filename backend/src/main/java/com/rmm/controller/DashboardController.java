package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(dashboardService.getStats());
    }

    @GetMapping("/todo-items")
    public Result<Map<String, Object>> getTodoItems() {
        return Result.success(dashboardService.getTodoItems());
    }

    @GetMapping("/expiry-stats")
    public Result<Map<String, Object>> getExpiryStats() {
        return Result.success(dashboardService.getExpiryStats());
    }
}
