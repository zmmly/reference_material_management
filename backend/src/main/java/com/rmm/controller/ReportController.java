package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard/summary")
    public Result<Map<String, Object>> getDashboardSummary() {
        return Result.success(reportService.getDashboardSummary());
    }

    @GetMapping("/dashboard/category-stats")
    public Result<List<Map<String, Object>>> getCategoryStats() {
        return Result.success(reportService.getCategoryStats());
    }

    @GetMapping("/dashboard/location-stats")
    public Result<List<Map<String, Object>>> getLocationStats() {
        return Result.success(reportService.getLocationStats());
    }

    @GetMapping("/dashboard/expiry-stats")
    public Result<List<Map<String, Object>>> getExpiryStats() {
        return Result.success(reportService.getExpiryStats());
    }

    @GetMapping("/in-out-trend")
    public Result<Map<String, Object>> getInOutTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportService.getInOutTrend(startDate, endDate));
    }
}
